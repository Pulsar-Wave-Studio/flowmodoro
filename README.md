# Flowmodoro

A JetBrains IDE plugin for the Flowmodoro productivity technique. Work until you lose focus, then take a break proportional to how long you worked.

Compatible with IntelliJ IDEA, Rider, and other JetBrains IDEs (2025.3+).

## How it works

1. Start the timer when you begin a task
2. Stop it when your task is completed or your focus breaks. The length of your break is calculated automatically
3. Take your break, then repeat

Unlike Pomodoro, there are no fixed work intervals. Every couple of break intervals, there is a long break.

## Features

- **Proportional breaks** | break duration is a configurable fraction of work time (default 1/5), or a fixed duration
- **Long breaks** | automatically triggered after a configurable number of regular breaks
- **Break dot tracker** | visual indicator of completed breaks in the current session
- **Task notes** | jot down what you're working on, persisted across IDE restarts
- **Sound notification** | optional audio cue when a break ends
- **IDE notification** | balloon notification when a break ends, even if the tool window is hidden
~~~~
## Settings

Configure via `Settings > Tools > Flowmodoro`:

- Break mode: fraction of work time (e.g. 1/5) or fixed duration
- Number of regular breaks before a long break
- Long break duration
- Sound notifications on/off

## Installation

Install from disk via `Settings > Plugins > gear icon > Install Plugin from Disk`, selecting the `.zip` from the [releases page](../../releases).