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

        val rr = rrRepo.findBySessionIDAndDeptShortCode(session.sessionID, req.deptShortCode)
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


    // 2) Fetch faculties
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

    // 3) Upsert research requirement with new session rules
    fun upsertResearchRequirement(
        body: ResearchRequirementREq,
        currentUser: ERPUserView
    ): OpResponse<ResearchRequirement> {

        // 1️⃣ Fetch session (current active session)
        val session = sessionRepo.findBySessionStatus(SessionStatus.OPEN)
            ?: return OpResponse.failure("Active session is not available.")

        // 2️⃣ Role-based access control
        when (currentUser.userType) {
            UserType.FACULTY -> {
                // Faculty can only update during OPEN session
                if (session.sessionStatus != SessionStatus.OPEN) {
                    return OpResponse.failure("Faculty can only submit during OPEN session.")
                }
            }

            UserType.DRC -> {
                // DRC can submit requestedVacancy/remarks in OPEN or APPROVED
                if (session.sessionStatus == SessionStatus.CLOSED) {
                    return OpResponse.failure("DRC cannot modify a CLOSED session.")
                }
            }

            UserType.ADMIN -> {
                // Admin can do anything
            }

            else -> {
                return OpResponse.failure("Unauthorized role: ${currentUser.userType}")
            }
        }

        // 3️⃣ Session status checks (existing logic)
        when (session.sessionStatus) {
            SessionStatus.CLOSED -> {
                return OpResponse.failure("Session is CLOSED. Modification not allowed.")
            }

            SessionStatus.APPROVED -> {
                val existing = rrRepo.findBySessionIDAndDeptShortCode(session.sessionID, body.deptShortCode)
                if (existing != null && existing.approvedVacancy.isNotEmpty()) {
                    val requested = body.requestedVacancy.sumOf { it.noOfVacancy.toLong() }
                    val approved = existing.approvedVacancy.sumOf { it.vacancy.toLong() }
                    if (requested > approved) {
                        return OpResponse.failure(
                            "Requested posts ($requested) exceed approved vacancies ($approved)."
                        )
                    }
                }
            }

            SessionStatus.OPEN -> {
                // insert/update allowed
            }
        }

        // 4️⃣ Temporary guide handling
        val updatedVacancy = body.requestedVacancy.map { vacancy ->
            if (vacancy.possibleGuides.isEmpty()) {
                val drc = userRepo.findByUserType(UserType.DRC)
                    .firstOrNull { it.deptShortCodes.contains(body.deptShortCode) }
                if (drc != null) vacancy.copy(possibleGuides = listOf(drc.id)) else vacancy
            } else vacancy
        }.toMutableList()

        // 5️⃣ Prevent non-admin from modifying approvedVacancy
        val existing = rrRepo.findBySessionIDAndDeptShortCode(session.sessionID, body.deptShortCode)
        if (existing != null && body.approvedVacancy != existing.approvedVacancy && currentUser.userType != UserType.ADMIN) {
            return OpResponse.failure("Only Admin / Acad team can edit approved seat matrix.")
        }

        // 6️⃣ Prepare object to save
        val now = UIDate.getCurrentDate()
        val toSave = if (existing == null) {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.sessionID,
                deptShortCode = body.deptShortCode,
                requestedVacancy = updatedVacancy,
                approvedVacancy = body.approvedVacancy.toMutableList(),
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                submittedOn = body.submittedOn ?: now,
                latestUpdatedOn = now
            )
        } else {
            existing.copy(
                requestedVacancy = updatedVacancy,
                approvedVacancy = if (currentUser.userType == UserType.ADMIN) body.approvedVacancy.toMutableList() else existing.approvedVacancy,
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                latestUpdatedOn = now
            )
        }

        // 7️⃣ Save and return
        val saved = rrRepo.save(toSave)
        val msg = if (existing == null) "Research vacancies submitted successfully" else "Research vacancies updated successfully"
        return OpResponse.success(message = msg, data = saved)
    }


    // Inline mapper
    private fun ERPUserView.toMinView(): ERPMinView =
        ERPMinView(
            id = this.id,
            name = "${this.firstname} ${this.lastname}",
            email = this.email,
            deptShortCodes = this.deptShortCodes.toList()
        )
}