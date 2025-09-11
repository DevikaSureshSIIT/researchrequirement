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
        val session = sessionRepo.findBySessionStatus(SessionStatus.OPEN)
            ?: return RestResponse.error("No active research recruitment session.", HttpStatus.FORBIDDEN)

        val rr = rrRepo.findBySessionIDAndDeptShortCode(session.id, req.deptShortCode)
            ?: return RestResponse.error("No research requirements found for department ${req.deptShortCode}.", HttpStatus.NOT_FOUND)

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


    // 3) Fetch faculties
    fun fetchFaculties(req: DeptRequest): RestResponseEntity<List<ERPMinView>> {
        val faculties = userRepo.findByUserType(UserType.FACULTY)
        val filtered = if (req.deptShortCode == "*") {
            faculties
        } else {
            faculties.filter { it.deptShortCodes.contains(req.deptShortCode) }
        }
        val minViews = filtered.map { it.toMinView() }
        return RestResponse.withData(minViews)
    }

    // 4) Upsert research requirement with new session rules
    fun upsertResearchRequirement(
        body: ResearchRequirementREq,
        currentUser: ERPUserView
    ): OpResponse<ResearchRequirement> {

        // 1️⃣ Fetch session (only OPEN allowed)
        val session = sessionRepo.findBySessionStatus(SessionStatus.OPEN)
            ?: return OpResponse.failure("Active session is not available.")

        // 2️⃣ Only Faculty can submit/update
        if (currentUser.userType != UserType.FACULTY) {
            return OpResponse.failure("Only Faculty are allowed to submit or update research requirements.")
        }

        // 3️⃣ Ensure session is OPEN
        if (session.status != SessionStatus.OPEN) {
            return OpResponse.failure("Research requirements can only be modified during an OPEN session.")
        }

        // 4️⃣ Fetch existing requirement for this dept & session
        val existing = rrRepo.findBySessionIDAndDeptShortCode(session.id, body.deptShortCode)

        // 5️⃣ Vacancy validation
        if (existing != null && existing.approvedVacancy.isNotEmpty()) {
            val requested = body.requestedVacancy.sumOf { it.vacancy.toLong() }
            val approved = existing.approvedVacancy.sumOf { it.vacancy.toLong() }
            if (requested > approved) {
                return OpResponse.failure(
                    "Requested posts ($requested) exceed approved vacancies ($approved)."
                )
            }
        }

        // 6️⃣ Temporary guide handling (if no guide, assign DRC/faculty responsible for dept)
        val updatedVacancy = body.requestedVacancy.map { vacancy ->
            if (vacancy.possibleGuides.isEmpty()) {
                val faculty = userRepo.findByUserType(UserType.FACULTY)
                    .firstOrNull { it.deptShortCodes.contains(body.deptShortCode) }
                if (faculty != null) vacancy.copy(possibleGuides = mutableListOf((faculty.id)) else vacancy
            } else vacancy
        }.toMutableList()

        // 7️⃣ Build object to save (approvedVacancy is immutable here)
        val now = UIDate.getCurrentDate()
        val toSave = if (existing == null) {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = body.deptShortCode,
                researchVacancy = updatedVacancy,
                approvedVacancy = body.approvedVacancy.toMutableList(), // Only used for NEW entry
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                submittedOn = body.submittedOn ?: now,
                latestUpdatedOn = now
            )
        } else {
            existing.copy(
                researchVacancy = updatedVacancy,
                approvedVacancy = existing.approvedVacancy, // immutable
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                latestUpdatedOn = now
            )
        }

        // 8️⃣ Save and return
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
            erpID = this.erpID
        )
}