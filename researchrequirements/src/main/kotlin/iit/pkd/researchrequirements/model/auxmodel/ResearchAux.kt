package iit.pkd.researchrequirements.model.auxmodel


import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.CategoryID
import iit.pkd.researchrequirements.model.id.UserID



data class Remark(
    val who: String,
    val what: String,
    val date: UIDate,
)

// Replaces Vacancy → now SeatMatrix
data class ApprovedSeatMatrix(
    val categoryID: CategoryID,
    val vacancy: UInt
)
