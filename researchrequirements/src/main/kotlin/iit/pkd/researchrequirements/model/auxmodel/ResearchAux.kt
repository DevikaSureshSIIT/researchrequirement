package iit.pkd.researchrequirements.model.auxmodel


import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.UserID

data class ResearchVacancy(
    val subArea: String,
    val researchAreas: MutableList<String> =mutableListOf(),
    val vacancy: UInt,
    val possibleGuides: MutableList<UserID> = mutableListOf()

)

data class Remark(
    val who: String,
    val what: String,
    val date: UIDate,
)

// Replaces Vacancy → now SeatMatrix
data class SeatMatrix(
    val categoryID: CategoryID,
    val vacancy: UInt
)

// Simple ID for Category
data class CategoryID(val id: String)