package iit.pkd.researchrequirements.api

class OpResponse<T : Any> private constructor(
    val success: Boolean,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T : Any> success(message: String = "", data: T? = null): OpResponse<T> =
            OpResponse(success = true, message = message, data = data)

        fun <T : Any> failure(message: String): OpResponse<T> =
            OpResponse(success = false, message = message)
    }
}