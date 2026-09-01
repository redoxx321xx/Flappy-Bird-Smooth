# 🕊️ Flappy Bird — Ultra Smooth Edition

<div align="center">

**Created by CHAOS**  
*✨ Just Enjoy ✨*

[![Android Build](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Room-F44336)](#)

*An ultra-responsive, arcade classic built natively for modern Android with zero input lag, physics-driven squash-and-stretch animations, dynamic atmospheric parallax, and real-time ASMR audio synthesis.*

</div>

---

## 📖 About The Game

**Flappy Bird: Ultra Smooth Edition** is a modern reimagining of the iconic retro arcade game. Built from the ground up using **Kotlin** and **Jetpack Compose**, this version focuses on delivering an exceptionally smooth, responsive, and satisfying gameplay experience.

From customized flight physics and smart elevation-aware obstacle generation to relaxing procedural audio synthesizers, every micro-interaction has been carefully engineered for maximum comfort, replayability, and visual charm.

---

## 🌟 Key Features & Highlights

### 🎮 Gameplay & Physics
* **Ultra-Responsive Controls**: Direct low-latency touch response with smooth delta-time physics.
* **Squash & Stretch Dynamics**: Realistic cartoon deformations—the bird compresses on flap impulse and elongates smoothly during steep climbs and dives.
* **Smart Pipe Spacing Algorithm**: Obstacle gaps are dynamically calibrated to prevent unfair vertical jumps, adjusting clearance distance based on preceding pipe heights.
* **Bonus Coin System**: Collect glowing airborne coins nestled between pipes to boost your total score.

### 🎨 Visuals & Immersion
* **Dynamic Multi-Layered Parallax**:
  * Sweeping distant mountain ridges and layered urban skylines.
  * Theme-responsive celestial lighting (radiant sun with atmospheric halos, glowing moon with twinkling stars).
  * Drifting cloud cover with depth-based scrolling speeds.
* **Ambient Nature Particles**: Soft floating cherry blossom petals (Day), golden autumn leaves (Sunset), and luminescent stardust (Night).
* **Atmospheric Themes**:
  * ☀️ **Day**: Fresh cerulean sky with lush green terrain.
  * 🌅 **Sunset**: Warm golden amber hues with vibrant violet horizon gradients.
  * 🌙 **Night**: Deep midnight indigo canvas with glowing neon accents.

### 🎵 Real-Time Procedural ASMR Audio
* **Built-in Pure Tone Synthesizer**: No heavy external audio files—soundwaves are generated programmatically using Android's native `AudioTrack`.
* **Soothing Sound Palette**:
  * Acoustic harmonic pop on flap impulses.
  * Double-crystal chime when passing pipes cleanly.
  * Shimmering bell triad on coin collection.
  * Soft muffled impact on game over.

### 🕊️ Custom Skins & Customization
* **Classic Yellow**: The beloved arcade original.
* **Phoenix Ember**: Blazing crimson with fiery trail particles.
* **Cyber Neon**: Luminescent electric cyan with futuristic highlights.
* **Midnight Raven**: Sleek obsidian feathers with shadow trails.
* **Pink Sakura**: Gentle pastel blossom bird.

### 🏆 Progress & Performance
* **Local Persistence**: Integrated **Room SQLite** database tracking high scores and achievement milestones locally without requiring internet access.
* **Low-RAM & Battery Optimized**: Zero-allocation hardware Canvas drawing pipeline with strictly bounded particle pooling, ensuring high performance across budget and flagship devices alike.

---

## 🛠️ Technical Stack & Architecture

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0+ | Modern expressive syntax and coroutines |
| **UI Framework** | Jetpack Compose | Declarative UI for menus and overlays |
| **Graphics Engine** | Android Hardware Canvas | Zero-allocation real-time 2D game rendering |
| **Architecture** | MVVM | Clean separation of game loop, state, and rendering |
| **Persistence** | Room Database (SQLite) | Fast, offline-first local high score caching |
| **Audio** | Android `AudioTrack` | Real-time PCM sine-wave sound synthesis |
| **CI/CD** | GitHub Actions | Automated APK compilation on push |

---

## 🚀 Getting Started & Installation

### Option 1: Download Pre-Built APK
1. Head over to the **[Releases](../../releases)** tab.
2. Download the latest `FlappyBird-APK.zip` or `.apk` file.
3. Open the APK on your Android device (Android 8.0+ / API 26+) and follow the prompts to install.

### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/redoxx321xx/Flappy-Bird-Smooth.git

# Open the project folder
cd Flappy-Bird-Smooth

# Compile the debug APK
gradle assembleDebug
```
The generated APK will be available in `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👨‍💻 Credits & License

* **Developer**: CHAOS (`redoxx321xx`)
* **License**: Open Source under the [MIT License](LICENSE)

<div align="center">
  <sub>Built with ❤️ for mobile gaming fans everywhere.</sub>
</div>
