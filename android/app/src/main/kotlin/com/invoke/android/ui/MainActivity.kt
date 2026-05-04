package com.invoke.android.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.invoke.android.agent.LocalModelClient
import com.invoke.android.agent.SupabaseAuthClient
import com.invoke.android.stt.SttEngine
import com.invoke.android.ui.onboarding.BubbleOpacity
import com.invoke.android.ui.onboarding.BubbleSize
import com.invoke.android.ui.onboarding.ConnectionStatus
import com.invoke.android.ui.onboarding.OnboardingStep
import com.invoke.android.ui.onboarding.SetupType
import com.invoke.android.ui.theme.InvokeColor
import com.invoke.android.ui.theme.InvokeSpacing
import com.invoke.android.ui.theme.bodyStyle
import com.invoke.android.ui.theme.dp
import com.invoke.android.ui.theme.headingStyle
import com.invoke.android.ui.theme.rounded
import com.invoke.android.ui.theme.titleStyle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var scroll: ScrollView
    private lateinit var root: LinearLayout

    private val authClient = SupabaseAuthClient()
    private val modelClient = LocalModelClient()
    private var localConnectionStatus = ConnectionStatus.IDLE
    private var localConnectionMessage = ""
    private var currentTab = HomeTab.HOME
    private var showSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        scroll = ScrollView(this).apply {
            setBackgroundColor(InvokeColor.Background)
            isFillViewport = true
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        root.removeAllViews()
        hideKeyboard()

        if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            home()
            return
        }

        when (step()) {
            OnboardingStep.WELCOME -> welcomeScreen()
            OnboardingStep.CHOOSE_SETUP -> chooseSetupScreen()
            OnboardingStep.PERMISSIONS -> permissionsScreen()
            OnboardingStep.BUBBLE -> bubbleScreen()
            OnboardingStep.LOCAL_MODEL -> localModelScreen()
            OnboardingStep.ACCOUNT -> accountScreen()
            OnboardingStep.PERSONALIZATION -> personalizationScreen()
            OnboardingStep.HOME -> finishOnboarding()
        }
    }

    private fun welcomeScreen() {
        onboardingScaffold(
            stepIndex = 1,
            title = "INVOKE",
            subtitle = "Voice actions for every app",
            body = "Turn speech into messages, notes, snippets, and app actions. Use cloud sync or keep everything local.",
            primaryText = "Get started",
            primaryAction = { go(OnboardingStep.CHOOSE_SETUP) },
            secondaryText = "Sign in",
            secondaryAction = {
                saveSetup(SetupType.CLOUD)
                go(OnboardingStep.ACCOUNT)
            }
        ) {
            micHero()
            chips("Dictate anywhere", "Clean up text", "Save snippets", "Run local AI")
            trustNote("Privacy mode keeps data on your device.")
        }
    }

    private fun chooseSetupScreen() {
        onboardingScaffold(
            stepIndex = 2,
            title = "How do you want to use Invoke?",
            subtitle = "Choose a setup. You can change this later.",
            body = null,
            primaryText = "Continue",
            primaryAction = { nextAfterSetupChoice() },
            secondaryText = "Back",
            secondaryAction = { go(OnboardingStep.WELCOME) }
        ) {
            setupChoiceCard(
                title = "Private local setup",
                subtitle = "Use your own local model through Ollama.",
                badge = "Most private",
                selected = setupType() == SetupType.LOCAL
            ) { saveSetup(SetupType.LOCAL) }
            setupChoiceCard(
                title = "Cloud sync setup",
                subtitle = "Sign in to sync settings across devices.",
                badge = "Easy sync",
                selected = setupType() == SetupType.CLOUD
            ) { saveSetup(SetupType.CLOUD) }
            setupChoiceCard(
                title = "Try without account",
                subtitle = "Start locally and configure sync later.",
                badge = "Fastest",
                selected = setupType() == SetupType.SKIP
            ) { saveSetup(SetupType.SKIP) }
        }
    }

    private fun permissionsScreen() {
        onboardingScaffold(
            stepIndex = 3,
            title = "A few permissions",
            subtitle = "Invoke needs these to work smoothly across apps.",
            body = null,
            primaryText = "Continue",
            primaryAction = { go(OnboardingStep.BUBBLE) },
            secondaryText = "Back",
            secondaryAction = { go(OnboardingStep.CHOOSE_SETUP) }
        ) {
            permissionCard(
                icon = "Mic",
                title = "Microphone",
                description = "Needed so Invoke can hear your voice.",
                status = if (checkMicPermission()) "Granted" else "Needed",
                ok = checkMicPermission()
            ) { requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQ_MIC) }
            permissionCard(
                icon = "Bubble",
                title = "Floating bubble",
                description = "Shows the mic button on top of other apps.",
                status = "Optional",
                ok = true
            ) { openAppSettings() }
            permissionCard(
                icon = "Access",
                title = "Accessibility",
                description = "Lets Invoke help trigger actions inside supported apps.",
                status = if (isAccessibilityEnabled()) "Granted" else "Needed",
                ok = isAccessibilityEnabled()
            ) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            permissionCard(
                icon = "Battery",
                title = "Battery",
                description = "Keeps the voice bubble available when you need it.",
                status = "Optional",
                ok = true
            ) { openAppSettings() }
        }
    }

    private fun bubbleScreen() {
        onboardingScaffold(
            stepIndex = 4,
            title = "Your voice button",
            subtitle = "Tap the bubble anytime to speak.",
            body = "Invoke can turn speech into text, commands, snippets, or app actions.",
            primaryText = "Looks good",
            primaryAction = { nextAfterBubble() },
            secondaryText = "Back",
            secondaryAction = { go(OnboardingStep.PERMISSIONS) }
        ) {
            voiceBubblePreview()
            segmented("Bubble size", BubbleSize.entries.map { it.label }, bubbleSize().label) { label ->
                prefs.edit().putString(KEY_BUBBLE_SIZE, BubbleSize.entries.first { it.label == label }.name).apply()
                render()
            }
            segmented("Bubble opacity", BubbleOpacity.entries.map { it.label }, bubbleOpacity().label) { label ->
                prefs.edit().putString(KEY_BUBBLE_OPACITY, BubbleOpacity.entries.first { it.label == label }.name).apply()
                render()
            }
        }
    }

    private fun localModelScreen() {
        val endpoint = input("Ollama endpoint", prefs.getString(KEY_OLLAMA_ENDPOINT, "").orEmpty())
        val model = input("Model", prefs.getString(KEY_OLLAMA_MODEL, "qwen3:0.6b").orEmpty())

        onboardingScaffold(
            stepIndex = 5,
            title = "Connect your local model",
            subtitle = "Run Ollama on your computer and connect your phone on the same Wi-Fi.",
            body = "Your phone and computer must be on the same Wi-Fi.",
            primaryText = if (localConnectionStatus == ConnectionStatus.CONNECTED) "Continue" else "Test connection",
            primaryAction = {
                if (localConnectionStatus == ConnectionStatus.CONNECTED) {
                    go(OnboardingStep.PERSONALIZATION)
                } else {
                    testLocalModel(endpoint.text.toString(), model.text.toString())
                }
            },
            secondaryText = "Skip for now",
            secondaryAction = { go(OnboardingStep.PERSONALIZATION) }
        ) {
            textInputCard(endpoint, "Example: your-computer-ip:11434")
            textInputCard(model, "Recommended: qwen3:0.6b")
            statusPill(statusLabel(localConnectionStatus), localConnectionStatus == ConnectionStatus.CONNECTED)
            if (localConnectionMessage.isNotBlank()) root += body(localConnectionMessage)
        }
    }

    private fun accountScreen() {
        val email = input("Email", prefs.getString(KEY_AUTH_EMAIL, "").orEmpty())
        val password = input("Password", password = true)
        onboardingScaffold(
            stepIndex = 5,
            title = "Sync your setup",
            subtitle = "Sign in to keep settings, snippets, dictionary, and style preferences available across devices.",
            body = null,
            primaryText = "Sign in",
            primaryAction = { authenticate(false, email.text.toString(), password.text.toString()) },
            secondaryText = "Continue without sync",
            secondaryAction = { go(OnboardingStep.PERSONALIZATION) }
        ) {
            textInputCard(email, "name@example.com")
            textInputCard(password, "Your password")
            rowButtons(
                "Create account" to { authenticate(true, email.text.toString(), password.text.toString()) },
                "Advanced setup" to { advancedBackendScreen() }
            )
        }
    }

    private fun personalizationScreen() {
        onboardingScaffold(
            stepIndex = 6,
            title = "Make Invoke yours",
            subtitle = "Teach Invoke your words, tone, and shortcuts.",
            body = null,
            primaryText = "Finish setup",
            primaryAction = { finishOnboarding() },
            secondaryText = "Back",
            secondaryAction = { previousBeforePersonalization() }
        ) {
            infoCard("Dictionary", "Teach Invoke words, names, and phrases you use often.", "Names, email addresses, project terms")
            infoCard("Style", "Choose how Invoke formats your text.", "Personal, Work, Email, Other")
            styleSample()
            infoCard("Snippets", "Save reusable prompts and text shortcuts.", "Meeting follow-up, my email address, organize thoughts")
        }
    }

    private fun home() {
        headerBar()
        if (showSettings) {
            settingsScreen()
            return
        }

        when (currentTab) {
            HomeTab.HOME -> homeTab()
            HomeTab.DICTIONARY -> libraryTab("Dictionary", "No dictionary entries yet", "Add names, project terms, and phrases Invoke should understand.")
            HomeTab.STYLE -> styleTab()
            HomeTab.SNIPPETS -> libraryTab("Snippets", "No snippets yet", "Save reusable prompts and text shortcuts for common work.")
        }
        bottomNav()
    }

    private fun homeTab() {
        root += card {
            addView(sectionTitle("Recent voice actions"))
            addView(emptyState("No voice actions yet", "Tap the mic to create your first one."))
        }
        root += micButtonLarge()
    }

    private fun styleTab() {
        root += card {
            addView(sectionTitle("Style"))
            addView(body("Choose how Invoke formats your words."))
            segmented("Default style", listOf("Personal", "Work", "Email", "Other"), prefs.getString(KEY_STYLE, "Work").orEmpty()) {
                prefs.edit().putString(KEY_STYLE, it).apply()
                render()
            }
            addView(sampleOutput())
        }
    }

    private fun libraryTab(title: String, emptyTitle: String, emptyDescription: String) {
        root += card {
            addView(sectionTitle(title))
            addView(emptyState(emptyTitle, emptyDescription))
            addView(primaryButton("Add ${title.lowercase().removeSuffix("s")}") {
                toast("$title editor coming next")
            })
        }
    }

    private fun settingsScreen() {
        root += card {
            addView(sectionTitle("Account"))
            addView(body(prefs.getString(KEY_AUTH_EMAIL, "Local user").orEmpty()))
            addView(statusLine("Plan", "Local beta", true))
            addView(statusLine("Privacy mode", if (privacyMode()) "On" else "Off", privacyMode()))
        }
        root += card {
            addView(sectionTitle("Settings"))
            addView(settingToggle("Privacy mode", "Keep data stored only on your device.", KEY_PRIVACY_MODE))
            addView(settingsRow("Languages", "English"))
            addView(settingsRow("Bubble size", bubbleSize().label))
            addView(settingsRow("Bubble opacity", bubbleOpacity().label))
            addView(settingsRow("App version", "1.0.0"))
            addView(secondaryButton("Advanced backend configuration") { advancedBackendScreen() })
            addView(secondaryButton("Report an issue") { openUrl("https://github.com/somewherelostt/Invoke/issues") })
            addView(secondaryButton("Share feedback") { toast("Feedback link coming soon") })
        }
        root += primaryButton("Back to app") {
            showSettings = false
            render()
        }
    }

    private fun advancedBackendScreen() {
        root.removeAllViews()
        headerBar("Advanced setup")
        val url = input("Supabase project URL", prefs.getString(KEY_SUPABASE_URL, "").orEmpty())
        val anon = input("Supabase anon key", prefs.getString(KEY_SUPABASE_ANON, "").orEmpty(), password = true)
        root += card {
            addView(sectionTitle("Backend configuration"))
            addView(body("Most users do not need this. Add these values only for a custom Supabase project. Keys are stored locally and are not committed."))
            addView(url)
            addView(anon)
            addView(primaryButton("Save backend settings") {
                prefs.edit()
                    .putString(KEY_SUPABASE_URL, url.text.toString().trim())
                    .putString(KEY_SUPABASE_ANON, anon.text.toString().trim())
                    .apply()
                toast("Backend settings saved")
                render()
            })
            addView(secondaryButton("Back") { render() })
        }
    }

    private fun onboardingScaffold(
        stepIndex: Int,
        title: String,
        subtitle: String,
        body: String?,
        primaryText: String,
        primaryAction: () -> Unit,
        secondaryText: String?,
        secondaryAction: (() -> Unit)?,
        content: () -> Unit
    ) {
        progress(stepIndex, 6)
        root += TextView(this).apply {
            text = title
            titleStyle()
            setPadding(0, dp(InvokeSpacing.Xl), 0, dp(8))
        }
        root += TextView(this).apply {
            text = subtitle
            headingStyle()
            setPadding(0, 0, 0, dp(8))
        }
        if (body != null) root += body(body)
        content()
        root += spacer(24)
        root += primaryButton(primaryText, primaryAction)
        if (secondaryText != null && secondaryAction != null) {
            root += secondaryButton(secondaryText, secondaryAction)
        }
    }

    private fun progress(current: Int, total: Int) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(total) { index ->
            bar.addView(View(this).apply {
                background = rounded(
                    if (index < current) InvokeColor.Primary else InvokeColor.Input,
                    dp(999)
                )
                layoutParams = LinearLayout.LayoutParams(0, dp(5), 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
            })
        }
        root += bar
    }

    private fun micHero() {
        val frame = FrameLayout(this).apply {
            background = rounded(InvokeColor.Surface, dp(28), InvokeColor.Border, dp(1))
            setPadding(dp(16), dp(28), dp(16), dp(28))
            matchCard()
        }
        frame.addView(micCircle(92, 1f).apply {
            layoutParams = FrameLayout.LayoutParams(dp(92), dp(92), Gravity.CENTER)
            contentDescription = "Invoke microphone preview"
        })
        root += frame
    }

    private fun voiceBubblePreview() {
        val size = bubbleSize().previewDp
        val opacity = bubbleOpacity().alpha
        val frame = FrameLayout(this).apply {
            background = rounded(InvokeColor.Surface, dp(28), InvokeColor.Border, dp(1))
            minimumHeight = dp(190)
            matchCard()
        }
        frame.addView(micCircle(size, opacity).apply {
            layoutParams = FrameLayout.LayoutParams(dp(size), dp(size), Gravity.CENTER)
            contentDescription = "Floating voice button preview"
        })
        root += frame
    }

    private fun micButtonLarge(): View {
        val wrap = FrameLayout(this).apply {
            minimumHeight = dp(132)
            matchCard()
        }
        wrap.addView(micCircle(86, bubbleOpacity().alpha).apply {
            layoutParams = FrameLayout.LayoutParams(dp(86), dp(86), Gravity.CENTER)
            contentDescription = "Start voice action"
        })
        return wrap
    }

    private fun micCircle(sizeDp: Int, opacity: Float): TextView =
        TextView(this).apply {
            text = "mic"
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            alpha = opacity
            background = rounded(InvokeColor.MicPurple, dp(999))
            minWidth = dp(sizeDp)
            minHeight = dp(sizeDp)
        }

    private fun setupChoiceCard(title: String, subtitle: String, badge: String, selected: Boolean, onClick: () -> Unit) {
        root += card {
            setOnClickListener {
                onClick()
                render()
            }
            addView(statusPillView(badge, true))
            addView(sectionTitle(title))
            addView(body(subtitle))
            if (selected) addView(statusPillView("Selected", true))
        }
    }

    private fun permissionCard(icon: String, title: String, description: String, status: String, ok: Boolean, action: () -> Unit) {
        root += card {
            addView(row {
                addView(TextView(this@MainActivity).apply {
                    text = icon
                    setTextColor(InvokeColor.TextPrimary)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    background = rounded(InvokeColor.PrimarySoft, dp(16))
                    layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { setMargins(0, 0, dp(12), 0) }
                })
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(sectionTitle(title))
                    addView(body(description))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(statusPillView(status, ok))
            })
            if (!ok || status == "Optional") addView(secondaryButton("Allow", action))
        }
    }

    private fun textInputCard(input: EditText, helper: String) {
        root += card {
            addView(input)
            addView(body(helper))
        }
    }

    private fun infoCard(title: String, description: String, examples: String) {
        root += card {
            addView(sectionTitle(title))
            addView(body(description))
            addView(statusPillView(examples, true))
        }
    }

    private fun styleSample() {
        root += card {
            addView(sectionTitle("Sample"))
            addView(body("Raw: send quick update to team"))
            addView(sampleOutput())
        }
    }

    private fun sampleOutput(): TextView =
        TextView(this).apply {
            text = "Team, quick update: I will send the final notes shortly."
            textSize = 15f
            setTextColor(InvokeColor.TextPrimary)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(InvokeColor.Input, dp(18))
        }

    private fun chips(vararg labels: String) {
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        labels.toList().chunked(2).forEach { pair ->
            grid.addView(row {
                pair.forEach { label ->
                    addView(statusPillView(label, true).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            setMargins(dp(4), dp(4), dp(4), dp(4))
                        }
                    })
                }
            })
        }
        root += grid
    }

    private fun trustNote(text: String) {
        root += TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(InvokeColor.Success)
            setPadding(0, dp(12), 0, 0)
        }
    }

    private fun segmented(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
        root += TextView(this).apply {
            text = label
            setTextColor(InvokeColor.TextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(10), 0, dp(8))
        }
        root += row {
            options.forEach { option ->
                addView(MaterialButton(this@MainActivity).apply {
                    text = option
                    isAllCaps = false
                    textSize = 13f
                    minHeight = dp(48)
                    cornerRadius = dp(18)
                    setTextColor(Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (option == selected) InvokeColor.Primary else InvokeColor.Input
                    )
                    setOnClickListener { onSelect(option) }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(3), 0, dp(3), 0)
                    }
                })
            }
        }
    }

    private fun headerBar(title: String = "INVOKE") {
        root += row {
            addView(TextView(this@MainActivity).apply {
                text = title
                headingStyle()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(secondaryButton(if (showSettings) "Close" else "Menu") {
                showSettings = !showSettings
                render()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
        }
    }

    private fun bottomNav() {
        root += row {
            HomeTab.entries.forEach { tab ->
                addView(MaterialButton(this@MainActivity).apply {
                    text = tab.label
                    isAllCaps = false
                    textSize = 12f
                    minHeight = dp(50)
                    cornerRadius = dp(18)
                    setTextColor(Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (currentTab == tab) InvokeColor.Primary else InvokeColor.Surface
                    )
                    setOnClickListener {
                        currentTab = tab
                        render()
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(3), dp(8), dp(3), 0)
                    }
                })
            }
        }
    }

    private fun card(content: LinearLayout.() -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
            background = rounded(InvokeColor.Surface, dp(24), InvokeColor.Border, dp(1))
            matchCard()
            content()
        }

    private fun row(content: LinearLayout.() -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            content()
        }

    private fun View.matchCard() {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, dp(12)) }
    }

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            headingStyle()
            textSize = 18f
            setPadding(0, 0, 0, dp(6))
        }

    private fun body(text: String): TextView =
        TextView(this).apply {
            this.text = text
            bodyStyle()
            setPadding(0, 0, 0, dp(10))
        }

    private fun input(label: String, value: String = "", password: Boolean = false): EditText =
        EditText(this).apply {
            hint = label
            setText(value)
            textSize = 15f
            setTextColor(InvokeColor.TextPrimary)
            setHintTextColor(InvokeColor.TextTertiary)
            inputType = if (label.contains("email", true)) InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS else InputType.TYPE_CLASS_TEXT
            if (password) transformationMethod = PasswordTransformationMethod.getInstance()
            setSingleLine(true)
            minHeight = dp(54)
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(InvokeColor.Input, dp(20), InvokeColor.Border, dp(1))
            contentDescription = label
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(10)) }
        }

    private fun primaryButton(text: String, action: () -> Unit): MaterialButton =
        button(text, InvokeColor.Primary, action)

    private fun secondaryButton(text: String, action: () -> Unit): MaterialButton =
        button(text, InvokeColor.Input, action)

    private fun button(text: String, color: Int, action: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 15f
            minHeight = dp(54)
            cornerRadius = dp(22)
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(4), 0, dp(6)) }
        }

    private fun rowButtons(first: Pair<String, () -> Unit>, second: Pair<String, () -> Unit>) {
        root += row {
            addView(secondaryButton(first.first, first.second).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, dp(4), dp(5), dp(6))
                }
            })
            addView(secondaryButton(second.first, second.second).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(5), dp(4), 0, dp(6))
                }
            })
        }
    }

    private fun statusPill(label: String, ok: Boolean) {
        root += statusPillView(label, ok)
    }

    private fun statusPillView(label: String, ok: Boolean): TextView =
        TextView(this).apply {
            text = label
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (ok) InvokeColor.Success else InvokeColor.Warning)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = rounded(if (ok) 0xFF143D31.toInt() else 0xFF3D2E14.toInt(), dp(999))
        }

    private fun emptyState(title: String, description: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(30), dp(12), dp(30))
            addView(TextView(this@MainActivity).apply {
                text = title
                headingStyle()
                textSize = 20f
                gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = description
                bodyStyle()
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            })
        }

    private fun statusLine(label: String, value: String, ok: Boolean): View =
        row {
            addView(TextView(this@MainActivity).apply {
                text = label
                setTextColor(InvokeColor.TextSecondary)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(statusPillView(value, ok))
        }

    private fun settingsRow(label: String, value: String): View =
        statusLine(label, value, true)

    private fun settingToggle(title: String, description: String, key: String): View =
        row {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(sectionTitle(title))
                addView(body(description))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(secondaryButton(if (prefs.getBoolean(key, false)) "On" else "Off") {
                prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply()
                render()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(dp(88), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
        }

    private fun spacer(height: Int): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(height))
        }

    private fun nextAfterSetupChoice() {
        go(OnboardingStep.PERMISSIONS)
    }

    private fun nextAfterBubble() {
        when (setupType()) {
            SetupType.LOCAL -> go(OnboardingStep.LOCAL_MODEL)
            SetupType.CLOUD -> go(OnboardingStep.ACCOUNT)
            SetupType.SKIP -> go(OnboardingStep.PERSONALIZATION)
        }
    }

    private fun previousBeforePersonalization() {
        when (setupType()) {
            SetupType.LOCAL -> go(OnboardingStep.LOCAL_MODEL)
            SetupType.CLOUD -> go(OnboardingStep.ACCOUNT)
            SetupType.SKIP -> go(OnboardingStep.BUBBLE)
        }
    }

    private fun finishOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        render()
    }

    private fun testLocalModel(endpointInput: String, modelInput: String) {
        localConnectionStatus = ConnectionStatus.TESTING
        localConnectionMessage = "Testing..."
        render()
        lifecycleScope.launch {
            val normalized = modelClient.normalizeEndpoint(endpointInput)
            val result = modelClient.test(endpointInput, modelInput)
            localConnectionStatus = if (result.success) ConnectionStatus.CONNECTED else ConnectionStatus.FAILED
            localConnectionMessage = result.message
            if (result.success && normalized != null) {
                prefs.edit()
                    .putString(KEY_OLLAMA_ENDPOINT, normalized)
                    .putString(KEY_OLLAMA_MODEL, modelInput.trim())
                    .apply()
            }
            render()
        }
    }

    private fun authenticate(signUp: Boolean, email: String, password: String) {
        val cleanEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            toast("Enter a valid email")
            return
        }
        if (password.isBlank()) {
            toast("Enter your password")
            return
        }

        val projectUrl = prefs.getString(KEY_SUPABASE_URL, "").orEmpty()
        val anonKey = prefs.getString(KEY_SUPABASE_ANON, "").orEmpty()
        if (projectUrl.isBlank() || anonKey.isBlank()) {
            toast("Sync backend is not configured. Use Advanced setup or continue without sync.")
            return
        }

        lifecycleScope.launch {
            toast(if (signUp) "Creating account..." else "Signing in...")
            val result = if (signUp) {
                authClient.signUp(projectUrl, anonKey, cleanEmail, password)
            } else {
                authClient.signIn(projectUrl, anonKey, cleanEmail, password)
            }
            if (result.success) {
                prefs.edit()
                    .putString(KEY_AUTH_TOKEN, result.accessToken.ifBlank { "pending" })
                    .putString(KEY_AUTH_EMAIL, result.email.ifBlank { cleanEmail })
                    .apply()
                toast(result.message)
                go(OnboardingStep.PERSONALIZATION)
            } else {
                toast(result.message)
            }
        }
    }

    private fun saveSetup(type: SetupType) {
        prefs.edit().putString(KEY_SETUP_TYPE, type.name).apply()
    }

    private fun setupType(): SetupType =
        runCatching { SetupType.valueOf(prefs.getString(KEY_SETUP_TYPE, SetupType.SKIP.name).orEmpty()) }.getOrDefault(SetupType.SKIP)

    private fun step(): OnboardingStep =
        runCatching { OnboardingStep.valueOf(prefs.getString(KEY_ONBOARDING_STEP, OnboardingStep.WELCOME.name).orEmpty()) }.getOrDefault(OnboardingStep.WELCOME)

    private fun go(step: OnboardingStep) {
        prefs.edit().putString(KEY_ONBOARDING_STEP, step.name).apply()
        render()
    }

    private fun bubbleSize(): BubbleSize =
        runCatching { BubbleSize.valueOf(prefs.getString(KEY_BUBBLE_SIZE, BubbleSize.MEDIUM.name).orEmpty()) }.getOrDefault(BubbleSize.MEDIUM)

    private fun bubbleOpacity(): BubbleOpacity =
        runCatching { BubbleOpacity.valueOf(prefs.getString(KEY_BUBBLE_OPACITY, BubbleOpacity.HIGH.name).orEmpty()) }.getOrDefault(BubbleOpacity.HIGH)

    private fun privacyMode(): Boolean = prefs.getBoolean(KEY_PRIVACY_MODE, setupType() == SetupType.LOCAL)

    private fun statusLabel(status: ConnectionStatus): String =
        when (status) {
            ConnectionStatus.IDLE -> "Not tested"
            ConnectionStatus.TESTING -> "Testing"
            ConnectionStatus.CONNECTED -> "Connected"
            ConnectionStatus.FAILED -> "Failed"
        }

    private fun checkMicPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any { it.resolveInfo.serviceInfo.name.contains("invoke", true) }
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private operator fun LinearLayout.plusAssign(view: View) {
        addView(view)
    }

    private enum class HomeTab(val label: String) {
        HOME("Home"),
        DICTIONARY("Dictionary"),
        STYLE("Style"),
        SNIPPETS("Snippets")
    }

    companion object {
        private const val PREFS = "invoke_prefs"
        private const val REQ_MIC = 100
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ONBOARDING_STEP = "onboarding_step"
        private const val KEY_SETUP_TYPE = "selected_setup_type"
        private const val KEY_BUBBLE_SIZE = "bubble_size"
        private const val KEY_BUBBLE_OPACITY = "bubble_opacity"
        private const val KEY_PRIVACY_MODE = "privacy_mode"
        private const val KEY_OLLAMA_ENDPOINT = "ollama_endpoint"
        private const val KEY_OLLAMA_MODEL = "ollama_model"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON = "supabase_anon_key"
        private const val KEY_AUTH_TOKEN = "supabase_access_token"
        private const val KEY_AUTH_EMAIL = "supabase_user_email"
        private const val KEY_STYLE = "default_style"
    }
}
