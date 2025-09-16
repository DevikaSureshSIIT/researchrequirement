package iit.pkd.researchrequirements.service

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.api.RestResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.DeptRequest
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.auxmodel.*
import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.ResearchRequirementID
import iit.pkd.researchrequirements.model.requirement.*
import iit.pkd.researchrequirements.model.session.*
import iit.pkd.researchrequirements.model.user.*
import iit.pkd.researchrequirements.repo.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ResearchRequirementService(
    private val rrRepo: ResearchRequirementRepository,
    private val sessionRepo: ResearchRecruitmentSessionRepository,
    private val userRepo: ERPUserViewRepository
) {

    /** Fetch OPEN session research requirements */
    fun fetchCurrentResearchRequirements(req: DeptRequest): RestResponseEntity<ResearchRequirement> {
        val session = sessionRepo.findByStatus(SessionStatus.OPEN)
            ?: return RestResponse.error("No active research recruitment session.", HttpStatus.FORBIDDEN)

        val rr = rrRepo.findBySessionIDAndDeptShortCode(session.id, req.deptShortCode)
            ?: return RestResponse.error("No research requirements found for department ${req.deptShortCode}.")

        return RestResponse.withData(rr)
    }

    /** Fetch CLOSED session research requirement history */
    fun fetchHistoricalResearchRequirements(req: DeptRequest): RestResponseEntity<List<ResearchRequirement>> {
        val closedRequirements = rrRepo.findAllByDeptShortCodeAndIsArchivedTrue(req.deptShortCode)
        if (closedRequirements.isEmpty()) {
            return RestResponse.error("No historical research requirements found for department ${req.deptShortCode}.", HttpStatus.NOT_FOUND)
        }
        return RestResponse.withData(closedRequirements)
    }

    /** Fetch faculty members for a department */
    fun fetchFaculty(req: DeptRequest): RestResponseEntity<List<ERPMinView>> {
        val faculty = userRepo.findByUserType(UserType.FACULTY)
        val filtered = if (req.deptShortCode == "*") {
            faculty
        } else {
            faculty.filter { it.deptShortCodes.contains(req.deptShortCode) }
        }
        val minViews = filtered.map { it.toMinView() }
        return RestResponse.withData(minViews)
    }

    /** Save draft research requirement (no validation) */
    fun saveResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        return upsert(body, VacancyStatus.SAVED, RequirementStatus.SAVED)
    }

    /** Submit research requirement (validation against approved vacancy) */
    fun submitResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        return upsert(body, VacancyStatus.SUBMITTED, RequirementStatus.SUBMITTED)
    }

    /** Internal upsert helper for save/submit */
    private fun upsert(
        body: ResearchRequirementREq,
        vacStatus: VacancyStatus,
        reqStatus: RequirementStatus
    ): OpResponse<ResearchRequirement> {

        // Must be OPEN session
        val session = sessionRepo.findByStatus(SessionStatus.OPEN)
            ?: return OpResponse.failure("No active session. Modification allowed only in OPEN session.")

        // Validate possible guides exist
        val facultyIds = userRepo.findByUserType(UserType.FACULTY).map { it.id }
        val validatedVacancy = body.researchVacancy.map { subArea ->
            val updatedVacancies = subArea.vacancies.map { field ->
                val validGuides = field.possibleGuide.filter { it in facultyIds }
                if (validGuides.size != field.possibleGuide.size)
                    return OpResponse.failure("One or more possible guides are invalid for ${field.researchField}")
                field.copy(possibleGuide = validGuides.toMutableList())
            }.toMutableList()
            subArea.copy(vacancies = updatedVacancies)
        }.toMutableList()

        // Fetch existing record
        val existing = rrRepo.findBySessionIDAndDeptShortCode(session.id, body.deptShortCode)

        // Preserve approvedVacancy
        val now = UIDate.getCurrentDate()
        val toSave = if (existing == null) {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = body.deptShortCode,
                researchVacancy = validatedVacancy,
                approvedVacancy = mutableListOf(),
                vacancyStatus = vacStatus,
                requirementStatus = reqStatus,
                remarks = body.remarks.toMutableList(),
                decisions = mutableListOf(),
                version = body.version ?: "v1",
                isArchived = false
            )
        } else {
            // If submitting, validate total vacancy <= approved
            if (reqStatus == RequirementStatus.SUBMITTED && existing.approvedVacancy.isNotEmpty()) {
                val totalRequested = validatedVacancy.sumOf { sub ->
                    sub.vacancies.sumOf { it.vacancy.toInt() }
                }
                val totalApproved = existing.approvedVacancy.sumOf { it.vacancy.toInt() }
                if (totalRequested > totalApproved) {
                    return OpResponse.failure("Total requested vacancies ($totalRequested) exceed approved total ($totalApproved)")
                }
            }
            existing.copy(
                researchVacancy = validatedVacancy,
                vacancyStatus = vacStatus,
                requirementStatus = reqStatus,
                remarks = body.remarks.toMutableList(),
            )
        }

        val saved = rrRepo.save(toSave)
        val msg = if (existing == null) "Research vacancies saved/submitted successfully"
        else "Research vacancies updated successfully"
        return OpResponse.success(msg, saved)
    }

    /** Mapper ERPUserView → ERPMinView */
    private fun ERPUserView.toMinView(): ERPMinView =
        ERPMinView(
            id = this.id,
            name = "${this.firstname} ${this.lastname}",
            email = this.email,
            deptShortCodes = this.deptShortCodes.toList(),
            erpID = this.erpID
        )
}
