package iit.pkd.researchrequirements.model.requirement

import iit.pkd.researchrequirements.model.auxmodel.ApprovedSeatMatrix
import iit.pkd.researchrequirements.model.auxmodel.Decision
import iit.pkd.researchrequirements.model.auxmodel.Remark

import iit.pkd.researchrequirements.model.auxmodel.SubArea

import iit.pkd.researchrequirements.model.id.ResearchRequirementID
import iit.pkd.researchrequirements.model.id.SessionID

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("researchRequirements")
data class ResearchRequirement(
    @Id val id: ResearchRequirementID,
    @Indexed val sessionID: SessionID,
    @Indexed val deptShortCode: String,
    val requestedVacancy: List<SubArea> ,
    val approvedVacancy: List<ApprovedSeatMatrix> ,
    val vacancyStatus: VacancyStatus,
    val requirementStatus: RequirementStatus,
    val remarks: List<Remark>,
    val decisions: List<Decision>,

    val isArchived: Boolean = false
)
