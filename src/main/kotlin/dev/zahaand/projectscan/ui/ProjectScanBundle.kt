package dev.zahaand.projectscan.ui

import com.intellij.DynamicBundle

private const val BUNDLE = "messages/ProjectScanBundle"

object ProjectScanBundle : DynamicBundle(BUNDLE) {
    fun message(key: String, vararg params: Any): String = getMessage(key, *params)
}
