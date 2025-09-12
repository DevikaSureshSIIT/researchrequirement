package iit.pkd.researchrequirements.service


import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.api.RestResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.DeptRequest
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.ResearchRequirementID
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.session.ResearchRecruitmentSession
import iit.pkd.researchrequirements.model.session.SessionStatus
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.model.user.ERPUserView
import iit.pkd.researchrequirements.model.user.UserType
import iit.pkd.researchrequirements.repo.ERPUserViewRepository
import iit.pkd.researchrequirements.repo.ResearchRecruitmentSessionRepository
import iit.pkd.researchrequirements.repo.ResearchRequirementRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service


@Service
class ResearchRequirementService(
    private val rrRepo: ResearchRequirementRepository,
    private val sessionRepo: ResearchRecruitmentSessionRepository,
    private val userRepo: ERPUserViewRepository
) {
    // 1) Fetch research requirements for OPEN session
    fun fetchCurrentResearchRequirements(req: DeptRequest): RestResponseEntity<ResearchRequirement> {
        val session = sessionRepo.findByStatus(SessionStatus.OPEN)
            ?: return RestResponse.error("No active research recruitment session.", HttpStatus.FORBIDDEN)

        val rr = rrRepo.findBySessionIDAndDeptShortCode(session.id, req.deptShortCode)
            ?: return RestResponse.error("No research requirements found for department ${req.deptShortCode}.")

        return RestResponse.withData(rr)
    }

    // 2) Fetch research requirements for CLOSED sessions (history)
    fun fetchHistoricalResearchRequirements(req: DeptRequest): RestResponseEntity<List<ResearchRequirement>> {
        val closedRequirements = rrRepo.findAllByDeptShortCodeAndIsArchivedTrue(req.deptShortCode)
        if (closedRequirements.isEmpty()) {
            return RestResponse.error("No historical research requirements found for department ${req.deptShortCode}.", HttpStatus.NOT_FOUND)
        }
        return RestResponse.withData(closedRequirements)
    }


    // 3) Fetch faculty
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

    // 4) Upsert research requirement with new session rules
    fun upsertResearchRequirement(
        body: ResearchRequirementREq,
        currentUser: ERPUserView? = null
    ): OpResponse<ResearchRequirement> {



        // 1️⃣ Only OPEN session allowed
        val openSessions = sessionRepo.findAll()
            .filter { it.status == SessionStatus.OPEN }

        val session = openSessions.maxByOrNull { it.endDate }
            ?: return OpResponse.failure("No active session. Submission/update allowed only in OPEN session.")

        // 2️⃣ Validate possibleGuides: must exist as FACULTY in DB
        val facultyIds = userRepo.findByUserType(UserType.FACULTY).map { it.id }
        val updatedVacancy = body.researchVacancy.map { vacancy ->
            val validGuides = vacancy.possibleGuides.filter { it in facultyIds }
            if (validGuides.size != vacancy.possibleGuides.size) {
                return OpResponse.failure("One or more possible guides are invalid or not faculty")
            }
            vacancy.copy(possibleGuides = validGuides.toMutableList())
        }.toMutableList()

        // 3️⃣ Fetch existing requirement for this dept & session
        val existing = rrRepo.findBySessionIDAndDeptShortCode(session.id, body.deptShortCode)

        // 4️⃣ Preserve approvedVacancy and deptShortCode; request cannot modify them
        val now = UIDate.getCurrentDate()
        val toSave = if (existing == null) {
            // New record → deptShortCode from request
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = body.deptShortCode,
                researchVacancy = updatedVacancy,
                approvedVacancy = mutableListOf(), // New record, empty
                remarks = body.remarks.toMutableList(),
                isArchived = false,
                submittedOn = body.submittedOn ?: now,
                latestUpdatedOn = now
            )
        } else {
            // Existing record → preserve deptShortCode & sessionID
            existing.copy(
                researchVacancy = updatedVacancy,
                approvedVacancy = existing.approvedVacancy, // strictly preserve
                remarks = body.remarks.toMutableList(),
                isArchived = false, // always false
                latestUpdatedOn = now
            )
        }

        // 5️⃣ Save and return
        val saved = rrRepo.save(toSave)
        val msg = if (existing == null) "Research vacancies submitted successfully"
        else "Research vacancies updated successfully"
        return OpResponse.success(message = msg, data = saved)
    }

    // Inline mapper
    private fun ERPUserView.toMinView(): ERPMinView =
        ERPMinView(
            id = this.id,
            name = "${this.firstname} ${this.lastname}",
            email = this.email,
            deptShortCodes = this.deptShortCodes.toList(),
            erpID = this.erpID,

        )
}