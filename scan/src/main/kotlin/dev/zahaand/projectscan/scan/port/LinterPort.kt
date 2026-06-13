package dev.zahaand.projectscan.scan.port

data class LinterToolDescriptor(
    val toolName: String,
    val configFilePath: String?,
    val breaksBuild: Boolean?,
)

interface LinterPort {
    fun getAppliedLinterTools(): List<LinterToolDescriptor>
}
