package com.kis.mindfocus.data

import android.database.sqlite.SQLiteException
import com.kis.mindfocus.core.error.DataError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * The repository-side error boundary: runs [block] and converts any platform exception into a
 * [DataError]. [CancellationException] is rethrown first — swallowing it would break structured
 * concurrency and leave callers awaiting a coroutine that is already dead.
 */
internal inline fun <T> runCatchingData(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: DataError) {
        Result.failure(e)
    } catch (e: HttpException) {
        Result.failure(DataError.Server(e.code(), e.message()))
    } catch (e: IOException) {
        Result.failure(DataError.Network(e))
    } catch (e: SQLiteException) {
        Result.failure(DataError.Local(e))
    } catch (e: Throwable) {
        Result.failure(DataError.Unknown(e))
    }
