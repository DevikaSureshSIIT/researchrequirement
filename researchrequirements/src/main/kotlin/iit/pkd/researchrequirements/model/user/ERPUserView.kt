package iit.pkd.researchrequirements.model.user




import iit.pkd.researchrequirements.model.common.ERPID
import iit.pkd.researchrequirements.model.id.UserID
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

enum class UserType {
    STUDENT,
    RESEARCHSCHOLAR,

    FACULTY,
    STAFF,
    SYSTEMCREATED,
    EXTERNAL

}


@Document("erpUsers")
data class ERPUserView(
    @Id val id: UserID,
    @NotBlank @Size(max = 100) @Indexed private val username: String,
    @NotBlank @Size(max = 100) val firstname: String,
    @NotBlank @Size(max = 100) val lastname: String,
    @NotBlank @Size(max = 100) @Email @Indexed val email: String,
    val deptShortCodes: MutableSet<String> = mutableSetOf(),
    @Indexed val userType: UserType,
    @Indexed val erpID: UserID
)

// Response-min view requested in the doc (a.k.a. ERPMinView / ERPUserMinView)
data class ERPMinView(
    val id: UserID,
    val name: String, // firstname + lastname
    val email: String,
    val deptShortCodes: List<String>,
    val erpID: ERPID,
    )
