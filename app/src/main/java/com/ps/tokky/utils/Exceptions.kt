package com.ps.tokky.utils

import com.ps.tokky.data.models.TokenEntry
import java.io.IOException

class InvalidSecretKeyException(msg: String? = "") : Exception(msg)

class TokenNameExistsException(val token: TokenEntry? = null, msg: String? = null) : Exception(msg)
class EmptyURLContentException(msg: String? = "") : Exception(msg)
class BadlyFormedURLException(msg: String? = "") : Exception(msg)

class EncodingException(msg: String? = null, cause: Throwable? = null) : IOException(msg, cause)

class OtpInfoException : Exception {
    constructor(cause: Throwable?) : super(cause)

    constructor(message: String?) : super(message)
}