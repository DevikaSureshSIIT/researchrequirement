package iit.pkd.researchrequirements.controller

import iit.pkd.researchrequirements.api.RestResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.service.ResearchRequirementService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/MENUPATH")
class ResearchRequirementController(
    private val service: ResearchRequirementService
) {

    @PostMapping("/researchrequirements")
    fun getCurrentResearchRequirements(@RequestBody deptShortCode: String): RestResponseEntity<ResearchRequirement> {
        val op = service.fetchCurrentResearchRequirements(deptShortCode.trim())
        return if (op.success) {
            RestResponse.withMessageAndData(op.message, op.data!!)
        } else {
            RestResponse.error(op.message) // your RestResponse already handles HttpStatus
        }
    }

    @PostMapping("/researchrequirements/history")
    fun getHistoricalResearchRequirements(@RequestBody deptShortCode: String): RestResponseEntity<List<ResearchRequirement>> {
        val op = service.fetchHistoricalResearchRequirements(deptShortCode.trim())
        return if (op.success) {
            RestResponse.withMessageAndData(op.message, op.data!!)
        } else {
            RestResponse.error(op.message)
        }
    }

    @PostMapping("/faculty")
    fun getFaculty(@RequestBody deptShortCode: String): RestResponseEntity<List<ERPMinView>> {
        val op = service.fetchFaculty(deptShortCode.trim())
        return if (op.success) {
            RestResponse.withMessageAndData(op.message, op.data!!)
        } else {
            RestResponse.error(op.message)
        }
    }

    @PostMapping("/researchrequirement/save")
    fun saveRequirement(@RequestBody body: ResearchRequirementREq): RestResponseEntity<ResearchRequirement> {
        val op = service.saveResearchRequirement(body)
        return if (op.success) {
            RestResponse.withMessageAndData(op.message, op.data!!)
        } else {
            RestResponse.error(op.message)
        }
    }

    @PostMapping("/researchrequirement/submit")
    fun submitRequirement(@RequestBody body: ResearchRequirementREq): RestResponseEntity<ResearchRequirement> {
        val op = service.submitResearchRequirement(body)
        return if (op.success) {
            RestResponse.withMessageAndData(op.message, op.data!!)
        } else {
            RestResponse.error(op.message)
        }
    }
}
