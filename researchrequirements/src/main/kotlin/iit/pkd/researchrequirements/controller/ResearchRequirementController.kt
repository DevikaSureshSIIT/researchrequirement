package iit.pkd.researchrequirements.controller

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.DeptRequest
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.service.ResearchRequirementService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/MENUPATH")
class ResearchRequirementController(
    private val service: ResearchRequirementService
) {

    /** Fetch research requirements for OPEN session */
    @PostMapping("/researchrequirements")
    fun getCurrentResearchRequirements(@RequestBody dept: String): RestResponseEntity<ResearchRequirement> {
        return service.fetchCurrentResearchRequirements(DeptRequest(dept.trim()))
    }

    /** Fetch research requirement history for CLOSED sessions */
    @PostMapping("/researchrequirements/history")
    fun getHistoricalResearchRequirements(@RequestBody dept: String): RestResponseEntity<List<ResearchRequirement>> {
        return service.fetchHistoricalResearchRequirements(DeptRequest(dept.trim()))
    }

    /** Fetch faculty for given department */
    @PostMapping("/faculty")
    fun getFaculty(@RequestBody dept: String): RestResponseEntity<List<ERPMinView>> {
        return service.fetchFaculty(DeptRequest(dept.trim()))
    }

    /** Save research requirement as draft */
    @PostMapping("/researchrequirement/save")
    fun saveRequirement(@RequestBody body: ResearchRequirementREq): ResponseEntity<OpResponse<ResearchRequirement>> {
        val op = service.saveResearchRequirement(body)
        return ResponseEntity.ok(op)
    }

    /** Submit research requirement (with validation) */
    @PostMapping("/researchrequirement/submit")
    fun submitRequirement(@RequestBody body: ResearchRequirementREq): ResponseEntity<OpResponse<ResearchRequirement>> {
        val op = service.submitResearchRequirement(body)
        return ResponseEntity.ok(op)
    }
}
