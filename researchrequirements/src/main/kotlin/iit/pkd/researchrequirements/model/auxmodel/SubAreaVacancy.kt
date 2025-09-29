package iit.pkd.researchrequirements.model.auxmodel

import iit.pkd.researchrequirements.model.id.UserID

data class ResearchField(
    val researchField: String,
    val vacancy: UInt,
    val possibleGuide: List<UserID>
)

data class SubArea(
    val subArea: String,
    val researchFields:List<ResearchField>
)
