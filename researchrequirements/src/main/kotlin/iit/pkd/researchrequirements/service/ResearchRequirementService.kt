package iit.pkd.researchrequirements.service

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.api.RestResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.auxmodel.*
import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.*
import iit.pkd.researchrequirements.model.requirement.*
import iit.pkd.researchrequirements.model.session.SessionStatus
import iit.pkd.researchrequirements.model.user.*
import iit.pkd.researchrequirements.repo.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ResearchRequirementService(
    private val rrRepo: ResearchRequirementRepository,
    private val sessionRepo: ResearchRecruitmentSessionRepository,
    private val userRepo: ERPUserViewRepository,
    private val deptRepo: DepartmentRepository
) {

    /** Fetch current (OPEN latest) session research requirements for a dept */
    fun fetchCurrentResearchRequirements(deptShortCode: String): RestResponseEntity<List<ResearchRequirement>> {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return RestResponse.error("Invalid department short code: $deptShortCode", HttpStatus.BAD_REQUEST)

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return RestResponse.error("No active research recruitment session.", HttpStatus.FORBIDDEN)

        val rrList = rrRepo.findAllBySessionIDAndDeptShortCode(session.id, dept.deptShortCode)
        if (rrList.isEmpty())
            return RestResponse.error("No research requirements found for department ${dept.deptShortCode}.")
        return RestResponse.withData(rrList)

    }

    /** Fetch historical (archived/closed) research requirements for a dept */
    fun fetchHistoricalResearchRequirements(deptShortCode: String): RestResponseEntity<List<ResearchRequirement>> {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return RestResponse.error("Invalid department short code: $deptShortCode", HttpStatus.BAD_REQUEST)

        val closedRequirements = rrRepo.findAllByDeptShortCodeAndIsArchivedTrue(dept.deptShortCode)
        if (closedRequirements.isEmpty()) {
            return RestResponse.error("No historical research requirements found for department ${dept.deptShortCode}.", HttpStatus.NOT_FOUND)
        }
        return RestResponse.withData(closedRequirements)
    }

    /** Fetch faculty members for a department */
    fun fetchFaculty(deptShortCode: String): RestResponseEntity<List<ERPMinView>> {
        if (deptShortCode != "*") {
            deptRepo.findByDeptShortCode(deptShortCode)
                ?: return RestResponse.error("Invalid department short code: $deptShortCode", HttpStatus.BAD_REQUEST)
        }

        val faculty = userRepo.findByUserType(UserType.FACULTY)
        val filtered = if (deptShortCode == "*") faculty else faculty.filter { it.deptShortCodes.contains(deptShortCode) }
        val minViews = filtered.map { it.toMinView() }
        return RestResponse.withData(minViews)
    }

    /** Save as draft (requires requirementId) - no validations */
    fun saveResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        return upsertSaveOnly(body)
    }

    /** Submit (requires requirementId) */
    fun submitResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        return upsert(body, VacancyStatus.SUBMITTED, RequirementStatus.SUBMITTED)
    }

    /**
     * Upsert logic for SAVED status only, skips all validations
     */
    private fun upsertSaveOnly(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return OpResponse.failure("Invalid department short code: ${body.deptShortCode}")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failure("No active session. Modification allowed only in OPEN session.")

        val existing = rrRepo.findById(body.requirementId).orElse(null)

        val toSave = if (existing == null) {
            ResearchRequirement(
                id = body.requirementId,
                sessionID = session.id,
                deptShortCode = body.deptShortCode,
                researchVacancy = body.researchVacancy.toMutableList(),
                approvedVacancy = mutableListOf(),
                vacancyStatus = VacancyStatus.SAVED,
                requirementStatus = RequirementStatus.SAVED,
                remarks = body.remarks.toMutableList(),
                decisions = mutableListOf(),
                version = body.version ?: "v1",
                isArchived = false
            )
        } else {
            existing.copy(
                researchVacancy = body.researchVacancy.toMutableList(),
                vacancyStatus = VacancyStatus.SAVED,
                requirementStatus = RequirementStatus.SAVED,
                remarks = body.remarks.toMutableList()
                // approvedVacancy remains unchanged
            )
        }

        val saved = rrRepo.save(toSave)
        val msg = if (existing == null) "Research requirement created and saved successfully"
        else "Research requirement updated and saved successfully"
        return OpResponse.success(msg, saved)
    }


    /**
     * Upsert logic based only on requirementId.
     */
    private fun upsert(
        body: ResearchRequirementREq,
        vacStatus: VacancyStatus,
        reqStatus: RequirementStatus
    ): OpResponse<ResearchRequirement> {
        // validate dept
        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return OpResponse.failure("Invalid department short code: ${body.deptShortCode}")

        // choose latest OPEN session
        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failure("No active session. Modification allowed only in OPEN session.")

        // Validate possible guides
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

        // Fetch by requirementId
        val existing = rrRepo.findById(body.requirementId).orElse(null)

        // Validation for submit
        if (reqStatus == RequirementStatus.SUBMITTED && existing != null && existing.approvedVacancy.isNotEmpty()) {
            val totalRequested = validatedVacancy.sumOf { sub -> sub.vacancies.sumOf { it.vacancy.toInt() } }
            val totalApproved = existing.approvedVacancy.sumOf { it.vacancy.toInt() }
            if (totalRequested > totalApproved) {
                return OpResponse.failure("Total requested vacancies ($totalRequested) exceed approved total ($totalApproved)")
            }
        }

        val toSave = if (existing == null) {
            ResearchRequirement(
                id = body.requirementId,
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
            existing.copy(
                researchVacancy = validatedVacancy,
                vacancyStatus = vacStatus,
                requirementStatus = reqStatus,
                remarks = body.remarks.toMutableList()
            )
        }

        val saved = rrRepo.save(toSave)
        val msg = if (existing == null) "Research requirement created successfully" else "Research requirement updated successfully"
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
