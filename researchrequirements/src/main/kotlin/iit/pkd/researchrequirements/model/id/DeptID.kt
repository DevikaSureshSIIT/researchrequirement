package iit.pkd.researchrequirements.model.id

import iit.pkd.researchrequirements.model.common.ERPID

class DeptID(
    override val type: String,
    override val uuid: String = ERPID.generateUUID()
) : ERPID() {
    companion object {
        fun create(): DeptID = DeptID(type = "DeptID")
        fun empty(): DeptID = DeptID(type = "", uuid = "")
    }
}
