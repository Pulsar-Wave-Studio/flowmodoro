package com.flowmodoro.pws.flowmodoro

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.awt.Toolkit
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@Service(Service.Level.APP)
class FlowmodoroTimerService : Disposable {

    var state = SessionState()
        private set

    private val tickTimer = javax.swing.Timer(1000) { onTick() }
    private val listeners = mutableListOf<() -> Unit>()

    init {
        state = state.copy(taskName = FlowmodoroSettings.getInstance().state.lastTaskName)
    }

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }

    private fun notifyListeners() {
        listeners.toList().forEach { it() }
    }

    fun startWork() {
        if (state.phase != Phase.IDLE) return
        state = state.copy(phase = Phase.WORKING, elapsedWorkSeconds = 0)
        tickTimer.start()
        notifyListeners()
    }

    fun stopWorking() {
        if (state.phase != Phase.WORKING) return
        tickTimer.stop()
        val settings = FlowmodoroSettings.getInstance().state
        val workSecs = state.elapsedWorkSeconds
        val isLong = state.completedBreaks > 0 && state.completedBreaks % settings.breaksBeforeLongBreak == 0
        val breakSecs = if (isLong) {
            settings.longBreakMinutes * 60 + settings.longBreakSeconds
        } else if (settings.breakMode == BreakMode.FRACTION) {
            (workSecs * settings.breakNumerator / settings.breakDenominator).coerceAtLeast(1)
        } else {
            (settings.fixedBreakMinutes * 60 + settings.fixedBreakSeconds).coerceAtLeast(1)
        }
        val nextPhase = if (isLong) Phase.LONG_BREAK else Phase.BREAK
        state = state.copy(phase = nextPhase, remainingBreakSeconds = breakSecs)
        tickTimer.start()
        notifyListeners()
    }

    fun skipBreak() {
        if (state.phase != Phase.BREAK && state.phase != Phase.LONG_BREAK) return
        tickTimer.stop()
        val newCompleted = if (state.phase == Phase.LONG_BREAK) 0 else state.completedBreaks + 1
        state = SessionState(completedBreaks = newCompleted, taskName = state.taskName)
        notifyListeners()
    }

    fun reset() {
        tickTimer.stop()
        state = SessionState(taskName = state.taskName)
        notifyListeners()
    }

    fun updateTaskName(name: String) {
        state = state.copy(taskName = name)
        FlowmodoroSettings.getInstance().state.lastTaskName = name
    }

    private fun onTick() {
        when (state.phase) {
            Phase.WORKING -> {
                state = state.copy(elapsedWorkSeconds = state.elapsedWorkSeconds + 1)
                notifyListeners()
            }
            Phase.BREAK, Phase.LONG_BREAK -> {
                val remaining = state.remainingBreakSeconds - 1
                if (remaining <= 0) {
                    onBreakComplete()
                } else {
                    state = state.copy(remainingBreakSeconds = remaining)
                    notifyListeners()
                }
            }
            Phase.IDLE -> tickTimer.stop()
        }
    }

    private fun onBreakComplete() {
        val isLong = state.phase == Phase.LONG_BREAK
        val newCompleted = if (isLong) 0 else state.completedBreaks + 1
        state = SessionState(completedBreaks = newCompleted, taskName = state.taskName)
        tickTimer.stop()
        notifyListeners()
        showIdeNotification(isLong)
        playNotificationSound()
    }

    private fun showIdeNotification(isLong: Boolean) {
        val title = if (isLong) "Long Break Over!" else "Break Over!"
        val content = if (isLong) "Session reset. Ready to start fresh?" else "Ready to get back to work?"
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Flowmodoro")
            .createNotification(title, content, NotificationType.INFORMATION)
        Notifications.Bus.notify(notification)
    }

    private fun playNotificationSound() {
        if (!FlowmodoroSettings.getInstance().state.soundEnabled) return
        Thread {
            try {
                playTone(392.0, 350)
                Thread.sleep(60)
                playTone(523.0, 350)
                Thread.sleep(60)
                playTone(523.0, 350)
                Thread.sleep(60)
                playTone(392.0, 350)
            } catch (_: Exception) {
                Toolkit.getDefaultToolkit().beep()
            }
        }.start()
    }

    private fun playTone(frequency: Double, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * frequency * i / sampleRate
            val t = i.toDouble() / numSamples
            val envelope = (if (t < 0.12) t / 0.12 else exp(-3.0 * (t - 0.12))) * 0.30
            val sample = (sin(angle) * 32767 * envelope).toInt().toShort()
            buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val clip = AudioSystem.getClip()
        clip.open(AudioInputStream(ByteArrayInputStream(buffer), format, numSamples.toLong()))
        clip.use { clip ->
            clip.start()
            Thread.sleep(durationMs.toLong() + 100)
        }
    }

    override fun dispose() {
        tickTimer.stop()
    }

    companion object {
        fun getInstance(): FlowmodoroTimerService = service()
    }
}
