package iit.pkd.researchrequirements.model.id

import iit.pkd.researchrequirements.model.common.ERPID

class CategoryID(
    override val type: String,
    override val uuid: String = ERPID.generateUUID()
) : ERPID() {
    companion object {
        fun create(): CategoryID = CategoryID(type = "CategoryID")
        fun empty(): CategoryID = CategoryID(type = "", uuid = "")
    }
}
