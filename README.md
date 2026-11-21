# Persona 5 Royal for Android (native port)

> [!WARNING]
> **Maintenance of this port has been stopped.** This repository is no longer actively maintained.

An Android app that runs **your own copy** of Persona 5 Royal (Nintendo Switch, version 1.0.0) natively on an arm64 phone or tablet. The Switch's operating system services, its audio, video and input, and NVN (the Switch's graphics API) are reimplemented on Android, with Vulkan for the graphics. The game's own code runs directly on the phone's CPU: the Switch and Android phones share the same CPU architecture (AArch64).

**No PC needed:** install the APK, pick your game file (`.nsp` or `.xci`) and your `prod.keys` on the phone, let it prepare the game once (a few minutes), and play. The game is read from your file as it is; nothing is extracted.

> [!IMPORTANT]
> **No game files are included, here or in the APK.** This repository and its releases hold no game code, assets, shaders, keys or firmware. The APK is only the engine: it reads the game from your own dump. You need your own legally dumped copy of the game (the base game, version 1.0.0) and your own `prod.keys`. How to dump them from your console is **not** covered here. Please do not ask for game files or keys.

Persona 5 Royal is © ATLUS / © SEGA. This is an unofficial fan project, not affiliated with or endorsed by ATLUS, SEGA or Nintendo.

## Community

- **Discord** (English and Spanish channels), for news, help and testing: https://discord.gg/rMgRneBTCe

## Install

1. Copy your **Persona 5 Royal 1.0.0** `.nsp` (or `.xci`) and your `prod.keys` to the phone (the Download folder is fine). The base game only: not the update.
2. Install **P5R** from the [Releases](../../releases) page (allow installing apps from your browser or file manager when Android asks).
3. Open it and allow "All files access" (for its folder `Internal storage/Persona 5 Royal`: settings, saves, shaders).
4. **Game file → Choose**: pick your `.nsp`/`.xci`. **Keys → Choose**: pick your `prod.keys` (and `title.keys` too, if your dump needs it; you can select both). The app keeps the keys in its private storage; they are not uploaded or included in the APK.
5. Press **Prepare the game**. Keep the app open: it checks the game and translates its ~30,000 shaders for your GPU (about two minutes on a mid-range tablet; once).
6. Press **Play**.

Keep the game file where it is: every start reads the game from it. Saves are kept in `Internal storage/Persona 5 Royal/user/save`; the home screen can import and export them. Mods (LayeredFS style) can be placed in `Internal storage/Persona 5 Royal/mods/<ModName>/romfs/...` (see [docs/MODS.md](docs/MODS.md)).

## Options

The app includes the following configuration options:

- **Game settings:** text language, resolution (360p, 480p, 720p, or 1080p), interface resolution, graphics (texture detail), frame rate cap (30, or 60 experimental), shadows, FPS counter, **Fullscreen (stretch)**, **Crowd NPCs** (toggle off to boost FPS in busy areas like Shibuya), and **Post-processing / Bloom** (toggle off for lighter GPU load).
- **Touch controls:** show or hide controls, adjust size and opacity, configure L3/R3 buttons, customize the touch layout using the built-in editor, and adjust touch stick and camera sensitivity (50% to 250%).
- **Physical gamepad:** Nintendo vs Xbox/Android A/B and X/Y button layout, plus configurable button mappings.
- **Launcher customization:** a redesigned landscape interface inspired by Persona 5 Royal's visual identity, with dedicated sections for Play, Display, Performance, Controls, Files and Credits. The launcher includes animated backgrounds, custom background selection, background dimming, halftone effects and gyro-based parallax on the default background.
- **Launcher feedback:** configurable launcher sounds and haptic feedback.
- **Gamepad navigation:** navigate the launcher interface using a connected physical controller.
- **Pause menu:** press the Android Back button while playing to open the in-game pause menu, which provides access to display, performance and control settings.
- **Additional tools:** **Import CPK** (for fan translations / language packs), and **Export logs** (generate and share debug logs directly from the device).

## Requirements

