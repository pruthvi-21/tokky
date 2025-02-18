package com.boxy.authenticator.domain.models

import com.boxy.authenticator.core.encoding.Base32
import com.boxy.authenticator.domain.models.enums.AccountEntryMethod
import com.boxy.authenticator.domain.models.otp.HotpInfo
import com.boxy.authenticator.domain.models.otp.OtpInfo
import com.boxy.authenticator.domain.models.otp.SteamInfo
import com.boxy.authenticator.domain.models.otp.TotpInfo
import com.boxy.authenticator.utils.Constants
import io.ktor.http.encodeURLPath
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class TokenEntry(
    val id: String,
    var issuer: String,
    var label: String,
    var thumbnail: Thumbnail,
    val otpInfo: OtpInfo,
    val createdOn: Long,
    var updatedOn: Long,
    val addedFrom: AccountEntryMethod,
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            issuer: String,
            label: String,
            thumbnail: Thumbnail = Thumbnail.Color(Constants.THUMBNAIL_COlORS.random()),
            otpInfo: OtpInfo,
            addedFrom: AccountEntryMethod,
        ): TokenEntry {
            return TokenEntry(
                id = Uuid.random().toString(),
                issuer = issuer,
                label = label,
                thumbnail = thumbnail,
                otpInfo = otpInfo,
                createdOn = Clock.System.now().toEpochMilliseconds(),
                updatedOn = Clock.System.now().toEpochMilliseconds(),
                addedFrom = addedFrom
            )
        }
    }
}

fun TokenEntry.generateOtpAuthUrl(): String {
    val type = when (otpInfo) {
        is SteamInfo -> "steam"
        is TotpInfo -> "totp"
        is HotpInfo -> "hotp"
    }

    val encodedLabel = "${issuer.encodeURLPath()}:${label.encodeURLPath()}"

    val uriBuilder = StringBuilder("otpauth://$type/$encodedLabel")

    val params = mutableListOf(
        "secret=${Base32.encode(otpInfo.secretKey)}",
        "algorithm=${otpInfo.algorithm}",
        "digits=${otpInfo.digits}",
        "issuer=${issuer.encodeURLPath()}"
    )

    when (otpInfo) {
        is SteamInfo -> {}
        is TotpInfo -> params.add("period=${otpInfo.period}")
        is HotpInfo -> params.add("counter=${otpInfo.counter}")
    }

    uriBuilder.append("?").append(params.joinToString("&"))

    return uriBuilder.toString()
}