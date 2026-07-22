package com.vonbraunlabs.mindfocus.core.error

/**
 * The only failure vocabulary above the data layer. Repositories map `IOException`,
 * `HttpException` and `SQLiteException` onto these so nothing platform-specific reaches a
 * ViewModel.
 */
sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class Network(cause: Throwable) : DataError("Network unavailable", cause)

    class Server(val code: Int, val serverMessage: String?) :
        DataError("Server error $code: ${serverMessage.orEmpty()}")

    class Local(cause: Throwable) : DataError("Local storage error", cause)

    class NotFound(val id: String) : DataError("Session $id not found")

    class Unknown(cause: Throwable) : DataError("Unexpected error", cause)
}
