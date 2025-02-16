package com.boxy.authenticator

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.boxy.authenticator.navigation.BoxyNavHost
import com.boxy.authenticator.navigation.addAuthRoute
import com.boxy.authenticator.navigation.addExportTokensRoute
import com.boxy.authenticator.navigation.addHomeRoute
import com.boxy.authenticator.navigation.addImportTokensRoute
import com.boxy.authenticator.navigation.addQrScannerRoute
import com.boxy.authenticator.navigation.addSettingsRoute
import com.boxy.authenticator.navigation.addTokenSetupRoute
import com.boxy.authenticator.ui.theme.BoxyTheme
import com.boxy.authenticator.ui.util.BindScreenshotBlockerEffect
import com.boxy.authenticator.ui.viewmodels.LocalSettingsViewModel
import com.boxy.authenticator.ui.viewmodels.SettingsViewModel
import dev.icerock.moko.biometry.compose.BindBiometryAuthenticatorEffect
import dev.icerock.moko.biometry.compose.rememberBiometryAuthenticatorFactory
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersHolder

@Composable
fun App() {
    val biometryFactory = rememberBiometryAuthenticatorFactory()
    val biometryAuthenticator = biometryFactory.createBiometryAuthenticator()
    val settingsViewModel: SettingsViewModel = koinViewModel {
        ParametersHolder(mutableListOf(biometryAuthenticator))
    }
    BindBiometryAuthenticatorEffect(biometryAuthenticator)
    BindScreenshotBlockerEffect(settingsViewModel.isBlockScreenshotsEnabled.value)

    KoinContext {
        CompositionLocalProvider(LocalSettingsViewModel provides settingsViewModel) {
            BoxyTheme(theme = settingsViewModel.appTheme.value) {
                Surface {
                    BoxyNavHost {
                        addAuthRoute()
                        addHomeRoute()
                        addQrScannerRoute()
                        addTokenSetupRoute()
                        addSettingsRoute()
                        addExportTokensRoute()
                        addImportTokensRoute()
                    }
                }
            }
        }
    }
}