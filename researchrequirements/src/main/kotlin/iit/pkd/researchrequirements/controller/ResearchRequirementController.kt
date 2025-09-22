package iit.pkd.researchrequirements.controller

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.service.ResearchRequirementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * NOTE: first three endpoints accept plain text body (e.g. "CSE") as per your requirement.
 */
@RestController
@RequestMapping("/api/MENUPATH")
class ResearchRequirementController(
    private val service: ResearchRequirementService
) {

    /** Fetch research requirements for the latest OPEN session for the given department (body is plain text e.g. "CSE") */
    @PostMapping("/researchrequirements")
    fun getCurrentResearchRequirements(@RequestBody deptShortCode: String): RestResponseEntity<ResearchRequirement>{
        val dept = deptShortCode.trim()
        return service.fetchCurrentResearchRequirements(dept)
    }

    /** Fetch research requirement history for CLOSED/archived sessions (body is plain text e.g. "CSE") */
    @PostMapping("/researchrequirements/history")
    fun getHistoricalResearchRequirements(@RequestBody deptShortCode: String): RestResponseEntity<List<ResearchRequirement>> {
        val dept = deptShortCode.trim()
        return service.fetchHistoricalResearchRequirements(dept)
    }

    /** Fetch faculty for given department (body is plain text e.g. "CSE") */
    @PostMapping("/faculty")
    fun getFaculty(@RequestBody deptShortCode: String): RestResponseEntity<List<ERPMinView>> {
        val dept = deptShortCode.trim()
        return service.fetchFaculty(dept)
    }

    /** Save research requirement (upsert as draft). Body is ResearchRequirementREq (JSON). */
    @PostMapping("/researchrequirement/save")
    fun saveRequirement(@RequestBody body: ResearchRequirementREq): ResponseEntity<OpResponse<ResearchRequirement>> {
        val op = service.saveResearchRequirement(body)
        return ResponseEntity.ok(op)
    }

    /** Submit research requirement (upsert with validation). Body is ResearchRequirementREq (JSON). */
    @PostMapping("/researchrequirement/submit")
    fun submitRequirement(@RequestBody body: ResearchRequirementREq): ResponseEntity<OpResponse<ResearchRequirement>> {
        val op = service.submitResearchRequirement(body)
        return ResponseEntity.ok(op)
    }
}
