package iit.pkd.researchrequirements.model.department

import iit.pkd.researchrequirements.model.id.DeptID
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("departments")
data class Department(
    @Id val id: DeptID,
    @Indexed(unique = true)
    val deptShortCode: String,
    val deptName: String
)
