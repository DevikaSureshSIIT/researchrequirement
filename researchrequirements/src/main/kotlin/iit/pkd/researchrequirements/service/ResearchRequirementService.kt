package iit.pkd.researchrequirements.service

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.auxmodel.Remark
import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.ResearchRequirementID
import iit.pkd.researchrequirements.model.requirement.*
import iit.pkd.researchrequirements.model.session.SessionStatus
import iit.pkd.researchrequirements.model.user.UserType
import iit.pkd.researchrequirements.repo.DepartmentRepository
import iit.pkd.researchrequirements.repo.ERPUserViewRepository
import iit.pkd.researchrequirements.repo.ResearchRequirementRepository
import iit.pkd.researchrequirements.repo.ResearchRecruitmentSessionRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ResearchRequirementService(
    private val rrRepo: ResearchRequirementRepository,
    private val sessionRepo: ResearchRecruitmentSessionRepository,
    private val userRepo: ERPUserViewRepository,
    private val deptRepo: DepartmentRepository
) {

    /** Fetch current (OPEN latest) session research requirement for a dept */
    fun fetchCurrentResearchRequirements(deptShortCode: String): Mono<OpResponse<ResearchRequirement>> {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return OpResponse.failureAsMono("Invalid department short code: $deptShortCode")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failureAsMono("No active research recruitment session (OPEN).")

        val rrList = rrRepo.findAllBySessionIDAndDeptShortCodeAndIsArchivedFalse(session.id, dept.deptShortCode)
        if (rrList.isEmpty()) {
            return OpResponse.failureAsMono("No research requirement found for department ${dept.deptShortCode}.")
        }

        val latestRequirement = rrList.first()
        return Mono.just(OpResponse.success("Current research requirement fetched.", latestRequirement))
    }

    /** Fetch historical (CLOSED sessions) research requirements for a dept */
    fun fetchHistoricalResearchRequirements(deptShortCode: String): Mono<OpResponse<List<ResearchRequirement>>> {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return OpResponse.failureAsMono("Invalid department short code: $deptShortCode")

        val closedSessions = sessionRepo.findAllByStatus(SessionStatus.CLOSED)
        if (closedSessions.isEmpty()) return OpResponse.failureAsMono("No closed research recruitment sessions found.")

        val sessionIds = closedSessions.map { it.id }
        val closedRequirements = rrRepo.findAllByDeptShortCodeAndSessionIDIn(dept.deptShortCode, sessionIds)
        if (closedRequirements.isEmpty()) return OpResponse.failureAsMono("No historical research requirements found for department ${dept.deptShortCode}.")

        return Mono.just(OpResponse.success("Historical research requirements fetched.", closedRequirements))
    }

    /** Helper: apply current date to single remark */
    private fun List<Remark>.withCurrentDateSingle(): List<Remark> =
        if (this.isEmpty()) emptyList() else listOf(this.first().copy(date = UIDate.getCurrentDate()))

    fun saveResearchRequirement(body: ResearchRequirement): Mono<OpResponse<ResearchRequirementID>> {
        if (body.remarks.isNotEmpty() && body.remarks.size != 1)
            return OpResponse.failureAsMono("Exactly one remark must be provided per update")

        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return OpResponse.failureAsMono("Invalid department short code: ${body.deptShortCode}")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failureAsMono("No active session. Modification allowed only in OPEN session.")

        val existingOpt = if (body.id != ResearchRequirementID.empty())
            rrRepo.findById(body.id).orElse(null) else null

        if (existingOpt?.isArchived == true)
            return OpResponse.failureAsMono("Modification not allowed on archived requirement")

        val toSave = if (existingOpt != null) {
            if (existingOpt.deptShortCode != dept.deptShortCode || existingOpt.sessionID != session.id)
                return OpResponse.failureAsMono("Requirement ID does not belong to department ${dept.deptShortCode} in current OPEN session")

            existingOpt.copy(
                requestedVacancy = body.requestedVacancy, // no toMutableList()
                vacancyStatus = forwardOnlyStatusUpdate(existingOpt.vacancyStatus, VacancyStatus.SAVED),
                requirementStatus = RequirementStatus.SAVED,
                remarks = body.remarks.withCurrentDateSingle(),

                isArchived = false
            )
        } else {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = dept.deptShortCode,
                requestedVacancy = body.requestedVacancy, // no toMutableList()
                approvedVacancy = emptyList(),
                vacancyStatus = VacancyStatus.SAVED,
                requirementStatus = RequirementStatus.SAVED,
                remarks = body.remarks.withCurrentDateSingle(),
                decisions = emptyList(),

                isArchived = false
            )
        }

        return Mono.fromCallable { rrRepo.save(toSave) }
            .flatMap { saved -> OpResponse.successAsMono("Requirement saved successfully", saved.id) }
    }

    fun submitResearchRequirement(body: ResearchRequirement): Mono<OpResponse<Nothing>> {
        if (body.remarks.isEmpty() || body.remarks.size != 1)
            return OpResponse.failureAsMono("Exactly one remark must be provided when submitting requirement")

        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return OpResponse.failureAsMono("Invalid department short code: ${body.deptShortCode}")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failureAsMono("No active research recruitment session found.")

        val facultyOfDept = userRepo.findByUserTypeAndDeptShortCodesContaining(UserType.FACULTY, body.deptShortCode)
            .map { it.id }

        val validatedVacancy = body.requestedVacancy.map { subArea ->
            val updatedVacancies = subArea.researchFields.map { field ->
                val validGuides = field.possibleGuide.filter { it in facultyOfDept }
                if (validGuides.size != field.possibleGuide.size)
                    return OpResponse.failureAsMono<Nothing>(
                        "One or more possible guides are invalid for ${field.researchField}"
                    )
                field.copy(possibleGuide = validGuides) // list, no toMutableList()
            }
            subArea.copy(researchFields = updatedVacancies)
        }

        val existingOpt = if (body.id!= ResearchRequirementID.empty())
            rrRepo.findById(body.id).orElse(null) else null

        if (existingOpt?.isArchived == true)
            return OpResponse.failureAsMono("Modification not allowed on archived requirement")

        // ----- APPROVED VACANCY LIMIT CHECK -----
        if (existingOpt != null &&
            existingOpt.vacancyStatus == VacancyStatus.APPROVED &&
            existingOpt.approvedVacancy.isNotEmpty()
        ) {
            val totalRequested = validatedVacancy.sumOf { sub -> sub.researchFields.sumOf { it.vacancy.toInt() } }
            val totalApproved = existingOpt.approvedVacancy.sumOf { it.vacancy.toInt() }
            if (totalRequested > totalApproved) {
                return OpResponse.failureAsMono(
                    "Total requested vacancies ($totalRequested) exceed approved total ($totalApproved)"
                )
            }
        }
        // ----------------------------------------

        val toSave = if (existingOpt != null) {
            if (existingOpt.deptShortCode != dept.deptShortCode || existingOpt.sessionID != session.id)
                return OpResponse.failureAsMono("Requirement ID does not belong to department ${dept.deptShortCode} in current OPEN session")

            existingOpt.copy(
                requestedVacancy = validatedVacancy,
                vacancyStatus = forwardOnlyStatusUpdate(existingOpt.vacancyStatus, VacancyStatus.SUBMITTED),
                requirementStatus = RequirementStatus.SUBMITTED,
                remarks = body.remarks.withCurrentDateSingle(),
                isArchived = false
            )
        } else {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = dept.deptShortCode,
               requestedVacancy = validatedVacancy,
                approvedVacancy = emptyList(),
                vacancyStatus = VacancyStatus.SUBMITTED,
                requirementStatus = RequirementStatus.SUBMITTED,
                remarks = body.remarks.withCurrentDateSingle(),
                decisions = emptyList(),
                isArchived = false
            )
        }

        return Mono.fromCallable { rrRepo.save(toSave) }
            .flatMap { OpResponse.successAsMono("Research vacancies saved successfully.") } // data = null
    }


    /** Ensure vacancyStatus only moves forward */
    private fun forwardOnlyStatusUpdate(current: VacancyStatus, requested: VacancyStatus): VacancyStatus {
        return when (current) {
            VacancyStatus.SAVED -> requested
            VacancyStatus.SUBMITTED -> if (requested == VacancyStatus.APPROVED) VacancyStatus.APPROVED else VacancyStatus.SUBMITTED
            VacancyStatus.APPROVED -> VacancyStatus.APPROVED
        }
    }
}
