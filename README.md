# Soundtrack Selector

A client-side Minecraft music browser and custom playlist player. Install the matching JAR on the client; a server installation is not required. There is no networking protocol.

## Using the selector

Every game session starts in the permanent **Vanilla** mode. Minecraft controls its normal music timing and context, including menu, biome, dimension, boss and credits music. Discovering a mod track, record or other long sound does not make it background music. Other mods can still request their own music, and music discs retain their normal playback.

Press **M** in-game to open Soundtrack Selector. Change the binding in Minecraft's Controls menu. Search by track name, nickname, namespace, sound event, or resource path. Click a row to select it and its small square to add/remove it from the selected custom playlist. Use the mouse wheel or drag the scrollbar to scroll. Page Up/Down scroll a page; arrow keys select tracks when a text field is not focused. Tab navigates controls, Enter activates a focused control or plays the selected row, and Space activates a focused control or pauses/resumes playback. Choose **Music**, **Soundtrack** (enabled tracks), or **Long audio**; sort by name, source, or duration. **All on** and **All off** affect the entire filtered view, including rows outside the viewport. Vanilla's membership cannot be edited: choose a user playlist first.

**Play** (or double-clicking a row) plays the selected resource even when it is not in the playlist. Select a custom playlist and use **Start** to begin its enabled tracks; while playing, this button becomes **Next**. **Pause/Resume** controls only the custom track. **Stop** returns to Vanilla without forcing a new song. Closing the selector keeps custom playback running, preserving playlist progress and pause state; reopen it with **M** to control playback. Minecraft's Music volume applies; the player slider and +/- keys adjust that same setting. **Previous** revisits confirmed tracks in the current playback session (up to 64 entries), and is unavailable before another track has played. Explicit playback is unavailable during dragon/credits music.

Use the playlist-name field and **New** to create a saved named playlist. Creating one from Vanilla starts empty; creating one from a user playlist copies its choices. Click the current playlist name to cycle through Vanilla and saved playlists. Selecting a playlist stops custom playback but does not start another track. **Delete** requires a second confirmation click; Vanilla and Default are retained. An existing user playlist named Vanilla appears as **Vanilla (user)** and remains separate from the built-in mode.

Select a track and click **Nickname**. The search field becomes the nickname editor; enter a name and click **Save name**. Save an empty value to clear it. Nicknames appear in the track list and player footer, while the original event and resource identifiers remain visible in secondary text and track details. Nicknames do not change playback IDs. For sound events with several tracks, names are associated with their definition slots, so replacing the audio in a slot preserves its nickname. Reordering a pack's sound pool can change which variant occupies a slot. Minecraft 1.6.4 falls back to resource IDs where no sound-event identity exists.

**Appearance**, in the upper-right corner, switches between **Classic** (pixel-art wood, stone and inset panels) and **Modern** (compact navigation and a clean library), each with **Light** and **Dark** modes. Changes apply while music continues playing. Modern shows an additional now-playing panel on sufficiently wide GUI-scaled screens; smaller layouts keep the library and bottom player accessible. The rotating record follows playback and holds its angle while paused. **Reduced animations** disables rotation and hover transitions. Appearance is saved separately in `config/soundtrackselector.appearance.properties`; it does not change playlists or trigger discovery. Resource packs can override the reusable textures under `assets/soundtrackselector/textures/gui/`.

Track icons indicate known Overworld, Nether, End, menu, record, or generic audio categories. Known vanilla music-disc textures are used when available; other tracks use bundled symbolic artwork. Artwork is local and does not require downloads.

Playlists and nicknames are saved immediately in `config/soundtrackselector.json`. Existing format-1 and format-2 playlists migrate without losing explicit choices, including missing resources. Newly discovered tracks are not automatically enabled. Vanilla uses its own reserved identity and never imports the discovered catalog. Its definition and recovery copy are generated automatically as `config/soundtrackselector.vanilla.json` and `config/soundtrackselector.vanilla.recovery.json`; each startup reconstructs them from native music categories. A damaged user configuration is preserved, with Vanilla still available for that session.

