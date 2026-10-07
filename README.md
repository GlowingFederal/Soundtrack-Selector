# Soundtrack Selector

A client-side Minecraft mod for choosing which available audio tracks can play as background music. Install the matching JAR on the client; a server installation is not required. There is no networking protocol.

## Using the selector

Press **M** in-game to open Soundtrack Selector. Change the binding in Minecraft's Controls menu. Search by track name, namespace, sound event, or resource path. Click a row to select it and its small square to enable/disable it. Use the mouse wheel to scroll (legacy screens also support Page Up/Down). Choose **Music**, **Soundtrack** (enabled tracks), or **Long audio**; sort by name, source, or duration. **All on** and **All off** affect the entire filtered view, including rows outside the viewport.

The player footer shows track details, now-playing information, and approximate elapsed progress. **Play** (or double-clicking a track row) plays the selected resource even when disabled, then advances through the enabled playlist; **Pause/Resume** controls only that music channel; **Stop** ends it; **Next** immediately plays the next enabled track. Closing the selector ends player playback and resumes normal background timing. Minecraft's Music volume applies. Use the playlist-name field and **New** to copy the current selections into a saved named playlist. Click the current playlist name to cycle through saved playlists. **Delete** requires a second confirmation click; Default is retained. Each playlist keeps its own choices, including missing tracks. Existing format-1 selections migrate to Default. No seeking or per-track volume editor is included.

Selections are saved immediately in `config/soundtrackselector.json`. Each audio resource has its own stable identifier; different files in one sound event appear separately. New music candidates default to enabled; other long audio defaults to disabled unless explicitly selected. Choices for missing resources remain stored so returning mods/resource packs retain their settings.

Only readable Ogg/Vorbis resources **longer than 10 seconds** appear. Duration is measured from container metadata without decoding audio. The default Music view also requires a music/song/soundtrack/record/disc token in the resource path or referring event. Long cave/ambient effects appear only in Long audio unless enabled deliberately. This name heuristic can miss unusually named modded music; use Long audio and search its namespace to add it. The duration cutoff applies to every view. Short sounds continue to work normally; this mod controls the background-music path only. Mods playing music through independent sound players or custom music-manager replacements need separate compatibility work.

**Loop** repeats the enabled playlist and is on by default. **Random** shuffles each pass and is also on by default; turn it off to play tracks alphabetically by name. Every enabled track plays once per pass. With Loop off, playback ends at the end of the playlist; Play or Next starts playback again. These two settings are saved for the client and apply to both the player and normal background selection. Minecraft's background delays, volume, menu/world behavior, and sound system remain in use. Dragon and credits music are preserved. Playlists are local to this client. The selector does not add biome/combat rules, downloading, or external audio formats.

Resource reloads rebuild the cached catalog and preserve choices. For 1.6.4, discovery can enumerate active file/folder resource packs, default assets, and registered sound pools; opaque custom resource packs that expose no file list may only be discoverable through their pool registrations.

## Supported targets

| Minecraft | Loader pin | Build tooling / wrapper | Build JVM | Compiler / shipped Java |
|---|---|---|---|---|
| 1.6.4 | Forge 9.11.1.1345 | Unimined 1.4.1 / Gradle 8.13 | 21 | JDK 17, `--release 7`; game Java 8 |
| 1.7.10 | Forge 10.13.4.1614 | GTNHGradle 2.0.34, RFG 2.0.6 / Gradle 9.7.1 | 25 | JDK 17 + Jabel, shipped Java 8 |
| 1.8.9 | Forge 11.15.1.2318 | Unimined 1.4.1 / Gradle 8.13 | 21 | Java 8 |
| 1.12.2 | Forge 14.23.5.2864 | Unimined 1.4.1 / Gradle 8.13 | 21 | Java 8 |
| 1.16.5 | Forge 36.2.42 | ForgeGradle 6.0.54 / Gradle 8.13 | 21 | Java 8 |
| 1.18.2 | Forge 40.3.12 | ModDevGradle Legacy Forge 2.0.148 / Gradle 8.13 | 21 | Java 17 |
| 1.20.1 | Forge 47.4.10 | ModDevGradle Legacy Forge 2.0.148 / Gradle 8.13 | 21 | Java 17 |
| 1.21.11 | NeoForge 21.11.45 | ModDevGradle 2.0.148 / Gradle 9.2.1 | 21 | Java 21 |

The 1.6.4 build keeps stock Forge/ASM 4.1 and verifies every final class is at most major 51. No launcher/library replacement is required.

## Building and development

Based on the independent-target structure of Examplemod-Multiversion. The template repository is a reference; this project has its own identity, source, resources, and version state. No preprocessing framework is used.

Install Python 3.9+ and the JDKs in the table. The wrappers select their build JVM from `JAVA21_HOME`/`JAVA25_HOME`, `JAVA_HOME`, or standard installations. Keep `JAVA8_HOME`, `JAVA17_HOME`, `JAVA21_HOME`, and `JAVA25_HOME` available for compiler/game toolchains. Set `PYTHON` if Python is not on PATH.

```powershell
# All eight production targets, with one shared build number:
./gradlew.bat buildAll
# One production target:
./scripts/build-version.ps1 1.20.1
# One development client:
./gradlew.bat runClient1_20_1
# Verify an existing production artifact:
python scripts/build.py verify 1.20.1 --full-version 1.0.0.1
```

On Unix use `sh ./gradlew` or `python3 scripts/build.py all`. Production archives are under `versions/<minecraft>/build/libs/`, named `soundtrackselector-<semantic-version>.<build-number>+mc<target>-<loader>.jar`. `-dev.jar` files are not distributables. Only successful production transactions advance `version.properties`; development launches do not.

The root Gradle project models platform-neutral `common` code and aggregate launchers. Open `versions/<target>` as a separate Gradle project for native Minecraft completion/debugging, using that target's build JVM. Portable launch shortcuts remain under `.run/`. Unimined uses `genSources genIntellijRuns`; GTNH uses `setupDecompWorkspace idea`; ForgeGradle uses `genIntellijRuns`; ModDevGradle supplies native runs during IDE sync (`neoForgeIdeSync`).

`common` contains resource definitions, Ogg metadata, track/config/list models. Each target owns loader initialization, resource access, music integration, keybinding, and GUI. Runtime assets are generated from `common/assets/en_us.properties` in the native language format. The generated `BuildVersion` follows the template's version transaction. Production verification is configured in `verification.json`.

See [music/discovery architecture](docs/ARCHITECTURE.md) for native integration details. The pinned 1.6.4 development launcher materializes downloaded hashed assets into its native legacy resources directory before launch. Generated outputs, caches, game directories, logs, and IDE state remain ignored. Forge/FML license and credit files are retained as third-party notices. Project source is MIT licensed.
