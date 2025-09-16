package iit.pkd.researchrequirements.model.auxmodel

import iit.pkd.researchrequirements.model.id.UserID

data class ResearchFieldVacancy(
    val researchField: String,
    val vacancy: UInt,
    val possibleGuide: MutableList<UserID> = mutableListOf()
)

data class SubAreaVacancy(
    val subArea: String,
    val vacancies: MutableList<ResearchFieldVacancy> = mutableListOf()
)
