package com.sahiix.ocrplaybook.ui.navigation

/** Bottom-tab + detail destinations. */
sealed class Route(val path: String) {
    data object Scanner : Route("scanner")
    data object History : Route("history")
    data object Report : Route("report/{scanId}") {
        fun forId(id: Long) = "report/$id"
    }
    data object Settings : Route("settings")
}