Only readable Ogg/Vorbis resources **longer than 10 seconds** appear. Duration is measured from container metadata without decoding audio. The default Music view requires a music/song/soundtrack/record/disc token in the resource path or referring event. Long cave/ambient effects appear in Long audio. This browsing heuristic can miss unusually named mod music; use Long audio and search its namespace to find it. It never controls natural background selection. Short sounds continue to work normally. The progress display shows elapsed wall-clock time, frozen during pause and bounded by the measured duration; it is an estimate, not an audio-device position. Seeking is unavailable. The volume slider changes Minecraft's Music category, not an individual track.

**Loop** repeats the enabled custom playlist and is on by default. **Random** shuffles each pass and is also on by default; turn it off to play tracks alphabetically by displayed name. Every enabled track plays once per pass. With Loop off, the end of the playlist returns to Vanilla. These settings apply only to explicit custom playback. Playlists are local to this client; the selector does not add biome/combat rules, downloading, or external audio formats.

Audio discovery runs in the background; Vanilla music and the selector remain available while it finishes. **Refresh**, beside Search, rescans the library without stopping the current track. Repeated refresh requests are combined. Resource reloads also refresh the catalog while preserving saved choices and nicknames, including missing tracks. For 1.6.4, discovery can enumerate active file/folder resource packs, default assets, and registered sound pools; opaque custom resource packs that expose no file list may only be discoverable through their pool registrations. On 1.7.10–1.12.2, opaque packs that generate audio dynamically need an independent discovery source; ordinary file/folder packs, default assets and classpath resources are supported. Independent music players remain outside Soundtrack Selector's custom playback ownership.

## Supported targets

| Minecraft | Loader pin | Build tooling / wrapper | Build JVM | Compiler / shipped Java |
|---|---|---|---|---|
| 1.6.4 | Forge 9.11.1.1345 | Unimined 1.4.1 / Gradle 8.13 | 21 | JDK 17, `--release 7`; game Java 8 |
| 1.7.10 | Forge 10.13.4.1614 | GTNHGradle 2.0.34, RFG 2.0.6 / Gradle 9.7.1 | 25 | JDK 17 + Jabel, shipped Java 8 |
| 1.8.9 | Forge 11.15.1.2318 | Unimined 1.4.1 / Gradle 8.13 | 21 | Java 8 |
| 1.12.2 | Forge 14.23.5.2859 | Unimined 1.4.1 / Gradle 8.13 | 21 | Java 8 |
| 1.16.5 | Forge 36.2.34 | ForgeGradle 6.0.54 / Gradle 8.13 | 21 | Java 8 |
| 1.18.2 | Forge 40.3.0 | ModDevGradle Legacy Forge 2.0.148 / Gradle 8.13 | 21 | Java 17 |
| 1.20.1 | Forge 47.4.10 | ModDevGradle Legacy Forge 2.0.148 / Gradle 8.13 | 21 | Java 17 |
| 1.21.11 | NeoForge 21.11.45 | ModDevGradle 2.0.148 / Gradle 9.2.1 | 21 | Java 21 |

The listed Forge builds are minimum supported versions, selected from the [official Recommended builds](https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json). Newer Forge builds for the same Minecraft version remain supported. Minecraft 1.21.11 uses the stable NeoForge 21.11.45 baseline; its JAR requires NeoForge.

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

After development/testing, run `./gradlew.bat pruneBuilds` from the root (`./gradlew pruneBuilds` on Unix). This manual task is never attached to build, assemble or clean. It independently retains the newest production JAR in each supported `versions/<minecraft>/build/libs/` directory, using filesystem creation time rather than version/build-number ordering. If creation time is unavailable or non-positive, that target uses modification time; equal timestamps use absolute path as a stable tie-breaker. Sources, javadoc, dev/deobf, API and other classifier JARs, unrelated filenames and nested directories are excluded; symbolic links and redirected output directories are not pruned. Copying or overwriting files can preserve old creation times. Run pruning after builds finish. In IntelliJ, refresh the root Gradle project and run **Tasks > build > pruneBuilds**, or choose **Soundtrack Selector - Prune Old Builds** from the shared `.run/` configuration. The task prints retained/deleted artifacts and empty/single-artifact results.
