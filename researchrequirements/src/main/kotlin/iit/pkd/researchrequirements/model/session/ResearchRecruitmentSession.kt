package iit.pkd.researchrequirements.model.session



import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.ResearchRequirementID // not used; kept minimal
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

typealias SessionID = String

@Document("researchRecruitmentSessions")
data class ResearchRecruitmentSession(
    @Id val id: SessionID,
    @Indexed(unique = true) val name: String,
    val description: String,
    val isOpen: Boolean = true,
    val endDate: UIDate
)
