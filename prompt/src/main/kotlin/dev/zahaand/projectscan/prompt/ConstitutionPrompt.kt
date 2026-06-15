package dev.zahaand.projectscan.prompt

class ConstitutionPrompt(val blocks: List<PromptBlock>) {
    fun render(): String {
        val preamble = "Forward this prompt to `/speckit-constitution` to generate a new project Constitution from the scan data below."
        val blockContent = blocks.joinToString(separator = "\n\n") { "## ${it.heading}\n\n${it.content}" }
        return "$preamble\n\n$blockContent"
    }
}
