<p align="center">
  <img src="ranni_transp.png" alt="Ranni" width="240">
</p>

<h1 align="center">Ranni</h1>

<p align="center">
  A minimal Android app for tracking bouldering sessions and exercises at the climbing gym.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat" alt="Platform">
  <img src="https://img.shields.io/badge/Min_SDK-29-blue?style=flat" alt="Min SDK">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack_Compose-purple?style=flat" alt="Kotlin">
</p>

---

## Features

**Climb Logging** — Tap to log boulder routes by colour and grade. Supports gym-specific route systems (9 Degrees), outdoor gyms with YDS and V grading, and a generic V-scale option.

**Exercise Timer** — Create custom exercises with configurable sets, set duration, and rest timers. Audio cues signal when intervals end.

**History & Progress** — Calendar view of past sessions and a progress graph with configurable metrics (top climbs, time range).

**Scoring** — Each route grade has a point value; track your climbing score over time.

**Injury Tracking** — Log and monitor injuries alongside your sessions.

## Tech Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Database | Room |
| Architecture | MVVM, sealed-class navigation |

## Building

Open in Android Studio and run on a device or emulator.

```
minSdk 29 · targetSdk 35
```
