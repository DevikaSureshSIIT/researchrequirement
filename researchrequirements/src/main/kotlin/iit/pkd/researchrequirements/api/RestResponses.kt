package iit.pkd.researchrequirements.api


import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

typealias RestResponseEntity<T> = ResponseEntity<RestResponse<T>>

sealed class RestResponse<T : Any> {
    abstract val message: String

    //@ConsistentCopyVisibility
    data class Success<T : Any> internal constructor(
        override val message: String,
        val data: T? = null
    ) : RestResponse<T>() {
        internal fun toResponseEntity(): RestResponseEntity<T> =
            ResponseEntity.ok().body(this)
    }

    //@ConsistentCopyVisibility
    data class Error<T : Any> internal constructor(
        override val message: String
    ) : RestResponse<T>() {
        internal fun toResponseEntity(status: HttpStatus = HttpStatus.BAD_REQUEST): RestResponseEntity<T> =
            ResponseEntity.status(status).body(this)
    }

    companion object {
        fun <T : Any> withData(data: T): RestResponseEntity<T> =
            Success("", data).toResponseEntity()

        fun <T : Any> withMessageAndData(msg: String, data: T): RestResponseEntity<T> =
            Success(msg, data).toResponseEntity()

        fun <T : Any> withMessage(msg: String): RestResponseEntity<T> =
            Success<T>(msg, null).toResponseEntity()

        fun <T : Any> error(
            message: String,
            status: HttpStatus = HttpStatus.BAD_REQUEST
        ): RestResponseEntity<T> =
            Error<T>(message).toResponseEntity(status)
    }
}

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
