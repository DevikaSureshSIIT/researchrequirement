package iit.pkd.researchrequirements.model.session



import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.ResearchRequirementID // not used; kept minimal
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

typealias SessionID = String

enum class SessionStatus { OPEN, APPROVED, CLOSED }

@Document("researchRecruitmentSessions")
data class ResearchRecruitmentSession(
    @Id val sessionID: SessionID,
    @Indexed(unique = true) val sessionName: String,
    val sessionStatus: SessionStatus,
    val description: String,
    val endDate: UIDate
)
