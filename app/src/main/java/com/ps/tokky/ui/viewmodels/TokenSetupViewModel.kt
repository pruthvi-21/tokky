package com.ps.tokky.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ps.tokky.data.models.TokenEntry
import com.ps.tokky.data.models.otp.HotpInfo
import com.ps.tokky.data.models.otp.OtpInfo
import com.ps.tokky.data.models.otp.SteamInfo
import com.ps.tokky.data.models.otp.TotpInfo
import com.ps.tokky.domain.models.TokenFormEvent
import com.ps.tokky.domain.models.TokenFormState
import com.ps.tokky.domain.usecases.DeleteTokenUseCase
import com.ps.tokky.domain.usecases.FetchTokenByIdUseCase
import com.ps.tokky.domain.usecases.InsertTokenUseCase
import com.ps.tokky.domain.usecases.ReplaceExistingTokenUseCase
import com.ps.tokky.helpers.TokenFormValidator
import com.ps.tokky.utils.AccountEntryMethod
import com.ps.tokky.utils.Base32
import com.ps.tokky.utils.OTPType
import com.ps.tokky.utils.TokenNameExistsException
import com.ps.tokky.utils.TokenSetupMode
import com.ps.tokky.utils.cleanSecretKey
import kotlinx.coroutines.launch
import java.util.Date

private const val TAG = "TokenSetupViewModel"

