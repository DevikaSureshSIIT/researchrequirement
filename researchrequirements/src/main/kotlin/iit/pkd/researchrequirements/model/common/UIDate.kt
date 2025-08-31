package iit.pkd.researchrequirements.model.common



import java.time.LocalDate

data class UIDate(
    val year: Int, val month: Int, val day: Int
): Comparable<Any> {
    init {
        require(month in 1..12) { "Month must be between 1 and 12" }
        require(day in 1..31) { "Day must be between 1 and 31" }
        require(year > 0) { "Year must be non-zero" }
    }

    override fun toString(): String {
        return String.format("%02d-%s-%04d", day, monthNames[month - 1], year)
    }

    override fun compareTo(other: Any): Int {
        if (other !is UIDate) throw IllegalArgumentException("Cannot compare UIDate with ${other::class.simpleName}")
        return when {
            this.year != other.year -> this.year.compareTo(other.year)
            this.month != other.month -> this.month.compareTo(other.month)
            else -> this.day.compareTo(other.day)
        }
    }

    companion object {
        private val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        fun getCurrentDate(): UIDate {
            val d = LocalDate.now()
            return UIDate(d.year, d.monthValue, d.dayOfMonth)
        }
    }
}
