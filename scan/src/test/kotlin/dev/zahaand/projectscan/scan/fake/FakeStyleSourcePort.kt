package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.model.StyleSource
import dev.zahaand.projectscan.scan.port.StyleSourcePort

class FakeStyleSourcePort(
    private val sources: List<StyleSource> = emptyList(),
) : StyleSourcePort {
    override fun findStyleSources(): List<StyleSource> = sources
}
