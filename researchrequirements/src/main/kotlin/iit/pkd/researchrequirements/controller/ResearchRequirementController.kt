package iit.pkd.researchrequirements.controller

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.DeptRequest
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.service.ResearchRequirementService


import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*



@RestController
@RequestMapping("/api/MENUPATH")
class ResearchRequirementController(
    private val service: ResearchRequirementService
) {
    @PostMapping("/researchrequirements")
    fun getResearchRequirements(@RequestBody deptShortCode: String): RestResponseEntity<ResearchRequirement> =
        service.fetchResearchRequirements(DeptRequest(deptShortCode.trim()))

    @PostMapping("/faculty")
    fun getFaculty(@RequestBody deptShortCode: String): RestResponseEntity<List<ERPMinView>> =
        service.fetchFaculties(DeptRequest(deptShortCode.trim()))


    // 3) POST api/MENUPATH/researchrequirement (Upsert)
    @PostMapping("/researchrequirement")
    fun upsert(@RequestBody body: ResearchRequirementREq): ResponseEntity<OpResponse<ResearchRequirement>> {
        val op = service.upsertResearchRequirement(body)
        return ResponseEntity.ok(op)
    }
}