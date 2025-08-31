package iit.pkd.researchrequirements.model.auxmodel


import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.UserID

data class ResearchVacancy(
    val nameOfPost: String,
    val researchAreas: List<String>,
    val numOfPost: UInt,
    val faculty: UserID
)

data class Remark(
    val who: String,
    val what: String,
    val date: UIDate,
)

data class Vacancy(
    val categoryID: CategoryID,
    val categoryName: String,
    val numPosts: UInt
)

// Simple ID for Category (the doc references CategoryID but doesn't define it;
// keep it minimal without extra classes/config).
data class CategoryID(val id: String)
