package iit.pkd.researchrequirements.controller

import iit.pkd.researchrequirements.api.MonoRestResponseEntity
import iit.pkd.researchrequirements.api.RestResponse

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.dto.ResearchRequirementREq
import iit.pkd.researchrequirements.model.id.ResearchRequirementID
import iit.pkd.researchrequirements.model.requirement.ResearchRequirement
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.service.FacultyService
import iit.pkd.researchrequirements.service.ResearchRequirementService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/MENUPATH")
class ResearchRequirementController(
    private val service: ResearchRequirementService,
    private val facultyService: FacultyService
) {

    @PostMapping("/researchrequirements")
    fun getCurrentResearchRequirements(@RequestBody deptShortCode: String): Mono<MonoRestResponseEntity<ResearchRequirement>> {
        return service.fetchCurrentResearchRequirements(deptShortCode.trim())
            .map { op ->
                if (op.success) RestResponse.withMessageAndData(op.message, op.data!!)
                else RestResponse.error(op.message)
            }
    }

    @PostMapping("/researchrequirements/history")
    fun getHistoricalResearchRequirements(@RequestBody deptShortCode: String): Mono<MonoRestResponseEntity<List<ResearchRequirement>>> {
        return service.fetchHistoricalResearchRequirements(deptShortCode.trim())
            .map { op ->
                if (op.success) RestResponse.withMessageAndData(op.message, op.data!!)
                else RestResponse.error(op.message)
            }
    }

    @PostMapping("/faculty")
    fun getFaculty( @RequestBody deptShortCode: String): Mono<MonoRestResponseEntity<List<ERPMinView>>> {
        return facultyService.fetchFaculty(deptShortCode.trim())
            .map { op ->
                if (op.success) RestResponse.withMessageAndData(op.message, op.data!!)
                else RestResponse.error(op.message)
            }
    }

   @PostMapping("/researchrequirement/save")
    fun saveRequirement(@Valid @RequestBody body: ResearchRequirement): Mono<MonoRestResponseEntity<ResearchRequirementID>> {

        return service.saveResearchRequirement(body)
            .map { op ->
                if (op.success) RestResponse.withMessageAndData(op.message, op.data!!)
                else RestResponse.error(op.message)
            }
    }
    @PostMapping("/researchrequirement/submit")
    fun submitRequirement(@Valid @RequestBody body: ResearchRequirement): Mono<MonoRestResponseEntity<Nothing>> {
        return service.submitResearchRequirement(body)
            .map { op ->
                if (op.success) RestResponse.withMessage<Nothing>(op.message) // data will be null
                else RestResponse.withMessage<Nothing>(op.message) // data null on failure as well
            }
    }

}

