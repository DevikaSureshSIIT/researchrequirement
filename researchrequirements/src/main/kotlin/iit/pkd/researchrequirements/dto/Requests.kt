package iit.pkd.researchrequirements.dto

import iit.pkd.researchrequirements.model.auxmodel.Remark
import iit.pkd.researchrequirements.model.auxmodel.SubArea

import iit.pkd.researchrequirements.model.id.ResearchRequirementID


//data class DeptRequest(
  //  val deptShortCode: String
//)


/**
 * Request payload for save / submit endpoints.
 *
 * requirementId (optional): when provided, upsert will operate on this ID.
 * If not provided, a new ResearchRequirement will be created (or existing by session+dept found).
 */
data class ResearchRequirementREq(
    val requirementId: ResearchRequirementID,
    val deptShortCode: String,
    val researchVacancy: MutableList<SubArea> = mutableListOf(),
    val remarks: MutableList<Remark> = mutableListOf(),
    //val version: String? = null
)
