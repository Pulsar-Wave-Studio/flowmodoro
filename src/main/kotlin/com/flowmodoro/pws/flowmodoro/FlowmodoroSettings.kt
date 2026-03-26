package com.flowmodoro.pws.flowmodoro

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

enum class BreakMode { FRACTION, FIXED }

@Service(Service.Level.APP)
@State(name = "FlowmodoroSettings", storages = [Storage("flowmodoro.xml")])
class FlowmodoroSettings : PersistentStateComponent<FlowmodoroSettings.State> {

    data class State(
        var breakMode: BreakMode = BreakMode.FRACTION,
        var breakNumerator: Int = 1,
        var breakDenominator: Int = 5,
        var fixedBreakMinutes: Int = 5,
        var fixedBreakSeconds: Int = 0,
        var breaksBeforeLongBreak: Int = 4,
        var longBreakMinutes: Int = 15,
        var longBreakSeconds: Int = 0,
        var soundEnabled: Boolean = true,
        var lastTaskName: String = ""
    )

    private var _state = State()

    override fun getState(): State = _state
    override fun loadState(state: State) { _state = state }

    companion object {
        fun getInstance(): FlowmodoroSettings = service()
    }
}
