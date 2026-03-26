package com.flowmodoro.pws.flowmodoro

enum class Phase { IDLE, WORKING, BREAK, LONG_BREAK }

data class SessionState(
    val phase: Phase = Phase.IDLE,
    val elapsedWorkSeconds: Int = 0,
    val remainingBreakSeconds: Int = 0,
    val completedBreaks: Int = 0,
    val taskName: String = ""
)
