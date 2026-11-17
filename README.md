# Sweet Rush: Candy World (Android 3D Kart Racing)

A vibrant, landscape-only 3D Android kart racing game inspired by colorful candy fantasy worlds, built natively with **Kotlin**, **OpenGL ES 3.0**, **Jetpack Compose**, and **Material 3**.

## Features
- **4 Handcrafted 3D Candy Circuits**: Peppermint Plaza, Cupcake Coast, Chocolate Volcano, and Gumdrop Garden — each with elevated bridges, candy cane arches, wafer tunnels, boost pads, jump ramps, hazards, item boxes, and dense 3D candy scenery.
- **8 Original Karts & 8 Original Racers**: Distinct multi-part 3D kart bodies, rotating/steerable wheels, seated racers, and unlockable paint jobs, wheels, exhaust effects, decorations, and 5-stat upgrades.
- **Full Arcade Racing Physics**: Smooth acceleration, braking, drifting with 3-tier boost charging, Nitro boost, jumping & air control, boundary barriers, kart-to-kart collisions, checkpoints, lap counting, and 7 candy power-ups.
- **7 Competitive AI Racers**: Easy, Normal, Hard, and Expert difficulties with lane overtaking, hazard avoidance, drift cornering, power-up usage, and stuck recovery.
- **5 Game Modes**: Quick Race, 4-Race Grand Prix Cup, Time Trial, Free Practice, and Special Challenges.
- **Interactive 3D Garage & Candy Audio**: Touch-drag 360° 3D showroom, synthesized candy-arcade music & SFX, persistent local JSON save with backup recovery, and customizable landscape controls (Touch Buttons, Virtual Steering Wheel, or Tilt Steering).

## Building Debug APK
```bash
gradle :app:assembleDebug
```
CI workflow in `.github/workflows/android-debug.yml` builds and uploads `sweet-rush-debug.apk`.
