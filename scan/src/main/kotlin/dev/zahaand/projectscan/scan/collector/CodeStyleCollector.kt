package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.scan.port.StyleSourcePort

class CodeStyleCollector(private val styleSourcePort: StyleSourcePort) {
    fun collect(): SectionResult<CodeStyleInfo> {
        val sources = styleSourcePort.findStyleSources()
        return if (sources.isEmpty()) SectionResult.Empty
        else SectionResult.Ok(CodeStyleInfo(sources))
    }
}