- An arm64 Android phone or tablet with **Android 10 or newer** and **Vulkan 1.1**, 6 GB of RAM or more (8 GB recommended).
- Storage: the game file (about 15 GB) plus about 1 GB for the translated shaders and saves.
- Your own dump of **Persona 5 Royal 1.0.0** (US `01005CA01580E000`, EU `01004B10157F2000`, JP `0100B880154FC000`, or Asia `01006BB00C6F0000`) and your console's `prod.keys`.

## Status

The no-PC flow (selecting the game and keys, preparing shaders, and launching from the NSP) has been verified on **one device** so far:

| Device | SoC / GPU | Results |
|---|---|---|
| Lenovo Tab M11 (Android 14, 8 GB) | Snapdragon 685 / Adreno 610, Vulkan 1.1 | menus 60 fps, 3D title ~14 fps, the casino ~20 fps |

The Adreno 610 is a low-end GPU: on faster phones expect more. Other GPUs (Mali, PowerVR, Xclipse, newer Adreno) are untested. Reports are welcome (see [Reporting problems](#reporting-problems)).

What works: the menus, films (hardware VP9), sound, cutscenes in sync, battles, saving and loading, 30 or 60 fps cap, touch controls (movable) and game controllers, the text language of your choice.

Known limits: version 1.0.0 only (not the update); performance on low-end GPUs; the shadows option costs a lot on weak GPUs (off by default).

The **Chakeyy Launcher Rework** branch also introduces an extensive redesign of the Android interface and additional configuration features. These changes include the redesigned launcher, an in-game pause menu, expanded gamepad configuration and an interactive touch-control layout editor. Compatibility and performance may vary depending on the device and GPU.

## How it works

- **Native execution (the loader).** The APK reads `main` and `sdk` from your game file's ExeFS, maps and links them as the Switch's loader would, and runs them on the phone's CPU (`runtime/src/nce/`). The few instructions that cannot run on Android as they are get patched into small stubs: system calls (`svc`), the thread-pointer registers, and the system counter. Calls into Switch system libraries go to the runtime's implementations.
- **The game file, read in place.** The NSP/XCI is opened with your keys (`android/wonder/importer/nxgame.h`) and the game's files are decrypted on the fly as the game reads them (`runtime/src/game_image.cpp`).
- **Shaders** are translated once, on the phone, from the game's Maxwell shaders to SPIR-V with [Ryujinx](https://github.com/Ryubing/Ryujinx)'s shader translator (MIT), compiled to a native Android library with .NET NativeAOT (`libspirvharness.so`). The extraction is `games/p5r/import/`.
- **The runtime** is the Switch runtime of [Super Mario Bros. Wonder Recomp](https://github.com/BelmanteGu/SuperMarioWonderRecomp) (SwitchGamesRecomp, MIT, by BelmanteGu / Projeto Belmante). It provides the kernel, the services, NVN on Vulkan, audio (AAudio), hardware video decoding (MediaCodec) and touch controls. This port adds Persona 5 Royal's needs and the native loader.
- **Android interface and controls.** The launcher and in-game interface have been extended with Android-specific components. The launcher rework introduces a dedicated settings interface, animated visual elements, configurable feedback and controller navigation. The pause menu and control editors provide additional ways to configure the game without leaving the application.

The project started as a static recompilation (the game's code translated to C++), the upstream project's approach. That build is still there for debugging on Windows (`games/p5r/tools/build_pc.cmd`), but it needs your own recompiled game code, so it is private by nature. The Android APK is the native build (`NX_NCE`), which holds no game code and can be shared. `games/p5r/tools/prepare_data.ps1` still prepares a data folder on a PC, for that build or for testing.

## Building the APK

On Windows, with the Android SDK (`ANDROID_HOME`, or Android Studio's default location; platform 35, build-tools 35.0.0, NDK 28.2.13676358), a JDK 17+ (`JAVA_HOME`, or Android Studio's own), the Vulkan headers 1.4.357, and Docker Desktop (once, for the shader translator library):

```powershell
python -m venv private\venv; private\venv\Scripts\pip install cmake ninja
git clone --depth 1 --branch v1.4.357 https://github.com/KhronosGroup/Vulkan-Headers private\third_party\Vulkan-Headers-1.4.357
# the translator: Ryubing's source, then the library (NativeAOT for Android needs Linux: a Docker container)
mkdir private\third_party\ryubing
curl.exe -L -o private\third_party\ryubing.tar.gz https://codeberg.org/Software_arch/Ryubing/archive/a82350bb77.tar.gz
tar -xzf private\third_party\ryubing.tar.gz -C private\third_party\ryubing --strip-components=1
docker run --rm -v "${PWD}:/src" -w /src mcr.microsoft.com/dotnet/sdk:10.0 sh games/p5r/tools/build_harness_android.sh
games\p5r\tools\build_android.cmd -Nce            # -> private\p5r\apk_nce\p5r.apk; -Install installs it with adb
```

The APK is signed with your Android debug key (`%USERPROFILE%\.android\debug.keystore`; Android Studio makes it, or `keytool -genkeypair -keystore %USERPROFILE%\.android\debug.keystore -storepass android -keypass android -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -validity 10000`). Without `-Nce` the script builds the recompiled flavour, which needs your own recompiled game code in `private\p5r\gen` (see `docs/RECOMP.md`) and must never be shared.

## Reporting problems

Open an issue with your phone's model, SoC/GPU, Android version and RAM, what you did and what happened, and the log:

```bash
adb logcat -s p5r:V > p5r-log.txt
```

(run it before starting the game). Never attach game files, keys, or screenshots of copyrighted content beyond what the report needs.

## En español

> [!WARNING]
> **Se ha detenido el mantenimiento de este port.** Este repositorio ya no recibe mantenimiento activo.

Una app de Android que ejecuta **tu propia copia** de Persona 5 Royal (Switch, versión 1.0.0) de forma nativa en un móvil o tablet arm64. El sistema reimplementa en Android los servicios de Switch, incluyendo audio, vídeo, entrada y gráficos mediante Vulkan.

**No hace falta PC:** instalá el APK, seleccioná tu archivo del juego (`.nsp` o `.xci`) y tus `prod.keys`, prepará el juego una vez y comenzá a jugar. El juego se lee directamente desde tu archivo. No se extrae ni se incluyen archivos del juego en el APK.

- **Discord** (canales en español e inglés), para novedades, ayuda y pruebas: https://discord.gg/rMgRneBTCe

### Instalación

1. Copiá al móvil tu `.nsp` (o `.xci`) de Persona 5 Royal 1.0.0 (el juego base, sin la actualización) y tu `prod.keys` (por ejemplo, a Descargas).
2. Instalá **P5R** desde [Releases](../../releases).
3. Abrilo y permití el acceso a todos los archivos.
4. **Archivo del juego → Elegir**: seleccioná tu `.nsp` o `.xci`. **Claves → Elegir**: seleccioná tu `prod.keys` (y `title.keys` si tu copia lo necesita). La app guarda las claves en su almacenamiento privado; no se suben ni se incluyen en el APK.
5. Pulsá **Preparar el juego** y dejá la app abierta: comprobará el juego y traducirá sus shaders para tu GPU. Este proceso se realiza una sola vez.
6. Pulsá **Jugar**.

No borres ni movás el archivo del juego: cada vez que iniciés el juego se leerá directamente desde ahí. Las partidas se guardan en `Almacenamiento interno/Persona 5 Royal/user/save`; desde la pantalla principal podés importarlas y exportarlas.

Los mods compatibles con LayeredFS pueden colocarse en `Almacenamiento interno/Persona 5 Royal/mods/<NombreDelMod>/romfs/...` (consultá [docs/MODS.md](docs/MODS.md)).

### Opciones y novedades

- **Configuración del juego:** idioma de texto, resolución, calidad de texturas, límite de FPS, sombras, contador de FPS, pantalla completa, NPCs de multitudes y postprocesamiento.
- **Controles táctiles:** mostrar u ocultar controles, ajustar tamaño y opacidad, configurar botones L3/R3 y modificar la distribución mediante el editor de controles táctiles.
- **Mandos físicos:** selección de distribución Nintendo o Xbox/Android, reasignación de botones por acción y navegación del launcher mediante gamepad.
- **Launcher rediseñado:** interfaz horizontal inspirada en la identidad visual de Persona 5 Royal, con secciones para Play, Display, Performance, Controls, Files y Credits.
- **Personalización visual:** fondos animados, selección de fondo personalizado, oscurecimiento, efectos de semitono y parallax mediante giroscopio en el fondo predeterminado.
- **Respuesta del launcher:** sonidos y vibración háptica configurables.
- **Menú de pausa:** al pulsar el botón Back de Android durante una partida, se abre un menú desde el que podés acceder a opciones de pantalla, rendimiento y controles.
- **Herramientas adicionales:** importación de archivos CPK para traducciones y paquetes de idiomas, además de exportación de registros de depuración.

### Requisitos

- Teléfono o tablet Android arm64 con **Android 10 o superior**, **Vulkan 1.1** y al menos 6 GB de RAM (8 GB recomendados).
- Espacio de almacenamiento: aproximadamente 15 GB para el juego y 1 GB adicional para shaders y partidas.
- Tu propia copia de **Persona 5 Royal 1.0.0** (US `01005CA01580E000`, EU `01004B10157F2000`, JP `0100B880154FC000` o Asia `01006BB00C6F0000`) y las `prod.keys` de tu consola.

### Estado y compatibilidad

El flujo de instalación sin PC (selección del juego y las claves, preparación de shaders y ejecución desde el NSP) ha sido verificado hasta ahora en un dispositivo:

| Dispositivo | SoC / GPU | Resultados |
|---|---|---|
| Lenovo Tab M11 (Android 14, 8 GB) | Snapdragon 685 / Adreno 610, Vulkan 1.1 | Menús a 60 FPS, título 3D ~14 FPS, casino ~20 FPS |

La Adreno 610 es una GPU de gama baja, por lo que se esperan mejores resultados en dispositivos más potentes. Otras GPU (Mali, PowerVR, Xclipse y Adreno más recientes) todavía no han sido verificadas. Se agradecen los reportes de compatibilidad.

Entre las funciones operativas se encuentran los menús, vídeos mediante decodificación VP9 por hardware, audio, cinemáticas sincronizadas, combates, guardado y carga de partidas, límite de 30 o 60 FPS, controles táctiles y mandos físicos.

La rama **Chakeyy Launcher Rework** incorpora además el rediseño del launcher, el menú de pausa, opciones ampliadas para mandos y el editor de distribución de controles táctiles. La compatibilidad y el rendimiento pueden variar según el dispositivo y su GPU.

Limitaciones conocidas: solamente versión 1.0.0 (no la actualización) y rendimiento limitado en GPU de gama baja. La opción de sombras tiene un costo elevado en dispositivos menos potentes y está desactivada por defecto.

### Reportar problemas

Si encontrás un problema, abrí un issue e incluí el modelo del teléfono, SoC/GPU, versión de Android, memoria RAM, los pasos que realizaste y lo que ocurrió.

Podés generar un registro mediante:

```bash
adb logcat -s p5r:V > p5r-log.txt
```

Ejecutá el comando antes de iniciar el juego. No adjuntés archivos del juego, claves ni capturas de contenido protegido por derechos de autor que no sean necesarias para explicar el problema.

## Credits and license

- **Original Android port:** [Brandon04Gonzalez / p5r-android](https://github.com/Brandon04Gonzalez/p5r-android). Original Android port and foundational implementation.
- **Launcher rework:** [Chakeyy / p5r-android](https://github.com/Chakeyy/p5r-android). Redesigned launcher and related Android interface improvements incorporated into the `Chakeyy-Launcher-Rework` branch.
- **Current fork and integration:** [synndras / P5R-Android-Plus](https://github.com/synndras/P5R-Android-Plus). Integration, additional development and maintenance of this fork.
- **Runtime:** [SwitchGamesRecomp / Super Mario Bros. Wonder Recomp](https://github.com/BelmanteGu/SuperMarioWonderRecomp) by BelmanteGu (Projeto Belmante), MIT.
- **Shader translation:** Ryujinx's shader translator (Ryubing), MIT, built into the APK as a native library.
- The native-execution idea is the one Switch emulators call NCE; this implementation is written for this runtime.
- Like the upstream project, this port was written with AI assistance (Anthropic's Claude), directed and tested by a human.

MIT License, see [LICENSE](LICENSE). Please also refer to the respective upstream repositories and their licenses for third-party components.
