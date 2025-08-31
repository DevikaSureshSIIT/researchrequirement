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
    // 1) Fetch status of research vacancies (by dept) — only when session open
    fun fetchResearchRequirements(deptShortCode: String): RestResponseEntity<ResearchRequirement> {
        val session = sessionRepo.findByIsOpenTrue()
            ?: return RestResponse.error("No active research recruitment session.", HttpStatus.FORBIDDEN)

        val rr = rrRepo.findBySessionIDAndDeptShortCode(session.id, deptShortCode)
            ?: return RestResponse.error("No research requirements found for department $deptShortCode in current session.", HttpStatus.NOT_FOUND)

        return RestResponse.withData(rr)
    }

    fun fetchFaculties(deptShortCode: String): RestResponseEntity<List<ERPMinView>> {
        val faculties = userRepo.findByUserType(UserType.FACULTY)
        val filtered = if (deptShortCode == "*") {
            faculties
        } else {
            faculties.filter { it.deptShortCodes.contains(deptShortCode) }
        }
        val minViews = filtered.map { it.toMinView() }
        return RestResponse.withData(minViews)
    }

    // 3) Upsert research requirement — with constraints from the doc
    fun upsertResearchRequirement(body: ResearchRequirementREq): OpResponse<ResearchRequirement> {
        val session: ResearchRecruitmentSession = sessionRepo.findByIsOpenTrue()
            ?: return OpResponse.failure("Active session is closed. Modification is not allowed.")

        // Existing record for dept+session?
        val existing = rrRepo.findBySessionIDAndDeptShortCode(session.id, body.deptShortCode)

        // If a seat matrix (approvedVacancyList) exists, enforce constraint: requested total <= approved total.
        if (existing != null && existing.approvedVacancyList.isNotEmpty()) {
            val requested = body.vacancyList.sumOf { it.numOfPost.toLong() }
            val approved = existing.approvedVacancyList.sumOf { it.numPosts.toLong() }
            if (requested > approved) {
                return OpResponse.failure("Requested posts ($requested) exceed approved vacancies ($approved).")
            }
        }

        val now = UIDate.getCurrentDate()

        val toSave = if (existing == null) {
            ResearchRequirement(
                id = ResearchRequirementID.create(),
                sessionID = session.id,
                deptShortCode = body.deptShortCode,
                vacancyList = body.vacancyList.toMutableList(),
                approvedVacancyList = mutableListOf(), // immutable via service rule
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                submittedOn = body.submittedOn ?: now,
                latestUpdatedOn = now
            )
        } else {
            // keep approvedVacancyList immutable; update others
            existing.copy(
                vacancyList = body.vacancyList.toMutableList(),
                // approvedVacancyList stays from existing
                remarks = body.remarks.toMutableList(),
                isArchived = body.isArchived,
                latestUpdatedOn = now
            )
        }

        val saved = rrRepo.save(toSave)
        val msg = if (existing == null) "Research vacancies submitted successfully" else "Research vacancies updated successfully"
        return OpResponse.success(message = msg, data = saved)
    }

    // Helper mapping without extra mappers/config (inline per your constraint)
    private fun ERPUserView.toMinView(): ERPMinView =
        ERPMinView(
            id = this.id,
            name = "${this.firstname} ${this.lastname}",
            email = this.email,
            deptShortCodes = this.deptShortCodes.toList()
        )
}
