package iit.pkd.researchrequirements.repo

import iit.pkd.researchrequirements.model.department.Department
import iit.pkd.researchrequirements.model.id.DeptID
import iit.pkd.researchrequirements.model.id.ResearchRequirementID
import iit.pkd.researchrequirements.model.id.SessionID
import iit.pkd.researchrequirements.model.id.UserID
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.session.ResearchRecruitmentSession
import iit.pkd.researchrequirements.model.session.SessionStatus
import iit.pkd.researchrequirements.model.user.ERPUserView
import iit.pkd.researchrequirements.model.user.UserType
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ResearchRequirementRepository : MongoRepository<ResearchRequirement, ResearchRequirementID> {
    fun findBySessionIDAndDeptShortCode(sessionID: SessionID, deptShortCode: String): List<ResearchRequirement>
    fun findAllByDeptShortCodeAndIsArchivedTrue(deptShortCode: String): List<ResearchRequirement>
    fun findAllBySessionIDAndDeptShortCode(sessionID: SessionID, deptShortCode: String): List<ResearchRequirement>
}

@Repository
interface ResearchRecruitmentSessionRepository : MongoRepository<ResearchRecruitmentSession, SessionID> {
    /**
     * If multiple OPEN sessions exist, the latest one by endDate will be returned.
     */
    fun findTopByStatusOrderByEndDateDesc(status: SessionStatus): ResearchRecruitmentSession?
}

@Repository
interface ERPUserViewRepository : MongoRepository<ERPUserView, UserID> {
    fun findByUserType(userType: UserType): List<ERPUserView>
}

@Repository
interface DepartmentRepository : MongoRepository<Department, DeptID> {
    fun findByDeptShortCode(deptShortCode: String): Department?
}
