package com.invoke.android.ui.onboarding

enum class SetupType {
    LOCAL,
    CLOUD,
    SKIP
}

enum class OnboardingStep {
    WELCOME,
    CHOOSE_SETUP,
    PERMISSIONS,
    BUBBLE,
    LOCAL_MODEL,
    ACCOUNT,
    PERSONALIZATION,
    HOME
}

enum class BubbleSize(val label: String, val previewDp: Int) {
    SMALL("Small", 58),
    MEDIUM("Medium", 72),
    LARGE("Large", 88)
}

enum class BubbleOpacity(val label: String, val alpha: Float) {
    LOW("Low", 0.55f),
    MEDIUM("Medium", 0.78f),
    HIGH("High", 1f)
}

enum class ConnectionStatus {
    IDLE,
    TESTING,
    CONNECTED,
    FAILED
}
