package app.neokeyboard.ime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.neokeyboard.data.KeyboardSettings
import app.neokeyboard.haptics.KeyHaptics
import app.neokeyboard.sound.KeySoundPlayer
import app.neokeyboard.suggest.BundledLexicon
import app.neokeyboard.suggest.LexiconSuggestionEngine
import app.neokeyboard.suggest.UserLexiconStore
import app.neokeyboard.ui.NeoTheme
import app.neokeyboard.ui.keyboard.KeyboardScreen

/**
 * IME host Compose. InputMethodService không phải lifecycle-owner nên service tự cài ba
 * interface (Lifecycle/ViewModelStore/SavedStateRegistry) và gắn vào ComposeView TRƯỚC khi
 * trả từ onCreateInputView — nếu thiếu, ComposeView crash lúc attach. View dựng MỘT lần;
 * đổi field chỉ mutate [uiState], không dựng lại. Composition sống tới khi service chết.
 */
class NeoImeService : InputMethodService(),
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val vmStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = vmStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var uiState: KeyboardUiState
    private lateinit var settings: KeyboardSettings
    private lateinit var sound: KeySoundPlayer
    private lateinit var haptics: KeyHaptics
    private lateinit var controller: KeyboardController
    private lateinit var clipboardRepo: app.neokeyboard.clipboard.ClipboardRepository
    private var themeSnapshot by mutableStateOf<app.neokeyboard.theme.ThemeSnapshot?>(null)
    private var inputComposeView: ComposeView? = null

    private val wallpaperReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (::uiState.isInitialized) uiState.paletteRevision++
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        uiState = KeyboardUiState()
        settings = KeyboardSettings(this)
        sound = KeySoundPlayer(this)
        haptics = KeyHaptics(this)
        val store = runCatching { UserLexiconStore(this) }.getOrNull()
        val engine = LexiconSuggestionEngine(BundledLexicon.starter(), store) { settings.state.value.learnWords }
        clipboardRepo = runCatching { app.neokeyboard.clipboard.ClipboardRepository(this) }.getOrNull()
            ?.also { it.purgeOlderThan(settings.state.value.clipboardDays) }
            ?: app.neokeyboard.clipboard.ClipboardRepository(this)
        controller = KeyboardController(this, uiState, settings, sound, haptics, engine, clipboardRepo = clipboardRepo)
        refreshTheme()

        ContextCompat.registerReceiver(
            this, wallpaperReceiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        inputComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
            setViewTreeLifecycleOwner(this@NeoImeService)
            setViewTreeViewModelStoreOwner(this@NeoImeService)
            setViewTreeSavedStateRegistryOwner(this@NeoImeService)
            setContent {
                val prefs by settings.state.collectAsState()
                NeoTheme(
                    mode = prefs.mode,
                    colorSource = prefs.colorSource,
                    seed = prefs.customSeed,
                    paletteRevision = uiState.paletteRevision,
                    systemZero = themeSnapshot,
                ) {
                    KeyboardScreen(
                        ui = uiState,
                        prefs = prefs,
                        onKeyDown = controller::keyDown,
                        onKeyUp = controller::keyUp,
                        onPickCandidate = controller::pickCandidate,
                        onKeyCancel = controller::keyCancel,
                        onBackspaceRepeatStart = controller::backspaceRepeatStart,
                        onBackspaceWordDelete = controller::backspaceWordDelete,
                        onClipboard = controller::openClipboard,
                        onPasteClip = controller::pasteClip,
                        onCloseClipboard = controller::closeClipboard,
                        onPasteSystem = { controller.pasteFromSystem() },
                        clipboardPanel = {
                            app.neokeyboard.clipboard.ClipboardPanel(
                                repo = clipboardRepo,
                                onPaste = controller::pasteClip,
                                onClose = controller::closeClipboard,
                                onPasteSystem = { controller.pasteFromSystem() },
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        // Compose 1.8 lấy recomposer từ root của window (decor/parentPanel), không phải từ
        // ComposeView. Gắn owner lên decor trước khi view được attach, nếu không sẽ crash
        // "ViewTreeLifecycleOwner not found" ngay lần đầu mở bàn phím.
        window?.window?.decorView?.let { root ->
            root.setViewTreeLifecycleOwner(this)
            root.setViewTreeViewModelStoreOwner(this)
            root.setViewTreeSavedStateRegistryOwner(this)
        }
        window?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inputComposeView ?: ComposeView(this)
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        controller.onEditorChanged(info)
        if (!restarting) controller.onStartInput()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        refreshTheme()
    }

    private fun refreshTheme() {
        val client = app.neokeyboard.theme.ThemeBridgeClient(this)
        val fresh = try { client.loadSnapshot(this) } catch (_: Exception) { null }
        if (fresh != null && fresh.revision > (themeSnapshot?.revision ?: Long.MIN_VALUE)) {
            themeSnapshot = fresh
        } else if (themeSnapshot == null) {
            themeSnapshot = try { client.cacheRead(this) } catch (_: Exception) { null }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onFinishInput() {
        super.onFinishInput()
        controller.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        controller.onSelectionChanged(newSelStart, newSelEnd, candidatesEnd)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::uiState.isInitialized) uiState.paletteRevision++
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        vmStore.clear()
        runCatching { unregisterReceiver(wallpaperReceiver) }
        if (::sound.isInitialized) sound.release()
        if (::controller.isInitialized) controller.close()
        if (::settings.isInitialized) settings.close()
        super.onDestroy()
    }
}