package com.ps.tokky.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ps.tokky.data.models.TokenEntry
import com.ps.tokky.data.repositories.TokensRepository
import com.ps.tokky.utils.FileHelper
import com.ps.tokky.utils.TokenBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val tokensRepository: TokensRepository,
) : ViewModel() {

    data class ImportItem internal constructor(
        val token: TokenEntry,
        var checked: Boolean,
        var isDuplicate: Boolean,
    )

    var isFileUnlocked = mutableStateOf(false)

    var showPasswordDialog = mutableStateOf(false)
    var showDuplicateWarningDialog = mutableStateOf(false)

    private var _tokensToImport = mutableStateOf<List<ImportItem>>(emptyList())
    val tokensToImport: State<List<ImportItem>> = _tokensToImport

    private var _importError = mutableStateOf<String?>(null)
    val importError: State<String?> = _importError

    fun importAccounts() {
        viewModelScope.launch {
            val tokens = _tokensToImport.value.filter { !it.isDuplicate }.map { it.token }
            tokensRepository.insertAccounts(tokens)
        }
    }

    fun checkToken(id: String, checked: Boolean) {
        val updatedList = _tokensToImport.value.map { item ->
            if (item.token.id == id) item.copy(checked = checked) else item
        }
        _tokensToImport.value = updatedList
    }

    fun importAccountsFromFile(context: Context, fileUri: Uri, password: String) {
        _importError.value = null
        val fileData = FileHelper.readFromFile(
            context = context,
            path = fileUri,
            password = password
        ) ?: run {
            _importError.value =
                "Failed to read or decrypt the file. Please check the password or file integrity."
            return
        }

        val tokens = convertJsonToTokens(fileData) ?: run {
            _importError.value = "Failed to parse the file."
            return
        }

        isFileUnlocked.value = true
        buildTokensToImportList(tokens)
    }

    private fun buildTokensToImportList(tokens: List<TokenEntry>) {
        viewModelScope.launch {
            val existingAccountNames = tokensRepository.getAllTokens()
                .map { it.name }
                .toSet()

            val newAccounts = mutableListOf<TokenEntry>()
            val duplicateAccounts = mutableListOf<TokenEntry>()

            val importItems = tokens.map { token ->
                val isDuplicate = existingAccountNames.contains(token.name)

                if (isDuplicate) {
                    duplicateAccounts.add(token)
                } else {
                    newAccounts.add(token)
                }

                ImportItem(
                    token = token,
                    checked = true,
                    isDuplicate = isDuplicate,
                )
            }

            _tokensToImport.value = importItems.sortedBy { it.token.name }
        }
    }

    private fun convertJsonToTokens(fileData: String): List<TokenEntry>? {
        return try {
            val jsonArray = JSONArray(fileData)
            val importList = ArrayList<TokenEntry>()

            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                val token = TokenBuilder.buildFromExportJson(jsonObj)

                importList.add(token)
            }

            importList
        } catch (jsonException: JSONException) {
            null
        }
    }

    fun updateToken(tokenId: String, issuer: String, label: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val updatedList = _tokensToImport.value.map { item ->
                if (item.token.id == tokenId) {
                    val updatedToken = item.token.copy(issuer = issuer, label = label)
                    item.copy(token = updatedToken, isDuplicate = checkIfDuplicate(updatedToken))
                } else item
            }
            _tokensToImport.value = updatedList
            onComplete()
        }
    }

    private suspend fun checkIfDuplicate(token: TokenEntry): Boolean {
        return tokensRepository.findDuplicateToken(token.issuer, token.label, token.id) != null
    }
}