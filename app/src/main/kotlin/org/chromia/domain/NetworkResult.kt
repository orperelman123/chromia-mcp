package org.chromia.domain

import kotlinx.serialization.json.JsonObject

typealias JsonResult = NetworkResult<JsonObject>

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>()
}
