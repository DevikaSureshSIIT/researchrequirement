package iit.pkd.researchrequirements.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import reactor.core.publisher.Mono



annotation class ConsistentCopyVisibility














typealias MonoRestResponseEntity<T> = Mono<ResponseEntity<RestResponse<T>>>

/**
 * Represents a sealed class for handling structured REST API responses with a generic type.
 *
 * @param T The type of the data that the response contains.
 *
 */
sealed class RestResponse<T : Any> {

    /**
     * Represents a message associated with the response.
     *
     * This message is intended to provide contextual information about the outcome
     * of the operation associated with the response.
     */
    abstract val message: String

    /**
     * Represents a successful response in a REST API.
     *
     * This class is a subtype of [RestResponse] and indicates that a request
     * has been processed successfully. It optionally carries additional data
     * relevant to the successful response.
     *
     * @param T The type of the additional data, restricted to non-nullable types.
     * @property message A message providing more context about the success.
     * @property data The optional data associated with the successful response.
     */
    @ConsistentCopyVisibility
    data class Success<T : Any> internal constructor(
        override val message: String,
        val data: T? = null
    ) : RestResponse<T>() {
        /**
         * Converts the current instance of `Success<T>` into a `RestResponseEntity<T>`.
         *
         * @return A `RestResponseEntity<T>` created with an HTTP 200 OK status and the current object as the response body.
         */
        internal fun toResponseEntity(): MonoRestResponseEntity<T> =
            Mono.just(ResponseEntity.ok().body(this))
    }

    /**
     * Represents an error response within a REST API context.
     *
     * This class encapsulates an error message and provides functionality
     * to convert the error response into a `ResponseEntity` with a customizable HTTP status code.
     *
     * @param T The type parameter that extends `Any`, representing the generic type of the response.
     * @property message The error message describing the nature of the error.
     */
    data class Error<T : Any>(
        override val message: String
    ) : RestResponse<T>() {
        internal fun toResponseEntity(status: HttpStatus = HttpStatus.BAD_REQUEST): MonoRestResponseEntity<T> =
            Mono.just(ResponseEntity.status(status).body(this))
    }

    /**
     * Companion object providing utility methods to generate various types of `RestResponseEntity` or `ResponseEntity`.
     */
    companion object {
        /**
         * Creates a new `RestResponseEntity` containing a successful response with the provided data.
         *
         * @param T The type of the data.
         * @param data The data to be included in the response.
         * @return A `RestResponseEntity` representing a success response with the provided data.
         */
        fun <T : Any> withData(data: T): MonoRestResponseEntity<T> =
            Success("", data).toResponseEntity()

        /**
         * Creates a `RestResponseEntity` that represents a successful response with a message and associated data.
         *
         * @param msg The message to include in the response.
         * @param data The data to include in the response.
         * @return An instance of `RestResponseEntity` containing the provided message and data.
         */
        fun <T : Any> withMessageAndData(msg: String, data: T): MonoRestResponseEntity<T> =
            Success(msg, data).toResponseEntity()

        /**
         * Creates a `RestResponseEntity` of type `T` containing the specified message.
         *
         * @param msg The message to be included in the response.
         * @return A `RestResponseEntity<T>` containing the provided message.
         */
        fun <T : Any> withMessage(msg: String): MonoRestResponseEntity<T> =
            Success<T>(msg, null).toResponseEntity()

        /**
         * Generates an error response wrapped in a `ResponseEntity`.
         *
         * @param message The error message to be included in the response.
         * @param status The HTTP status to be returned with the error response. Defaults to `HttpStatus.BAD_REQUEST`.
         * @return A `ResponseEntity` containing the error message and status as a `RestResponse.Error`.
         */
        fun <T : Any> error(
            message: String,
            status: HttpStatus = HttpStatus.BAD_REQUEST
        ): MonoRestResponseEntity<T> =
            Error<T>(message).toResponseEntity(status)
    }
}