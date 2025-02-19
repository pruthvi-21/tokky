package com.boxy.authenticator.ui.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import boxy_authenticator.composeapp.generated.resources.Res
import boxy_authenticator.composeapp.generated.resources.export_accounts
import boxy_authenticator.composeapp.generated.resources.export_accounts_summary
import boxy_authenticator.composeapp.generated.resources.import_accounts
import boxy_authenticator.composeapp.generated.resources.import_accounts_summary
import boxy_authenticator.composeapp.generated.resources.last_backup_on
import boxy_authenticator.composeapp.generated.resources.preference_category_transfer_accounts
import com.boxy.authenticator.core.AppSettings
import com.boxy.authenticator.utils.formatMillis
import com.jw.preferences.Preference
import com.jw.preferences.PreferenceCategory
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun TransferAccounts(
    snackbarHostState: SnackbarHostState,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
) {
    val appSettings: AppSettings = koinInject()

    PreferenceCategory(
        title = { Text(stringResource(Res.string.preference_category_transfer_accounts)) },
    ) {
        Preference(
            title = { Text(text = stringResource(Res.string.export_accounts)) },
            summary = {
                val lastBackupTimestamp = appSettings.getLastBackupTimestamp()
                if (lastBackupTimestamp <= 0L) {
                    Text(text = stringResource(Res.string.export_accounts_summary))
                } else {
                    val formattedDate = formatMillis(lastBackupTimestamp)
                    Text(stringResource(Res.string.last_backup_on, formattedDate))
                }
            },
            onClick = { onNavigateToExport() },
        )
        Preference(
            title = { Text(text = stringResource(Res.string.import_accounts)) },
            summary = { Text(text = stringResource(Res.string.import_accounts_summary)) },
            onClick = { onNavigateToImport() },
            showDivider = false,
        )
    }
}
