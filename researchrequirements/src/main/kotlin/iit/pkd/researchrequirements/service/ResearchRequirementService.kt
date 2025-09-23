package iit.pkd.researchrequirements.service

import iit.pkd.researchrequirements.api.RestResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.auxmodel.*
import iit.pkd.researchrequirements.model.id.*
import iit.pkd.researchrequirements.model.requirement.*
import iit.pkd.researchrequirements.model.session.SessionStatus
import iit.pkd.researchrequirements.model.user.*
import iit.pkd.researchrequirements.repo.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import iit.pkd.researchrequirements.model.common.UIDate

@Service
class ResearchRequirementService(
    private val rrRepo: ResearchRequirementRepository,
    private val sessionRepo: ResearchRecruitmentSessionRepository,
    private val userRepo: ERPUserViewRepository,
    private val deptRepo: DepartmentRepository
) {

    /** Fetch current (OPEN latest) session research requirement for a dept */
    fun fetchCurrentResearchRequirements(deptShortCode: String): RestResponseEntity<ResearchRequirement> {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return RestResponse.error("Invalid department short code: $deptShortCode", HttpStatus.BAD_REQUEST)

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return RestResponse.error("No active research recruitment session.", HttpStatus.FORBIDDEN)

        val rrList = rrRepo.findAllBySessionIDAndDeptShortCode(session.id, dept.deptShortCode)
        if (rrList.isEmpty()) {
            return RestResponse.error("No research requirement found for department ${dept.deptShortCode}.")
        }

        val latestRequirement = rrList.maxByOrNull { it.version.removePrefix("v").toIntOrNull() ?: 0 }
            ?: rrList.first()

        return RestResponse.withData(latestRequirement)
    }

    /** Fetch historical (archived/closed) research requirements for a dept */
    fun fetchHistoricalResearchRequirements(deptShortCode: String): RestResponseEntity<List<ResearchRequirement>> {
        val dept = deptRepo.findByDeptShortCode(deptShortCode)
            ?: return RestResponse.error("Invalid department short code: $deptShortCode", HttpStatus.BAD_REQUEST)

        val closedSessions = sessionRepo.findAllByStatus(SessionStatus.CLOSED)
        if (closedSessions.isEmpty()) {
            return RestResponse.error("No closed research recruitment sessions found.", HttpStatus.NOT_FOUND)
        }

        val closedRequirements = rrRepo.findAllByDeptShortCodeAndSessionIDIn(
            dept.deptShortCode,
            closedSessions.map { it.id }
        )

        if (closedRequirements.isEmpty()) {
            return RestResponse.error(
                "No historical research requirements found for department ${dept.deptShortCode}.",
                HttpStatus.NOT_FOUND
            )
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
        val filtered =
            if (deptShortCode == "*") faculty else faculty.filter { it.deptShortCodes.contains(deptShortCode) }
        val minViews = filtered.map { it.toMinView() }
        return RestResponse.withData(minViews)
    }

    /** Save as draft */
    fun saveResearchRequirement(body: ResearchRequirementREq): RestResponseEntity<ResearchRequirement> {
        return upsertSaveOnly(body)
    }

    /** Submit */
    fun submitResearchRequirement(body: ResearchRequirementREq): RestResponseEntity<ResearchRequirement> {
        if (body.remarks.isEmpty()) {
            return RestResponse.error("Remarks cannot be empty when submitting requirement")
        }
        return upsert(body, VacancyStatus.SUBMITTED, RequirementStatus.SUBMITTED)
    }

    /** Helper: set current date for all remarks */
    private fun List<Remark>.withCurrentDate(): MutableList<Remark> =
        this.map { it.copy(date = UIDate.getCurrentDate()) }.toMutableList()

    /** Upsert logic for SAVED status only */
    private fun upsertSaveOnly(body: ResearchRequirementREq): RestResponseEntity<ResearchRequirement> {
        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return RestResponse.error("Invalid department short code: ${body.deptShortCode}")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return RestResponse.error("No active session. Modification allowed only in OPEN session.")

        val existingList = rrRepo.findAllBySessionIDAndDeptShortCode(session.id, dept.deptShortCode)
        val existingForDept = existingList.maxByOrNull { it.version.removePrefix("v").toIntOrNull() ?: 0 }

        return if (existingForDept != null) {
            if (existingForDept.id != body.requirementId) {
                RestResponse.error("Requirement ID does not belong to department ${dept.deptShortCode}")
            } else {
                val updated = existingForDept.copy(
                    researchVacancy = body.researchVacancy.toMutableList(),
                    vacancyStatus = forwardOnlyStatusUpdate(existingForDept.vacancyStatus, VacancyStatus.SAVED),
                    requirementStatus = RequirementStatus.SAVED,
                    remarks = body.remarks.withCurrentDate()
                )
                val saved = rrRepo.save(updated)
                RestResponse.withMessageAndData("Research requirement updated and saved successfully", saved)
            }
        } else {
            val toSave = ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = body.deptShortCode,
                researchVacancy = body.researchVacancy.toMutableList(),
                approvedVacancy = mutableListOf(),
                vacancyStatus = VacancyStatus.SAVED,
                requirementStatus = RequirementStatus.SAVED,
                remarks = body.remarks.withCurrentDate(),
                decisions = mutableListOf(),
                version = "v1",
                isArchived = false
            )
            val saved = rrRepo.save(toSave)
            RestResponse.withMessageAndData("Research requirement created and saved successfully", saved)
        }
    }

    /** Upsert logic for SUBMITTED status */
    private fun upsert(
        body: ResearchRequirementREq,
        vacStatus: VacancyStatus,
        reqStatus: RequirementStatus
    ): RestResponseEntity<ResearchRequirement> {
        val dept = deptRepo.findByDeptShortCode(body.deptShortCode)
            ?: return RestResponse.error("Invalid department short code: ${body.deptShortCode}")

        val session = sessionRepo.findTopByStatusOrderByEndDateDesc(SessionStatus.OPEN)
            ?: return RestResponse.error("No active session. Modification allowed only in OPEN session.")

        val facultyOfDept = userRepo.findByUserType(UserType.FACULTY)
            .filter { it.deptShortCodes.contains(body.deptShortCode) }
            .map { it.id }

        val validatedVacancy = body.researchVacancy.map { subArea ->
            val updatedVacancies = subArea.researchFields.map { field ->
                val validGuides = field.possibleGuide.filter { it in facultyOfDept }
                if (validGuides.size != field.possibleGuide.size) {
                    return RestResponse.error("One or more possible guides are invalid for ${field.researchField}")
                }
                field.copy(possibleGuide = validGuides.toMutableList())
            }.toMutableList()
            subArea.copy(researchFields = updatedVacancies)
        }.toMutableList()

        val existingList = rrRepo.findAllBySessionIDAndDeptShortCode(session.id, dept.deptShortCode)
        val existingForDept = existingList.maxByOrNull { it.version.removePrefix("v").toIntOrNull() ?: 0 }

        val toSave = if (existingForDept == null) {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = body.deptShortCode,
                researchVacancy = validatedVacancy,
                approvedVacancy = mutableListOf(),
                vacancyStatus = vacStatus,
                requirementStatus = reqStatus,
                remarks = body.remarks.withCurrentDate(),
                decisions = mutableListOf(),
                version = "v1",
                isArchived = false
            )
        } else {
            if (existingForDept.id != body.requirementId) {
                return RestResponse.error("Requirement ID does not belong to department ${dept.deptShortCode}")
            }

            if (reqStatus == RequirementStatus.SUBMITTED &&
                existingForDept.vacancyStatus == VacancyStatus.APPROVED &&
                existingForDept.approvedVacancy.isNotEmpty()
            ) {
                val totalRequested = validatedVacancy.sumOf { sub -> sub.researchFields.sumOf { it.vacancy.toInt() } }
                val totalApproved = existingForDept.approvedVacancy.sumOf { it.vacancy.toInt() }
                if (totalRequested > totalApproved) {
                    return RestResponse.error(
                        "Total requested vacancies ($totalRequested) exceed approved total ($totalApproved)"
                    )
                }
            }

            existingForDept.copy(
                researchVacancy = validatedVacancy,
                vacancyStatus = forwardOnlyStatusUpdate(existingForDept.vacancyStatus, vacStatus),
                requirementStatus = reqStatus,
                remarks = body.remarks.withCurrentDate()
            )
        }

        val saved = rrRepo.save(toSave)
        val msg =
            if (existingForDept == null) "Research requirement created successfully" else "Research requirement updated successfully"
        return RestResponse.withMessageAndData(msg, saved)
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
