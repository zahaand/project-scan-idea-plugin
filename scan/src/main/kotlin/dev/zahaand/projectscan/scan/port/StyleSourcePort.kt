package dev.zahaand.projectscan.scan.port

import dev.zahaand.projectscan.model.StyleSource

interface StyleSourcePort {
    fun findStyleSources(): List<StyleSource>
}
