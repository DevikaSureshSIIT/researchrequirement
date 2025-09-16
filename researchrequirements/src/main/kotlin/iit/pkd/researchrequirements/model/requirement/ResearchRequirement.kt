package iit.pkd.researchrequirements.model.requirement

import iit.pkd.researchrequirements.model.auxmodel.Decision
import iit.pkd.researchrequirements.model.auxmodel.Remark
import iit.pkd.researchrequirements.model.auxmodel.SeatMatrix
import iit.pkd.researchrequirements.model.auxmodel.SubAreaVacancy
import iit.pkd.researchrequirements.model.id.ResearchRequirementID
import iit.pkd.researchrequirements.model.session.SessionID
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("researchRequirements")
data class ResearchRequirement(
    @Id val id: ResearchRequirementID,
    @Indexed val sessionID: SessionID,
    @Indexed val deptShortCode: String,
    val researchVacancy: MutableList<SubAreaVacancy> = mutableListOf(),
    val approvedVacancy: MutableList<SeatMatrix> = mutableListOf(),
    val vacancyStatus: VacancyStatus,
    val requirementStatus: RequirementStatus,
    val remarks: MutableList<Remark> = mutableListOf(),
    val decisions: MutableList<Decision> = mutableListOf(),
    val version: String,
    val isArchived: Boolean = false
)
