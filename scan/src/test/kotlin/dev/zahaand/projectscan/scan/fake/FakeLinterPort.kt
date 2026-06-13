package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.scan.port.LinterPort
import dev.zahaand.projectscan.scan.port.LinterToolDescriptor

class FakeLinterPort(
    private val tools: List<LinterToolDescriptor> = emptyList(),
) : LinterPort {
    override fun getAppliedLinterTools(): List<LinterToolDescriptor> = tools
}
