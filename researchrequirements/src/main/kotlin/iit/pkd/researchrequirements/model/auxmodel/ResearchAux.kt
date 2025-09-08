package iit.pkd.researchrequirements.model.auxmodel


import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.UserID

data class ResearchVacancy(
    val subArea: String,
    val researchArea: String,
    val possibleGuides: List<UserID>, // list of faculty IDs
    val noOfVacancy: UInt
)

data class Remark(
    val who: String,
    val what: String,
    val date: UIDate,
)

// Replaces Vacancy → now SeatMatrix
data class SeatMatrix(
    val categoryID: CategoryID,
    val categoryName: String,
    val vacancy: UInt
)

// Simple ID for Category
data class CategoryID(val id: String)