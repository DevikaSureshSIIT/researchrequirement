package iit.pkd.researchrequirements.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

typealias RestResponseEntity<T> = ResponseEntity<RestResponse<T>>

sealed class RestResponse<T : Any> {
    abstract val message: String

    data class Success<T : Any> internal constructor(
        override val message: String,
        val data: T? = null
    ) : RestResponse<T>() {
        internal fun toResponseEntity(): RestResponseEntity<T> =
            ResponseEntity.ok().body(this)
    }

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
