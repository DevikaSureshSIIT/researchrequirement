package iit.pkd.researchrequirements.model.auxmodel

import iit.pkd.researchrequirements.model.id.UserID

enum class DecisionStatus {
    ACCEPTED,
    REJECTED,
    PENDING
}

data class Decision(
    val date: String,
    val nameOfDecider: String,
    val userIDofDecider: UserID,
    val status: DecisionStatus,
    val remark: String
)

