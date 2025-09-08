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
    // 1) Fetch status of research vacancies
    fun fetchResearchRequirements(req: DeptRequest): RestResponseEntity<ResearchRequirement> {
        val session = sessionRepo.findBySessionStatus(SessionStatus.OPEN)
            ?: return RestResponse.error("No active research recruitment session.", HttpStatus.FORBIDDEN)

        val rr = rrRepo.findBySessionIDAndDeptShortCode(session.sessionID, req.deptShortCode)
            ?: return RestResponse.error("No research requirements found for department ${req.deptShortCode}.", HttpStatus.NOT_FOUND)

        return RestResponse.withData(rr)
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
    fun upsertResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        val session: ResearchRecruitmentSession? = sessionRepo.findBySessionStatus(SessionStatus.OPEN)
        if (session == null) {
            return OpResponse.failure("Active session is not available.")
        }

        when (session.sessionStatus) {
            SessionStatus.CLOSED -> {
                return OpResponse.failure("Session is CLOSED. Modification not allowed. Archiving data.")
            }

            SessionStatus.APPROVED -> {
                val existing = rrRepo.findBySessionIDAndDeptShortCode(session.sessionID, body.deptShortCode)
                if (existing != null && existing.approvedVacancy.isNotEmpty()) {
                    val requested = body.requestedVacancy.sumOf { it.noOfVacancy.toLong() }
                    val approved = existing.approvedVacancy.sumOf { it.vacancy.toLong() }
                    if (requested > approved) {
                        return OpResponse.failure("Requested posts ($requested) exceed approved vacancies ($approved).")
                    }
                }
            }

            SessionStatus.OPEN -> {
                // normal insert flow allowed
            }
        }

        val now = UIDate.getCurrentDate()

        val existing = rrRepo.findBySessionIDAndDeptShortCode(session.sessionID, body.deptShortCode)
        val toSave = if (existing == null) {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.sessionID,
                deptShortCode = body.deptShortCode,
                requestedVacancy = body.requestedVacancy.toMutableList(),
                approvedVacancy = mutableListOf(),
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                submittedOn = body.submittedOn ?: now,
                latestUpdatedOn = now
            )
        } else {
            existing.copy(
                requestedVacancy = body.requestedVacancy.toMutableList(),
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                latestUpdatedOn = now
            )
        }

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