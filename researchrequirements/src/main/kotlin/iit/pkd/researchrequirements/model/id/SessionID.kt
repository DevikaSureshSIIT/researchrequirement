package iit.pkd.researchrequirements.model.id

import iit.pkd.researchrequirements.model.common.ERPID

class SessionID(
    override val type: String,
    override val uuid: String = ERPID.generateUUID()
) : ERPID() {
    companion object {
        fun create(): SessionID = SessionID(type = "SessionID")
        fun empty(): SessionID = SessionID(type = "", uuid = "")
    }
}
