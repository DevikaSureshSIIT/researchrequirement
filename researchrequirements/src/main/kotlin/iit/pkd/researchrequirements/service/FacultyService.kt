package iit.pkd.researchrequirements.service

import iit.pkd.researchrequirements.api.OpResponse
import iit.pkd.researchrequirements.mapper.toMinView
import iit.pkd.researchrequirements.model.user.ERPMinView
import iit.pkd.researchrequirements.model.user.ERPUserView
import iit.pkd.researchrequirements.model.user.UserType
import iit.pkd.researchrequirements.repo.DepartmentRepository
import iit.pkd.researchrequirements.repo.ERPUserViewRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class FacultyService(
    private val userRepo: ERPUserViewRepository,
    private val deptRepo: DepartmentRepository
) {

    /** Fetch faculty members for a department */
    fun fetchFaculty(deptShortCode: String): Mono<OpResponse<List<ERPMinView>>> {
        if (deptShortCode != "*") {
            deptRepo.findByDeptShortCode(deptShortCode)
                ?: return OpResponse.failureAsMono("Invalid department short code: $deptShortCode")
        }

        val faculty = if (deptShortCode == "*") {
            userRepo.findByUserType(UserType.FACULTY)
        } else {
            userRepo.findByUserTypeAndDeptShortCodesContaining(UserType.FACULTY, deptShortCode)
        }

        val minViews = faculty.map { it.toMinView() }
        return Mono.just(OpResponse.success("Faculty fetched successfully.", minViews))
    }


}
