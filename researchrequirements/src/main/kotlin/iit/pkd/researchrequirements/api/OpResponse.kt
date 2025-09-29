package iit.pkd.researchrequirements.api

//import org.jetbrains.kotlin.com.intellij.util.Function

import reactor.core.publisher.Mono


/**
 * Represents the result of an operation that can either succeed or fail.
 *
 * This class is generic and can wrap any data type as part of a successful operation,
 * while also providing a message and a success flag indicating the operation's status.
 *
 * @param T The type of data that can be included in the response.
 * @property success Indicates whether the operation was successful.
 * @property message A message providing details about the operation's result.
 * @property data Optional data associated with a successful operation, or null if the operation failed.
 *
 */
class OpResponse<T : Any> private constructor(
    val success: Boolean,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T : Any> successAsMono(message: String = "", data: T? = null): Mono<OpResponse<T>> =
            Mono.just(OpResponse(success = true, message = message, data = data))

        fun <T : Any> failureAsMono(message: String): Mono<OpResponse<T>> =
            Mono.just(OpResponse(success = false, message = message))

        fun <T : Any> success(message: String = "", data: T? = null): OpResponse<T> =
            OpResponse(success = true, message = message, data = data)

        fun <T : Any> failure(message: String): OpResponse<T> =
            OpResponse(success = false, message = message)
    }
}