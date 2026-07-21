# Pomodoro Light

A minimalist Pomodoro timer tool for the Light Phone III, built with the LightOS SDK.

## What is this?

Pomodoro Light is a focus timer built specifically for the [Light Phone III](https://www.thelightphone.com/), a minimalist phone designed to help people spend less time on their screens. The app follows the classic Pomodoro Technique: alternating focused work sessions with short breaks, helping you get things done without the distractions of a typical smartphone.

It's built using Light's official [LightOS SDK](https://github.com/lightphone/light-sdk), which means it follows the same design language, restrictions, and philosophy as Light's own built-in tools — no infinite scrolling, no unnecessary notifications, just a simple tool that does one thing well.


<img width="314" height="362" alt="pomodoroLightDemo" src="https://github.com/user-attachments/assets/eea2d0ea-3aae-4730-9469-f16363e8af29" />

## Features

- Focus/Break cycle timer (15-60 min focus, 5-30 min break, in 5-minute steps)
- Circular progress ring visualisation
- History screen with a 7-day bar chart and a scrollable 30-day list of completed pomodoros and focus minutes
- Minimalist design following LightOS UI components

## Built with

- Kotlin + Jetpack Compose
- LightOS SDK (`com.thelightphone.sdk`)

## Status

This project is under active development as part of Light's early developer programme. Some planned features (like local sound alerts) are currently blocked by SDK limitations that Light has confirmed are on their roadmap. See [open issues](https://github.com/lightphone/light-sdk/issues) for details.

## Getting started

This project is a fork of [lightphone/light-sdk](https://github.com/lightphone/light-sdk), the official scaffold for building tools for the Light Phone III. To build and run it, follow the setup instructions in the original repository's `README.md` and `docs/` folder — you'll need Android Studio, a GitHub token with package read access, and the LightOS Emulator set up as described there.
