package iit.pkd.researchrequirements.service

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.auxmodel.*
import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.*
import iit.pkd.researchrequirements.model.requirement.*
import iit.pkd.researchrequirements.model.session.SessionStatus
import iit.pkd.researchrequirements.model.user.*
import iit.pkd.researchrequirements.repo.*
import org.springframework.stereotype.Service

@Service
class ResearchRequirementService(
    private val rrRepo: ResearchRequirementRepository,
    private val sessionRepo: ResearchRecruitmentSessionRepository,
    private val userRepo: ERPUserViewRepository,
    private val deptRepo: DepartmentRepository
) {

    /** Fetch current (OPEN latest) session research requirement for a dept*/
    fun fetchCurrentResearchRequirements(deptShortCode: String): OpResponse<ResearchRequirement>
    {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return OpResponse.failure("Invalid department short code: $deptShortCode")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failure("No active research recruitment session (OPEN).")

        // Use data-layer query to fetch active (not archived) requirements for the session+dept
        val rrList = rrRepo.findAllBySessionIDAndDeptShortCodeAndIsArchivedFalse(session.id, dept.deptShortCode)

        if (rrList.isEmpty()) {
            return OpResponse.failure("No research requirement found for department ${dept.deptShortCode}.")
        }

        // pick latest version by version number (UInt)
        val latestRequirement = rrList.maxByOrNull { it.version } ?: rrList.first()
        return OpResponse.success(data = latestRequirement)
    }





    /** Fetch historical (CLOSED sessions) research requirements for a dept */
    fun fetchHistoricalResearchRequirements(deptShortCode: String): OpResponse<List<ResearchRequirement>>
    {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return OpResponse.failure("Invalid department short code: $deptShortCode")

        val closedSessions = sessionRepo.findAllByStatus(SessionStatus.CLOSED)
        if (closedSessions.isEmpty()) {
            return OpResponse.failure("No closed research recruitment sessions found.")
        }

        val sessionIds = closedSessions.map { it.id }
        val closedRequirements = rrRepo.findAllByDeptShortCodeAndSessionIDIn(dept.deptShortCode, sessionIds)

        if (closedRequirements.isEmpty()) {
            return OpResponse.failure("No historical research requirements found for department ${dept.deptShortCode}.")
        }
        return OpResponse.success(data = closedRequirements)
    }



    /** Fetch faculty members for a department — querying at data-layer */
    fun fetchFaculty(deptShortCode: String): OpResponse<List<ERPMinView>>
    {
        if (deptShortCode != "*") {
            deptRepo.findByDeptShortCode(deptShortCode)
                ?: return OpResponse.failure("Invalid department short code: $deptShortCode")
        }

        // Data-layer filtering: find faculty who have deptShortCode in their deptShortCodes set
        val faculty = if (deptShortCode == "*") {
            userRepo.findByUserType(UserType.FACULTY)
        } else {
            userRepo.findByUserTypeAndDeptShortCodesContaining(UserType.FACULTY, deptShortCode)
        }

        val minViews = faculty.map { it.toMinView() }
        return OpResponse.success(data = minViews)
    }




    /** Save as draft
     *  Enforce exactly one remark per update (if provided). If remarks empty allowed for save,
     *  but we will ensure only 0 or 1 remark is present (0 allowed for save if you prefer).
     */
    fun saveResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement>
    {
        // allow saving even without remarks, but if remarks provided enforce size == 1
        if (body.remarks.isNotEmpty() && body.remarks.size != 1)
        {
            return OpResponse.failure("Exactly one remark must be provided per update")
        }
        return upsertSaveOnly(body)
    }




    /** Submit (requires exactly one remark) */
    fun submitResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement>
    {
        if (body.remarks.isEmpty() || body.remarks.size != 1)
        {
            return OpResponse.failure("Exactly one remark must be provided when submitting requirement")
        }
        return upsert(body, VacancyStatus.SUBMITTED, RequirementStatus.SUBMITTED)
    }

    // helper: apply current date to the single remark (if exists)
    private fun List<Remark>.withCurrentDateSingle(): MutableList<Remark> =
        if (this.isEmpty()) mutableListOf()
        else mutableListOf(this.first().copy(date = UIDate.getCurrentDate()))




    /**
     * Upsert logic for SAVED status only.
     * Uses body.requirementId to determine update; if provided and exists, create a new version (archive old).
     * If not found, create new requirement.
     */
    private fun upsertSaveOnly(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return OpResponse.failure("Invalid department short code: ${body.deptShortCode}")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failure("No active session. Modification allowed only in OPEN session.")

        // If requirementId is provided -> update that specific document
        val existingOpt = if (body.requirementId != ResearchRequirementID.empty()) {
            rrRepo.findById(body.requirementId).orElse(null)
        } else null

        if (existingOpt != null) {
            // Verify ownership
            if (existingOpt.deptShortCode != dept.deptShortCode || existingOpt.sessionID != session.id) {
                return OpResponse.failure("Requirement ID does not belong to department ${dept.deptShortCode} in current OPEN session")
            }

            // Archive old version
            val old = existingOpt.copy(isArchived = true)
            rrRepo.save(old)

            // Create new version
            val newReq = existingOpt.copy(
                id = ResearchRequirementID.create(),
                researchVacancy = body.researchVacancy.toMutableList(),
                approvedVacancy = old.approvedVacancy.toMutableList(),
                vacancyStatus = forwardOnlyStatusUpdate(old.vacancyStatus, VacancyStatus.SAVED),
                requirementStatus = RequirementStatus.SAVED,
                remarks = body.remarks.withCurrentDateSingle(),
                decisions = old.decisions.toMutableList(),
                version = old.version + 1u,
                isArchived = false
            )
            val saved = rrRepo.save(newReq)
            return OpResponse.success("Research requirement updated and saved successfully", saved)
        }

        // If requirementId is empty -> create fresh requirement
        // Ensure only one active per dept/session
        val activeList = rrRepo.findAllBySessionIDAndDeptShortCodeAndIsArchivedFalse(session.id, dept.deptShortCode)
        activeList.forEach { rrRepo.save(it.copy(isArchived = true)) }

        val toSave = ResearchRequirement(
            id = ResearchRequirementID.create(),
            sessionID = session.id,
            deptShortCode = dept.deptShortCode,
            researchVacancy = body.researchVacancy.toMutableList(),
            approvedVacancy = mutableListOf(),
            vacancyStatus = VacancyStatus.SAVED,
            requirementStatus = RequirementStatus.SAVED,
            remarks = body.remarks.withCurrentDateSingle(),
            decisions = mutableListOf(),
            version = 1u,
            isArchived = false
        )
        val saved = rrRepo.save(toSave)
        return OpResponse.success("Research requirement created and saved successfully", saved)
    }






    /**
     * Upsert logic for SUBMITTED status. Similar versioning/archiving logic as save.
     * Validates possible guides belong to department. Keeps approvedVacancy immutable here.
     */
    private fun upsert(
        body: ResearchRequirementREq,
        vacStatus: VacancyStatus,
        reqStatus: RequirementStatus
    ): OpResponse<ResearchRequirement> {
        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return OpResponse.failure("Invalid department short code: ${body.deptShortCode}")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return OpResponse.failure("No active session. Modification allowed only in OPEN session.")

        // Validate possible guides
        val facultyOfDept = userRepo.findByUserTypeAndDeptShortCodesContaining(UserType.FACULTY, body.deptShortCode)
            .map { it.id }

        val validatedVacancy = body.researchVacancy.map { subArea ->
            val updatedVacancies = subArea.researchFields.map { field ->
                val validGuides = field.possibleGuide.filter { it in facultyOfDept }
                if (validGuides.size != field.possibleGuide.size) {
                    return OpResponse.failure("One or more possible guides are invalid for ${field.researchField}")
                }
                field.copy(possibleGuide = validGuides.toMutableList())
            }.toMutableList()
            subArea.copy(researchFields = updatedVacancies)
        }.toMutableList()

        // If requirementId is provided -> update specific document
        val existingOpt = if (body.requirementId != ResearchRequirementID.empty()) {
            rrRepo.findById(body.requirementId).orElse(null)
        } else null

        if (existingOpt != null) {
            if (existingOpt.deptShortCode != dept.deptShortCode || existingOpt.sessionID != session.id) {
                return OpResponse.failure("Requirement ID does not belong to department ${dept.deptShortCode} in current OPEN session")
            }

            // Check approvedVacancy limit if submitting
            if (reqStatus == RequirementStatus.SUBMITTED &&
                existingOpt.vacancyStatus == VacancyStatus.APPROVED &&
                existingOpt.approvedVacancy.isNotEmpty()
            ) {
                val totalRequested = validatedVacancy.sumOf { sub -> sub.researchFields.sumOf { it.vacancy.toInt() } }
                val totalApproved = existingOpt.approvedVacancy.sumOf { it.vacancy.toInt() }
                if (totalRequested > totalApproved) {
                    return OpResponse.failure("Total requested vacancies ($totalRequested) exceed approved total ($totalApproved)")
                }
            }

            // Archive old version
            val old = existingOpt.copy(isArchived = true)
            rrRepo.save(old)

            // Save new version
            val newReq = existingOpt.copy(
                id = ResearchRequirementID.create(),
                researchVacancy = validatedVacancy,
                vacancyStatus = forwardOnlyStatusUpdate(existingOpt.vacancyStatus, vacStatus),
                requirementStatus = reqStatus,
                remarks = body.remarks.withCurrentDateSingle(),
                version = old.version + 1u,
                isArchived = false
            )
            val saved = rrRepo.save(newReq)
            return OpResponse.success("Research requirement updated successfully", saved)
        }

        // If requirementId is empty -> create new requirement
        val activeList = rrRepo.findAllBySessionIDAndDeptShortCodeAndIsArchivedFalse(session.id, dept.deptShortCode)
        activeList.forEach { rrRepo.save(it.copy(isArchived = true)) }

        val toSave = ResearchRequirement(
            id = ResearchRequirementID.create(),
            sessionID = session.id,
            deptShortCode = dept.deptShortCode,
            researchVacancy = validatedVacancy,
            approvedVacancy = mutableListOf(),
            vacancyStatus = vacStatus,
            requirementStatus = reqStatus,
            remarks = body.remarks.withCurrentDateSingle(),
            decisions = mutableListOf(),
            version = 1u,
            isArchived = false
        )
        val saved = rrRepo.save(toSave)
        return OpResponse.success("Research requirement created successfully", saved)
    }
    /** Ensure vacancyStatus only moves forward */
    private fun forwardOnlyStatusUpdate(current: VacancyStatus, requested: VacancyStatus): VacancyStatus {
        return when (current) {
            VacancyStatus.SAVED -> requested
            VacancyStatus.SUBMITTED -> if (requested == VacancyStatus.APPROVED) VacancyStatus.APPROVED else VacancyStatus.SUBMITTED
            VacancyStatus.APPROVED -> VacancyStatus.APPROVED
        }
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
