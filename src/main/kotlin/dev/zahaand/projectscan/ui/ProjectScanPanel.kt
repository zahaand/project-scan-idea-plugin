package dev.zahaand.projectscan.ui

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import dev.zahaand.projectscan.baseline.BaselineRule
import dev.zahaand.projectscan.prompt.ConstitutionPrompt
import dev.zahaand.projectscan.prompt.PromptGenerator
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.scan.ScanService
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane

class ProjectScanPanel(
    private val project: Project,
    private val scanService: ScanService,
    private val promptGenerator: PromptGenerator,
    private val baselineRules: List<BaselineRule>,
) : JBPanel<ProjectScanPanel>(BorderLayout()) {
    private val scanButton = JButton(ProjectScanBundle.message("toolwindow.ProjectScan.scan.button"))
    private val hintLabel = JBLabel(ProjectScanBundle.message("toolwindow.ProjectScan.hint"))

    private val sectionContainer = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    @Volatile private var scanResult: ScanResult? = null
    @Volatile private var constitutionPrompt: ConstitutionPrompt? = null

    init {
        val topBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(scanButton)
            add(hintLabel)
        }
        add(topBar, BorderLayout.NORTH)
        add(JScrollPane(sectionContainer), BorderLayout.CENTER)

        scanButton.addActionListener {
            setScanButtonEnabled(false)
            object : Task.Backgroundable(
                project,
                ProjectScanBundle.message("toolwindow.ProjectScan.title"),
                false,
            ) {
                override fun run(indicator: ProgressIndicator) {
                    val result = scanService.scan()
                    val prompt = promptGenerator.generate(result, baselineRules)
                    scanResult = result
                    constitutionPrompt = prompt
                }

                override fun onSuccess() {
                    showResults(scanResult!!, constitutionPrompt!!)
                }

                override fun onThrowable(error: Throwable) {
                    revertToPreScan()
                    Notifications.Bus.notify(
                        Notification(
                            "ProjectScan",
                            ProjectScanBundle.message("scan.error.notification.title"),
                            error.message ?: "Unknown error",
                            NotificationType.ERROR,
                        ),
                        project,
                    )
                }

                override fun onFinished() {
                    setScanButtonEnabled(true)
                }
            }.queue()
        }
    }

    fun showResults(result: ScanResult, prompt: ConstitutionPrompt) {
        val sections = ScanResultRenderer.render(result, prompt)
        sectionContainer.removeAll()
        sections.forEach { sectionContainer.add(SectionPanel(it)) }
        sectionContainer.revalidate()
        sectionContainer.repaint()
    }

    fun revertToPreScan() {
        sectionContainer.removeAll()
        sectionContainer.revalidate()
        sectionContainer.repaint()
    }

    fun setScanButtonEnabled(enabled: Boolean) {
        scanButton.isEnabled = enabled
    }
}
