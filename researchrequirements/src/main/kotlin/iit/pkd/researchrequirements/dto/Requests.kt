package iit.pkd.researchrequirements.dto

import iit.pkd.researchrequirements.model.auxmodel.Remark
import iit.pkd.researchrequirements.model.auxmodel.ResearchVacancy
import iit.pkd.researchrequirements.model.auxmodel.SeatMatrix
import iit.pkd.researchrequirements.model.common.UIDate


// For POST /researchrequirements and /faculties
data class DeptRequest(
    val deptShortCode: String
)

// Upsert payload (explicitly named per your request: ResearchRequirementREq)
data class ResearchRequirementREq(
    val deptShortCode: String,
    val requestedVacancy: List<ResearchVacancy> = emptyList(),
    val remarks: List<Remark> = emptyList(),
    val isArchived: Boolean = false,
    val submittedOn: UIDate? = null, // if null, service will set UIDate.getCurrentDate()
    val approvedVacancy: List<SeatMatrix> = emptyList(),   // add this
)
