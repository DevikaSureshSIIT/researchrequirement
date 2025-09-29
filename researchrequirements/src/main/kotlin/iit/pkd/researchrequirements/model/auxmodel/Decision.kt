package iit.pkd.researchrequirements.model.auxmodel

import iit.pkd.researchrequirements.model.common.UIDate
import iit.pkd.researchrequirements.model.id.UserID

enum class DecisionStatus {
    ACCEPTED,
    REJECTED,
    PENDING
}

/**
 * Decision record (kept minimal). In this menu only DRC acts — the service doesn't attempt to model acad/research actor flows.
 */
data class Decision(
    val date: UIDate,
    val nameOfDecider: String,
    val userIDofDecider: UserID,
    val status: DecisionStatus,
    val remark: String
)
