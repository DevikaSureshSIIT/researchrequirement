package iit.pkd.researchrequirements.repo


import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.session.ResearchRecruitmentSession
import iit.pkd.researchrequirements.model.session.SessionID
import iit.pkd.researchrequirements.model.user.ERPUserView
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ResearchRequirementRepository : MongoRepository<ResearchRequirement, String> {
    fun findBySessionIDAndDeptShortCode(sessionID: SessionID, deptShortCode: String): ResearchRequirement?
}

@Repository
interface ResearchRecruitmentSessionRepository : MongoRepository<ResearchRecruitmentSession, String> {
    fun findByIsOpenTrue(): ResearchRecruitmentSession?
}

@Repository
interface ERPUserViewRepository : MongoRepository<ERPUserView, String> {
    fun findByUserType(userType: iit.pkd.researchrequirements.model.user.UserType): List<ERPUserView>
}
