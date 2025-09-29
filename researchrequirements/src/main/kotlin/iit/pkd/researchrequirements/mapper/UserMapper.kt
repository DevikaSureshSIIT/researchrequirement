package iit.pkd.researchrequirements.mapper

import iit.pkd.researchrequirements.model.user.ERPUserView
import iit.pkd.researchrequirements.model.user.ERPMinView
/** Mapper ERPUserView → ERPMinView */
fun ERPUserView.toMinView(): ERPMinView =
    ERPMinView(
        id = this.id,
        name = "${this.firstname} ${this.lastname}",
        email = this.email,
        deptShortCodes = this.deptShortCodes.toList(),
        erpID = this.erpID
    )
