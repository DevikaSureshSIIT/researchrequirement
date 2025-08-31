package iit.pkd.researchrequirements.model.requirement



import iit.pkd.researchrequirements.model.auxmodel.Remark
import iit.pkd.researchrequirements.model.auxmodel.ResearchVacancy
import iit.pkd.researchrequirements.model.auxmodel.Vacancy
import iit.pkd.researchrequirements.model.common.UIDate
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
    val vacancyList: MutableList<ResearchVacancy> = mutableListOf(),
    val approvedVacancyList: MutableList<Vacancy> = mutableListOf(),
    val remarks: MutableList<Remark> = mutableListOf(),
    val isArchived: Boolean = false,
    val submittedOn: UIDate,
    val latestUpdatedOn: UIDate
)
