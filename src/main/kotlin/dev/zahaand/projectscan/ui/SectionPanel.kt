package dev.zahaand.projectscan.ui

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JPanel

private const val NORTH_BAR_H_GAP = 4
private const val NORTH_BAR_V_GAP = 2

class SectionPanel(section: UiSection) : JBPanel<SectionPanel>(BorderLayout()) {
    private val bodyTextArea =
        JBTextArea(section.body).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }

    private val bodyScrollPane = JBScrollPane(bodyTextArea)

    private val toggleButton = JButton(if (section.collapsedByDefault) "▶" else "▼")

    val copyButton =
        JButton(ProjectScanBundle.message("section.copy.button")).apply {
            isEnabled = section.copyEnabled
        }

    init {
        bodyScrollPane.isVisible = !section.collapsedByDefault

        val northBar =
            JPanel(FlowLayout(FlowLayout.LEFT, NORTH_BAR_H_GAP, NORTH_BAR_V_GAP)).apply {
                add(toggleButton)
                add(JBLabel(section.title))
                add(copyButton)
            }

        add(northBar, BorderLayout.NORTH)
        add(bodyScrollPane, BorderLayout.CENTER)

        toggleButton.addActionListener {
            val nowVisible = !bodyScrollPane.isVisible
            bodyScrollPane.isVisible = nowVisible
            toggleButton.text = if (nowVisible) "▼" else "▶"
            revalidate()
            repaint()
        }

        copyButton.addActionListener {
            CopyPasteManager.getInstance().setContents(StringSelection(bodyTextArea.text))
        }
    }
}
