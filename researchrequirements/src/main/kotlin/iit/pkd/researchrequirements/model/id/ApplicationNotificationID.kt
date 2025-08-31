package iit.pkd.researchrequirements.model.id

import iit.pkd.researchrequirements.model.common.ERPID



class ApplicationNotificationID(
    override val type: String,
    override val uuid: String = ERPID.generateUUID()
) : ERPID() {
    companion object {
        fun create(): ApplicationNotificationID =
            ApplicationNotificationID(type = "ApplicationNotificationID")

        fun empty(): ApplicationNotificationID =
            ApplicationNotificationID(type = "", uuid = "")
    }
}


