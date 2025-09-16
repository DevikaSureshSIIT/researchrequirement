package iit.pkd.researchrequirements.dto

import iit.pkd.researchrequirements.model.auxmodel.Remark
import iit.pkd.researchrequirements.model.auxmodel.SubAreaVacancy



data class DeptRequest(
    val deptShortCode: String
)


/**
 * Request payload for save / submit endpoints.
 *
 * Note:
 * - The server will set vacancyStatus / requirementStatus according to the endpoint (save or submit).
 * - "version" is optional in request; if provided it will be persisted/copied into the stored document.
 */
data class ResearchRequirementREq(
    val deptShortCode: String,
    val researchVacancy: MutableList<SubAreaVacancy> = mutableListOf(),
    val remarks: MutableList<Remark> = mutableListOf(),
    val version: String? = null
)
