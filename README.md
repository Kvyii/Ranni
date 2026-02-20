# Ranni

A minimal Android app for tracking bouldering sessions and exercises at the climbing gym.

## Features

- **Climb Logging** — Tap to log boulder routes by colour and grade. Supports gym-specific route systems (9 Degrees) and a generic V-scale option.
- **Exercise Timer** — Create custom exercises with configurable sets, set duration, and rest timers. Start a session and get audio cues when intervals end.
- **History & Progress** — Calendar view of completed sessions and a progress graph with configurable metrics (top climbs, time range).
- **Scoring** — Each route grade has a point value; track your climbing score over time.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- Room for local persistence
- MVVM with manual ViewModel wiring (no DI framework)
- Animated navigation via sealed-class screen state

## Building

Open in Android Studio and run on a device/emulator (minSdk 29, targetSdk 35).
