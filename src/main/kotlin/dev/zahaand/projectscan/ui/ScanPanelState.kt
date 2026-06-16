package dev.zahaand.projectscan.ui

import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.prompt.ConstitutionPrompt

sealed class ScanPanelState {
    object PreScan : ScanPanelState()
    data class PostScan(val data: PostScanData) : ScanPanelState()
}

data class PostScanData(
    val scanResult: ScanResult,
    val constitutionPrompt: ConstitutionPrompt,
)
