# Changelog

2026-10-06 22:45 â€” Initial Soundtrack Selector implementation

- Created the client-side soundtrack selector across all eight ExampleMod targets, preserving independent native builds and pinned loader/Java toolchains.
- Added resource-level music discovery, native sound-registry file discovery, cached Ogg/Vorbis metadata, and the strict longer-than-10-seconds eligibility rule.
- Added searchable visible-row GUIs, configurable M keybinding, per-resource toggles, Enable All/Disable All, and atomic human-readable client preferences that retain missing tracks.
- Integrated equal-weight selection with native background music timing and lifecycle; preserved dragon/credits music and ordinary sound/record playback.
- Added reload handling, restrained discovery/failure logging, correct client metadata, and project-specific archive verification. Retained stock Forge 1.6.4/ASM 4.1 with Java 7 distribution bytecode.

- Corrected the pinned 1.6.4 development launcher's legacy asset layout without changing stock production libraries or the template repository.

2026-10-06 23:03 â€” Music library and player controls

- Replaced the basic toggle list with a responsive music-player layout, track details, now-playing status, approximate progress, source/name/duration sorting, and bulk controls for the current view.
- Added Music, enabled Soundtrack, and Long audio views. Music identifiers default to enabled; long ambient/cave audio remains available but defaults to disabled. Explicit preferences remain authoritative.
- Added resource preview, per-track pause/resume, stop, and random next-track controls across all eight targets, using the existing background music source and native audio ownership. Closing the screen ends previews and restores normal background timing.

- Added saved named playlists with independent selections, persistent active-playlist state, copy-on-create, confirmation before deletion, and migration of existing selections into Default.

- Used NeoForge's native optional-format mod-pack metadata so client assets do not declare an incompatible server-data format.

2026-10-06 23:25 â€” Loop and random playback order

- Added persistent Loop and Random controls across all eight targets, both enabled by default. Loop repeats the enabled playlist; Random shuffles each pass, with alphabetical playback when disabled.
- Unified player and background selection around a cached playlist order. Each enabled track plays once per pass; non-looping playback ends at the last track and can be restarted with Preview or Next.
- Player playback advances when the native music source finishes, preserving pause, single-source ownership, and normal background timing outside the player.

2026-10-06 23:46 â€” Play button and double-click playback

- Renamed Preview to Play across all eight targets and updated player hints and playback messages.
- Added double-click playback on track rows through the existing player path; single clicks select, and checkbox clicks continue to change eligibility without starting audio.

2026-10-07 00:28 â€” Restore Recommended Forge compatibility (1.0.1.1)

- Compile Minecraft 1.12.2, 1.16.5 and 1.18.2 against Recommended Forge 14.23.5.2859, 36.2.34 and 40.3.0; align loader metadata, target records and existing archive expectations with those compatibility floors. Preserve the existing Recommended Forge pins for other Forge targets and the stable NeoForge 21.11.45 baseline.
- Correct legacy mcmod.info version-reference syntax and required Forge dependencies, preserving current gameplay and optional-client behavior.
- Use the string dependency-information flag required by FML 1.6.4; keep the Boolean representation on later targets.
- Use the lowercase forge dependency ID introduced by FML 1.12.2; retain the native Forge ID on earlier targets.
- Bump the semantic patch version and start the corrected release at 1.0.1.1.

2026-10-07 01:39 â€” Add manual production build pruning

- Add opt-in root pruneBuilds and the portable Soundtrack Selector - Prune Old Builds IntelliJ configuration. Keep the newest production JAR independently per supported Minecraft target using creation time, with a modification-time fallback and deterministic ties.
- Restrict cleanup to production filenames in target build/libs directories, excluding classifier/unrelated archives, nested outputs and redirected paths; preserve existing mod versioning and build behavior.

2026-10-12 03:49 — Restore Vanilla music ownership and add track nicknames

- Add a permanent built-in Vanilla mode with a reserved identity, always selected at startup. Remove discovered-catalog selection from normal gameplay scheduling across all eight targets; only explicit custom playback acquires music ownership.
- Restore the original native music controller and its retained state on Stop, playlist exhaustion and lifecycle release. On 1.6.4, use a dedicated custom music source and restore the saved native countdown. Leave Vanilla music requests, record playback and independently controlled mod sounds unchanged.
- Generate native-category Vanilla definitions and recovery copies automatically, including when user preferences are unreadable. Reconstruct built-in data instead of trusting saved catalog contents; preserve damaged user files.
- Migrate existing playlist formats to format 3 while retaining explicit selections and missing resources. Keep a user playlist named Vanilla separate; new discovered tracks require explicit custom-playlist selection.
- Add editable, searchable track nicknames with clearing, immediate persistence and original identifiers in the UI. Use sound-event definition slots to distinguish pooled variants and retain names through resource replacement; fall back to resource identity where legacy pools provide no usable event.

2026-10-20 02:16 — Move audio discovery off the client tick

- Run pack enumeration, sound-definition parsing and buffered Ogg duration inspection on one bounded background worker across all eight targets. Publish complete immutable results on the client thread using current playlist preferences.
- Keep Vanilla playback independent of discovery, preserve active custom audio and its current playlist pass during library refresh, and create 1.7.10/1.8.9 private streaming entries only for the selected track.
- Add a nonblocking discovery indicator and Refresh beside Search. Coalesce repeated refresh requests, cancel invalidated generations and interrupt discovery at shutdown; ordinary screen transitions do not rescan the library.
- Use independent legacy file/index descriptors and worker-owned modern pack managers to isolate resource I/O from the client and sound engine. Preserve strict duration filtering, saved choices and nicknames.
- Legacy opaque packs are inspected through classpath resources and concrete file/jar pool URLs rather than shared mutable game resource APIs; dynamically generated audio requires an independent source.

2026-10-20 14:40 — Redesign the player with Classic and Modern themes

- Replace the technical selector presentation across all eight targets with a framed pixel-art Classic player and a compact Modern player, each with complete light/dark skin atlases, reusable icons, track category artwork and native vanilla-disc texture fallbacks. Collapse optional navigation/artwork regions at smaller GUI scales.
- Add an in-screen Appearance menu with immediate style/mode changes and reduced animations, persisted separately from soundtrack data through a serial background writer. Keep playback ownership, playlist data, search, selection, scroll and asynchronous discovery independent of theme changes.
- Animate a textured music record from the existing pause-aware playback clock, with a stationary pickup arm drawn in front; retain honest read-only elapsed progress. Add bounded Previous history and native Minecraft Music volume controls while preserving Play, Start/Next, Pause, Stop/Vanilla restoration, Loop, Random, nicknames and playlist management.
- Add draggable clipped scrolling, visible focus, keyboard navigation, icon tooltips and distinct selected/enabled/playing states. Add shared regression coverage for layout bounds, interaction semantics, theme isolation, history, artwork fallback, dragging and appearance persistence; require theme classes and textures in every production archive.
- Use shadow-free text and input presentation in light themes, retaining native editing/selection behavior and strengthening selected/disabled text contrast.

2026-10-21 02:35 — Keep playlists playing when the selector closes

- Remove playback release from selector close handlers across all eight targets. Closing the menu now preserves the current custom track, playlist progress, playback history and pause state instead of stopping playback and returning to Vanilla.
- Retain GUI input cleanup and preference saving on close; explicit Stop and existing playback lifecycle rules still restore Vanilla.
