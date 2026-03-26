package com.flowmodoro.pws.flowmodoro

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import com.intellij.ui.components.JBScrollPane
import javax.swing.JTextArea
import javax.swing.UIManager
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class FlowmodoroPanel : JBPanel<FlowmodoroPanel>(BorderLayout()) {

    private val service get() = FlowmodoroTimerService.getInstance()

    private val workingColor = JBColor(Color(0x2E7D32), Color(0x66BB6A))
    private val breakColor = JBColor(Color(0x1565C0), Color(0x42A5F5))
    private val longBreakColor = JBColor(Color(0x6A1B9A), Color(0xCE93D8))

    private val placeholder = "What are you working on?"
    private var isShowingPlaceholder = false

    private val taskArea = JTextArea(4, 0).apply {
        lineWrap = true
        wrapStyleWord = true
        margin = JBUI.insets(8)
        font = UIManager.getFont("TextField.font")
        val saved = FlowmodoroSettings.getInstance().state.lastTaskName
        if (saved.isEmpty()) {
            isShowingPlaceholder = true
            foreground = JBColor.gray
            text = placeholder
        } else {
            text = saved
        }
    }

    private val taskScrollPane = JBScrollPane(taskArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)

    private val phaseLabel = JBLabel("Ready", SwingConstants.CENTER).apply {
        font = font.deriveFont(Font.BOLD, 14f)
    }

    private val timerLabel = JBLabel("00:00", SwingConstants.CENTER).apply {
        font = Font(Font.MONOSPACED, Font.BOLD, 48)
    }

    private val breakCounterPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.CENTER, 6, 0)).apply {
        isOpaque = false
    }

    private val primaryButton = JButton("Start Work").apply {
        font = font.deriveFont(Font.BOLD, 13f)
        preferredSize = Dimension(150, 32)
    }

    private val resetButton = JButton("Reset").apply {
        font = font.deriveFont(13f)
        isVisible = false
    }

    private val uiListener: () -> Unit = ::syncUI

    init {
        border = JBUI.Borders.empty(12)

        add(taskScrollPane, BorderLayout.NORTH)

        val centerPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.emptyTop(8)
            isOpaque = false

            add(Box.createVerticalGlue())
            add(phaseLabel.also { it.alignmentX = CENTER_ALIGNMENT })
            add(Box.createVerticalStrut(8))
            add(timerLabel.also { it.alignmentX = CENTER_ALIGNMENT })
            add(Box.createVerticalStrut(20))
            add(breakCounterPanel.also { it.alignmentX = CENTER_ALIGNMENT })
            add(Box.createVerticalGlue())
        }
        add(centerPanel, BorderLayout.CENTER)

        val buttonPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.CENTER, 8, 0)).apply {
            border = JBUI.Borders.emptyBottom(4)
            isOpaque = false
            add(primaryButton)
            add(resetButton)
        }
        add(buttonPanel, BorderLayout.SOUTH)

        primaryButton.addActionListener { onPrimaryButtonClick() }
        resetButton.addActionListener { service.reset() }

        taskArea.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                if (isShowingPlaceholder) {
                    isShowingPlaceholder = false
                    taskArea.text = ""
                    taskArea.foreground = JBColor.foreground()
                }
            }
            override fun focusLost(e: FocusEvent) {
                if (taskArea.text.isEmpty()) {
                    isShowingPlaceholder = true
                    taskArea.foreground = JBColor.gray
                    taskArea.text = placeholder
                }
            }
        })

        taskArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onTaskChanged()
            override fun removeUpdate(e: DocumentEvent) = onTaskChanged()
            override fun changedUpdate(e: DocumentEvent) = onTaskChanged()
            private fun onTaskChanged() {
                if (!isShowingPlaceholder) service.updateTaskName(taskArea.text)
            }
        })

        syncUI()
    }

    override fun addNotify() {
        super.addNotify()
        service.addListener(uiListener)
        syncUI()
    }

    override fun removeNotify() {
        service.removeListener(uiListener)
        super.removeNotify()
    }

    private fun onPrimaryButtonClick() {
        when (service.state.phase) {
            Phase.IDLE -> service.startWork()
            Phase.WORKING -> service.stopWorking()
            Phase.BREAK, Phase.LONG_BREAK -> service.skipBreak()
        }
    }

    private fun syncUI() {
        val s = service.state
        val settings = FlowmodoroSettings.getInstance().state

        val phaseColor = when (s.phase) {
            Phase.IDLE -> JBColor.foreground()
            Phase.WORKING -> workingColor
            Phase.BREAK -> breakColor
            Phase.LONG_BREAK -> longBreakColor
        }

        phaseLabel.text = when (s.phase) {
            Phase.IDLE -> "Ready"
            Phase.WORKING -> "Working"
            Phase.BREAK -> "Break"
            Phase.LONG_BREAK -> "Long Break"
        }
        phaseLabel.foreground = phaseColor
        timerLabel.foreground = phaseColor

        timerLabel.text = when (s.phase) {
            Phase.IDLE -> "00:00"
            Phase.WORKING -> formatSeconds(s.elapsedWorkSeconds)
            Phase.BREAK, Phase.LONG_BREAK -> formatSeconds(s.remainingBreakSeconds)
        }

        primaryButton.text = when (s.phase) {
            Phase.IDLE -> "Start Work"
            Phase.WORKING -> "Stop Working"
            Phase.BREAK, Phase.LONG_BREAK -> "Skip Break"
        }

        resetButton.isVisible = s.phase != Phase.IDLE
        taskArea.isEnabled = s.phase == Phase.IDLE

        rebuildBreakDots(s.completedBreaks, settings.breaksBeforeLongBreak)
    }

    private fun rebuildBreakDots(completed: Int, total: Int) {
        breakCounterPanel.removeAll()
        if (total > 0) {
            for (i in 0 until total) {
                val dot = JBLabel(if (i < completed) "●" else "○")
                dot.font = dot.font.deriveFont(20f)
                dot.foreground = if (i < completed) breakColor else JBColor.gray
                breakCounterPanel.add(dot)
            }
        }
        breakCounterPanel.revalidate()
        breakCounterPanel.repaint()
    }

    private fun formatSeconds(totalSecs: Int): String {
        val m = totalSecs / 60
        val s = totalSecs % 60
        return "%02d:%02d".format(m, s)
    }
}
