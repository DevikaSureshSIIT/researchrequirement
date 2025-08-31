package iit.pkd.researchrequirements.model.id



import iit.pkd.researchrequirements.model.common.ERPID
//import jakarta.persistence.Embeddable


class ResearchRequirementID(
    override val type: String,
    override val uuid: String = ERPID.generateUUID()
) : ERPID() {
    companion object {
        fun create(): ResearchRequirementID =
            ResearchRequirementID(type = "ResearchRequirementID")

        fun empty(): ResearchRequirementID =
            ResearchRequirementID(type = "", uuid = "")
    }
}


