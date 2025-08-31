package iit.pkd.researchrequirements.model.common



import java.util.*

abstract class ERPID {
    abstract val type: String
    abstract val uuid: String

    companion object {
        fun generateUUID(): String = UUID.randomUUID().toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ERPID) return false
        return this.uuid == other.uuid && this.type == other.type
    }

    override fun hashCode(): Int = 31 * type.hashCode() + uuid.hashCode()
}
