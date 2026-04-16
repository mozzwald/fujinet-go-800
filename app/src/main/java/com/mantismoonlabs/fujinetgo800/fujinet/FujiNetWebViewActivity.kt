package com.mantismoonlabs.fujinetgo800.fujinet

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mantismoonlabs.fujinetgo800.R
import com.mantismoonlabs.fujinetgo800.ui.ShellButton
import com.mantismoonlabs.fujinetgo800.ui.theme.Fuji800ATheme

class FujiNetWebViewActivity : ComponentActivity() {
    private companion object {
        const val WebUiUrl = "http://127.0.0.1:8000/"
        const val AllowedHost = "127.0.0.1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val activity = this
        setContent {
            Fuji800ATheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.web_ui_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                ShellButton(label = "Close", onClick = ::finish)
                            }
                        }
                        val webView = remember {
                            WebView(activity).apply {
                                settings.allowContentAccess = false
                                settings.allowFileAccess = false
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.setSupportMultipleWindows(false)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val uri = request?.url ?: return true
                                        return uri.scheme != "http" || uri.host != AllowedHost
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                loadUrl(WebUiUrl)
                            }
                        }
                        AndroidView(
                            factory = { webView },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
