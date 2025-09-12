package iit.pkd.researchrequirements.controller

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.api.RestResponseEntity
import iit.pkd.researchrequirements.dto.DeptRequest
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.id.UserID
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.repo.ERPUserViewRepository
import iit.pkd.researchrequirements.service.ResearchRequirementService


import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*



@RestController
@RequestMapping("/api/MENUPATH")
class ResearchRequirementController(
    private val service: ResearchRequirementService,
    private val userRepo: ERPUserViewRepository
) {
    @RestController
    @RequestMapping("/api/MENUPATH/researchrequirements")
    class ResearchRequirementController(
        private val service: ResearchRequirementService
    ) {

        // 1️⃣ Fetch research requirements for the OPEN session
        @PostMapping
        fun getCurrentResearchRequirements(@RequestBody deptShortCode: String): RestResponseEntity<ResearchRequirement> {
            val req = DeptRequest(deptShortCode.trim())
            return service.fetchCurrentResearchRequirements(req)
        }

        @PostMapping("/history")
        fun getHistoricalResearchRequirements(@RequestBody deptShortCode: String): RestResponseEntity<List<ResearchRequirement>> {
            val req = DeptRequest(deptShortCode.trim())
            return service.fetchHistoricalResearchRequirements(req)
        }

    }


    //3) Fetch faculty
    @PostMapping("/faculty")
    fun getFaculty(@RequestBody deptShortCode: String): RestResponseEntity<List<ERPMinView>> =
        service.fetchFaculty(DeptRequest(deptShortCode.trim()))


    // 4) POST api/MENUPATH/researchrequirement (Upsert)

        @PostMapping("/researchrequirement")
        fun upsert(@RequestBody body: ResearchRequirementREq): ResponseEntity<OpResponse<ResearchRequirement>> {

        // Call service; no user required
        val op = service.upsertResearchRequirement(body, null)
        return ResponseEntity.ok(op)

}}