class TokenSetupViewModel(
    private val insertTokenUseCase: InsertTokenUseCase,
    private val fetchTokenByIdUseCase: FetchTokenByIdUseCase,
    private val deleteTokenUseCase: DeleteTokenUseCase,
    private val replaceExistingTokenUseCase: ReplaceExistingTokenUseCase,
    private val formValidator: TokenFormValidator,
) : ViewModel() {
    private var initialState = TokenFormState()

    private var _uiState = mutableStateOf(initialState)
    val uiState: State<TokenFormState> = _uiState

    var tokenSetupMode: TokenSetupMode = TokenSetupMode.NEW
    private var tokenToUpdate: TokenEntry? = null

    val showBackPressDialog = mutableStateOf(false)
    val showDeleteTokenDialog = mutableStateOf(false)
    val showDuplicateTokenDialog = mutableStateOf(DuplicateTokenDialogArgs(false))

    data class DuplicateTokenDialogArgs(
        val show: Boolean,
        val token: TokenEntry? = null,
        val existingToken: TokenEntry? = null,
    )

    fun setInitialStateFromTokenWithId(tokenId: String) {
        viewModelScope.launch {
            fetchTokenByIdUseCase(tokenId)
                .fold(
                    onSuccess = { token ->
                        if (token != null) {
                            tokenToUpdate = token
                            tokenSetupMode = TokenSetupMode.UPDATE
                            setInitialStateFromToken(token)
                        }
                    },
                    onFailure = {

                    }
                )
        }
    }

    fun setInitialStateFromUrl(authUrl: String) {
        tokenSetupMode = TokenSetupMode.URL
        setInitialStateFromToken(TokenEntry.buildFromUrl(authUrl))
    }

    private fun setInitialStateFromToken(token: TokenEntry) {
        var tokenState = TokenFormState(
            issuer = token.issuer,
            label = token.label,
            thumbnailColor = token.thumbnailColor,
            type = token.type,
            secretKey = Base32.encode(token.otpInfo.secretKey),
            algorithm = token.otpInfo.algorithm,
            digits = token.otpInfo.digits.toString()
        )

        tokenState = when (token.type) {
            OTPType.TOTP -> tokenState.copy(period = (token.otpInfo as TotpInfo).period.toString())

            OTPType.HOTP -> tokenState.copy(counter = (token.otpInfo as HotpInfo).counter.toString())

            OTPType.STEAM -> tokenState
        }
        tokenState = updateFieldVisibilityState(tokenState)

        initialState = tokenState
        _uiState.value = tokenState
    }

    fun onEvent(event: TokenFormEvent) {
        when (event) {
            is TokenFormEvent.IssuerChanged -> {
                updateState {
                    copy(
                        issuer = event.issuer,
                        validationErrors = validationErrors - "issuer"
                    )
                }
            }

            is TokenFormEvent.LabelChanged -> {
                updateState { copy(label = event.label) }
            }

            is TokenFormEvent.SecretKeyChanged -> {
                updateState {
                    copy(
                        secretKey = event.secretKey.cleanSecretKey(),
                        validationErrors = validationErrors - "secretKey"
                    )
                }
            }

            is TokenFormEvent.TypeChanged -> {
                updateState { copy(type = event.type) }
                _uiState.value = updateFieldVisibilityState(_uiState.value)
            }

            is TokenFormEvent.ThumbnailColorChanged -> {
                updateState { copy(thumbnailColor = event.thumbnailColor) }
            }

            is TokenFormEvent.AlgorithmChanged -> {
                updateState { copy(algorithm = event.algorithm) }
            }

            is TokenFormEvent.PeriodChanged -> {
                updateState {
                    copy(
                        period = event.period,
                        validationErrors = validationErrors - "period"
                    )
                }
            }

            is TokenFormEvent.DigitsChanged -> {
                updateState {
                    copy(
                        digits = event.digits,
                        validationErrors = validationErrors - "digits"
                    )
                }
            }

            is TokenFormEvent.CounterChanged -> {
                updateState {
                    copy(
                        counter = event.counter,
                        validationErrors = validationErrors - "counter"
                    )
                }
            }

            is TokenFormEvent.EnableAdvancedOptionsChanged -> {
                updateState { copy(enableAdvancedOptions = event.enableAdvancedOptions) }
            }

            is TokenFormEvent.Submit -> {
                validateInputs(event)
            }
        }
    }

    private fun updateState(newState: TokenFormState.() -> TokenFormState) {
        _uiState.value = _uiState.value.newState()
    }

    private fun validateInputs(event: TokenFormEvent.Submit) {
        val issuerResult = formValidator.validateIssuer(_uiState.value.issuer)
        val secretKeyResult = formValidator.validateSecretKey(_uiState.value.secretKey)
        val periodResult = formValidator.validatePeriod(_uiState.value.period)
        val counterResult = formValidator.validateCounter(_uiState.value.counter)

        _uiState.value = _uiState.value.copy(
            validationErrors = mapOf(
                "issuer" to issuerResult.errorMessage.takeIf { !issuerResult.isValid },
                "secretKey" to secretKeyResult.errorMessage.takeIf { !secretKeyResult.isValid },
                "period" to periodResult.errorMessage.takeIf { !periodResult.isValid },
                "counter" to counterResult.errorMessage.takeIf { !counterResult.isValid }
            )
        )

        val state = _uiState.value

        fun buildOtpInfo(): OtpInfo {
            return when (state.type) {
                OTPType.TOTP -> {
                    val totpResults =
                        listOf(issuerResult, secretKeyResult, periodResult)
                    val hasError = totpResults.any { !it.isValid }
                    if (hasError) throw Exception()

                    TotpInfo(
                        Base32.decode(uiState.value.secretKey),
                        state.algorithm,
                        state.digits.toInt(),
                        state.period.toInt(),
                    )
                }

                OTPType.HOTP -> {
                    val hotpResults =
                        listOf(issuerResult, secretKeyResult, counterResult)
                    val hasError = hotpResults.any { !it.isValid }
                    if (hasError) throw Exception()

                    HotpInfo(
                        Base32.decode(uiState.value.secretKey),
                        state.algorithm,
                        state.digits.toInt(),
                        state.counter.toLong(),
                    )
                }

                OTPType.STEAM -> {
                    val steamResults = listOf(issuerResult, secretKeyResult)
                    val hasError = steamResults.any { !it.isValid }
                    if (hasError) throw Exception()

                    SteamInfo(Base32.decode(uiState.value.secretKey))
                }
            }
        }

        viewModelScope.launch {
            try {
                val otpInfo = buildOtpInfo()

                val token = when (tokenSetupMode) {
                    TokenSetupMode.NEW,
                    TokenSetupMode.URL,
                        -> {
                        var newToken = TokenEntry.buildNewToken(
                            issuer = state.issuer,
                            label = state.label,
                            type = state.type,
                            thumbnailColor = state.thumbnailColor,
                            otpInfo = otpInfo,
                            addedFrom = AccountEntryMethod.FORM,
                        )

                        if (tokenSetupMode == TokenSetupMode.URL) {
                            newToken = newToken.copy(addedFrom = AccountEntryMethod.QR_CODE)
                        }

                        newToken
                    }

                    TokenSetupMode.UPDATE -> {
                        tokenToUpdate?.copy(
                            issuer = state.issuer,
                            label = state.label,
                            thumbnailColor = state.thumbnailColor,
                            updatedOn = Date()
                        ) ?: throw IllegalStateException("No token ID available for update")
                    }
                }
                onValidationSuccess(token, event)
            } catch (e: Exception) {
                Log.i(TAG, "validateInputs: ", e)
            }
        }
    }

    private fun onValidationSuccess(token: TokenEntry, event: TokenFormEvent.Submit) {
        viewModelScope.launch {
            insertTokenUseCase(token, true)
                .fold(
                    onSuccess = {
                        event.onComplete()
                    },
                    onFailure = { exception ->
                        if (exception is TokenNameExistsException) {
                            exception.token?.let { event.onDuplicate(token, it) }
                        } else {
                            Log.e(
                                TAG,
                                "onValidationSuccess: Unknown error while inserting",
                                exception
                            )
                            // TODO: display a error
                        }
                    }
                )
        }
    }

    private fun updateFieldVisibilityState(state: TokenFormState): TokenFormState {
        return when (state.type) {
            OTPType.TOTP -> state.copy(
                isAlgorithmFieldVisible = true,
                isDigitsFieldVisible = true,
                isPeriodFieldVisible = true,
                isCounterFieldVisible = false,
            )

            OTPType.HOTP -> state.copy(
                isAlgorithmFieldVisible = true,
                isDigitsFieldVisible = true,
                isPeriodFieldVisible = false,
                isCounterFieldVisible = true,
            )

            OTPType.STEAM -> state.copy(
                isAlgorithmFieldVisible = false,
                isDigitsFieldVisible = false,
                isPeriodFieldVisible = false,
                isCounterFieldVisible = false,
            )
        }
    }

    // Reset form
    fun dispose() {
        val resetState = TokenFormState()
        _uiState.value = resetState
        initialState = resetState
    }

    fun isFormUpdated(): Boolean {
        // Ignoring enableAdvancedOptions and validationErrors
        // since they don't represent user-modified data.
        val currentState = _uiState.value
        return currentState.copy(
            enableAdvancedOptions = initialState.enableAdvancedOptions,
            validationErrors = initialState.validationErrors,
        ) != initialState
    }

    fun deleteToken(tokenId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            deleteTokenUseCase(tokenId)
            onComplete()
        }
    }

    fun replaceExistingToken(existingToken: TokenEntry, token: TokenEntry) {
        viewModelScope.launch {
            replaceExistingTokenUseCase(existingToken, token)
        }
    }
}
