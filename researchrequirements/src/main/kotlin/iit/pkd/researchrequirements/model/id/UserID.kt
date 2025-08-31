package iit.pkd.researchrequirements.model.id

import iit.pkd.researchrequirements.model.common.ERPID




//import jakarta.persistence.Embeddable


class UserID(
    override val type: String,
    override val uuid: String = ERPID.generateUUID()
) : ERPID() {
    companion object {
        fun create(): UserID =
            UserID(type = "UserID")
        fun empty(): UserID = UserID(type = "", uuid = "")
    }
}


