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
        fun getCurrentResearchRequirements(@RequestBody req: DeptRequest): RestResponseEntity<ResearchRequirement> {
            return service.fetchCurrentResearchRequirements(req)
        }

        // 2️⃣ Fetch historical research requirements for CLOSED sessions
        @PostMapping("/history")
        fun getHistoricalResearchRequirements(@RequestBody req: DeptRequest): RestResponseEntity<List<ResearchRequirement>> {
            return service.fetchHistoricalResearchRequirements(req)
        }
    }


    //3) Fetch faculty
    @PostMapping("/faculty")
    fun getFaculty(@RequestBody deptShortCode: String): RestResponseEntity<List<ERPMinView>> =
        service.fetchFaculty(DeptRequest(deptShortCode.trim()))


    // 4) POST api/MENUPATH/researchrequirement (Upsert)
    @PostMapping("/researchrequirement")
    fun upsert(
        @RequestBody body: ResearchRequirementREq,
        @RequestHeader("userId") userId: String   // or from authentication context
    ): ResponseEntity<OpResponse<ResearchRequirement>> {

        // fetch current user from repo
        val currentUser = userRepo.findAll()
            .firstOrNull { it.id.uuid == userId }
            ?: throw RuntimeException("User not found")


        val op = service.upsertResearchRequirement(body, currentUser)
        return ResponseEntity.ok(op)
    }

}