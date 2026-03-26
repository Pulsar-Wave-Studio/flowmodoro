package com.flowmodoro.pws.flowmodoro

import com.intellij.openapi.options.Configurable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class FlowmodoroSettingsConfigurable : Configurable {

    private val fractionRadio = JRadioButton("Fraction of work time")
    private val fixedRadio = JRadioButton("Fixed duration")
    private val numeratorSpinner = JSpinner(SpinnerNumberModel(1, 1, 99, 1))
    private val denominatorSpinner = JSpinner(SpinnerNumberModel(5, 1, 99, 1))
    private val fixedMinSpinner = JSpinner(SpinnerNumberModel(5, 0, 120, 1))
    private val fixedSecSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 1))
    private val breaksBeforeLongSpinner = JSpinner(SpinnerNumberModel(4, 1, 50, 1))
    private val longBreakMinSpinner = JSpinner(SpinnerNumberModel(15, 0, 120, 1))
    private val longBreakSecSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 1))
    private val soundCheckbox = JCheckBox("Play sound when break ends")

    override fun getDisplayName() = "Flowmodoro"

    override fun createComponent(): JComponent {
        val radioGroup = ButtonGroup()
        radioGroup.add(fractionRadio)
        radioGroup.add(fixedRadio)

        fractionRadio.addItemListener { updateSpinnerStates() }
        fixedRadio.addItemListener { updateSpinnerStates() }

        val spinnerWidth = Dimension(65, fractionRadio.preferredSize.height)

        val fractionRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            add(fractionRadio)
            add(numeratorSpinner.also { it.preferredSize = spinnerWidth })
            add(JLabel("/"))
            add(denominatorSpinner.also { it.preferredSize = spinnerWidth })
        }

        val fixedRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            add(fixedRadio)
            add(fixedMinSpinner.also { it.preferredSize = spinnerWidth })
            add(JLabel("min"))
            add(fixedSecSpinner.also { it.preferredSize = spinnerWidth })
            add(JLabel("sec"))
        }

        val breakDurationSection = section("Break Duration").apply {
            add(fractionRow)
            add(fixedRow)
        }

        val longBreakSection = section("Long Break").apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(JLabel("Regular breaks before long break:"))
                add(breaksBeforeLongSpinner.also { it.preferredSize = spinnerWidth })
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(JLabel("Long break duration:"))
                add(longBreakMinSpinner.also { it.preferredSize = spinnerWidth })
                add(JLabel("min"))
                add(longBreakSecSpinner.also { it.preferredSize = spinnerWidth })
                add(JLabel("sec"))
            })
        }

        val notificationsSection = section("Notifications").apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(soundCheckbox)
            })
        }

        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(breakDurationSection)
            add(Box.createVerticalStrut(8))
            add(longBreakSection)
            add(Box.createVerticalStrut(8))
            add(notificationsSection)
        }

        val outer = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(content, BorderLayout.NORTH)
        }

        reset()
        return outer
    }

    private fun section(title: String): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createTitledBorder(title)
    }

    private fun updateSpinnerStates() {
        numeratorSpinner.isEnabled = fractionRadio.isSelected
        denominatorSpinner.isEnabled = fractionRadio.isSelected
        fixedMinSpinner.isEnabled = fixedRadio.isSelected
        fixedSecSpinner.isEnabled = fixedRadio.isSelected
    }

    override fun isModified(): Boolean {
        val s = FlowmodoroSettings.getInstance().state
        return (fractionRadio.isSelected && s.breakMode != BreakMode.FRACTION) ||
               (fixedRadio.isSelected && s.breakMode != BreakMode.FIXED) ||
               numeratorSpinner.value as Int != s.breakNumerator ||
               denominatorSpinner.value as Int != s.breakDenominator ||
               fixedMinSpinner.value as Int != s.fixedBreakMinutes ||
               fixedSecSpinner.value as Int != s.fixedBreakSeconds ||
               breaksBeforeLongSpinner.value as Int != s.breaksBeforeLongBreak ||
               longBreakMinSpinner.value as Int != s.longBreakMinutes ||
               longBreakSecSpinner.value as Int != s.longBreakSeconds ||
               soundCheckbox.isSelected != s.soundEnabled
    }

    override fun apply() {
        val s = FlowmodoroSettings.getInstance().state
        s.breakMode = if (fractionRadio.isSelected) BreakMode.FRACTION else BreakMode.FIXED
        s.breakNumerator = numeratorSpinner.value as Int
        s.breakDenominator = denominatorSpinner.value as Int
        s.fixedBreakMinutes = fixedMinSpinner.value as Int
        s.fixedBreakSeconds = fixedSecSpinner.value as Int
        s.breaksBeforeLongBreak = breaksBeforeLongSpinner.value as Int
        s.longBreakMinutes = longBreakMinSpinner.value as Int
        s.longBreakSeconds = longBreakSecSpinner.value as Int
        s.soundEnabled = soundCheckbox.isSelected
    }

    override fun reset() {
        val s = FlowmodoroSettings.getInstance().state
        fractionRadio.isSelected = s.breakMode == BreakMode.FRACTION
        fixedRadio.isSelected = s.breakMode == BreakMode.FIXED
        numeratorSpinner.value = s.breakNumerator
        denominatorSpinner.value = s.breakDenominator
        fixedMinSpinner.value = s.fixedBreakMinutes
        fixedSecSpinner.value = s.fixedBreakSeconds
        breaksBeforeLongSpinner.value = s.breaksBeforeLongBreak
        longBreakMinSpinner.value = s.longBreakMinutes
        longBreakSecSpinner.value = s.longBreakSeconds
        soundCheckbox.isSelected = s.soundEnabled
        updateSpinnerStates()
    }

    override fun disposeUIResources() {}
}
