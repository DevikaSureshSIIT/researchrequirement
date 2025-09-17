package iit.pkd.researchrequirements.model.session



import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.ResearchRequirementID // not used; kept minimal
import iit.pkd.researchrequirements.model.id.SessionID
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document



enum class SessionStatus { OPEN,  CLOSED }

@Document("researchRecruitmentSessions")
data class ResearchRecruitmentSession(
    @Id val id: SessionID,
    @Indexed(unique = true) val name: String,
    val status: SessionStatus,
    val description: String,
    val endDate: UIDate
)
