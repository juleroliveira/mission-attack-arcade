package com.arcade.missionattack

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializa a SplashScreen oficial do Android sem piscar tela branca
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Manter a tela sempre ligada durante a gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        WebView.setWebContentsDebuggingEnabled(true)

        val wv = WebView(this).apply {
            setBackgroundColor(0xFF020307.toInt())
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        webView = wv

        val settings: WebSettings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MissionAttack", "Jogo carregado com sucesso: $url")
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("MissionAttack", "Erro ao carregar: ${error?.description}")
            }
        }

        wv.webChromeClient = WebChromeClient()

        // Ponte Nativa para Futuras Integrações com Google AdMob e Google Play Billing
        wv.addJavascriptInterface(PlayStoreBridge(this, wv), "PlayStoreBridge")
        wv.addJavascriptInterface(PlayStoreBridge(this, wv), "AndroidBridge")

        setContentView(wv)

        wv.loadUrl("file:///android_asset/index.html")

        // Botão Voltar do Android
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView?.evaluateJavascript(
                    "if (typeof window.handleAndroidBack === 'function') { window.handleAndroidBack(); } else { false; }"
                ) { result ->
                    if ("true" != result) {
                        finish()
                    }
                } ?: finish()
            }
        })
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.apply {
            onResume()
            resumeTimers()
        }
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        webView?.apply {
            evaluateJavascript("if (typeof gameState !== 'undefined' && gameState === 'PLAYING') { gameState = 'PAUSED'; }", null)
            onPause()
            pauseTimers()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    /**
     * Ponte Nativa oficial para Google Mobile Ads (AdMob) e Google Play Billing (IAP)
     */
    class PlayStoreBridge(private val activity: Activity, private val webView: WebView) {

        @JavascriptInterface
        fun isNativeBridgeReady(): Boolean = true

        @JavascriptInterface
        fun showRewardedAd() {
            activity.runOnUiThread {
                Toast.makeText(activity, "Google AdMob: Anúncio premiado pronto para ativação", Toast.LENGTH_SHORT).show()
                // Callback de recompensa no JavaScript do jogo
                webView.evaluateJavascript("if (typeof onRewardEarned === 'function') { onRewardEarned(); }", null)
            }
        }

        @JavascriptInterface
        fun showInterstitialAd() {
            activity.runOnUiThread {
                Log.d("MissionAttack", "Google AdMob: Intersticial solicitado")
            }
        }

        @JavascriptInterface
        fun purchaseItem(productId: String) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Google Play Billing: Produto '$productId' pronto", Toast.LENGTH_SHORT).show()
                // Callback de sucesso da compra no jogo
                webView.evaluateJavascript("if (typeof onPurchaseSuccess === 'function') { onPurchaseSuccess('$productId'); }", null)
            }
        }
    }
}
