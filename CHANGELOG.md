# Changelog

2026-10-06 22:45 — Initial Soundtrack Selector implementation

- Created the client-side soundtrack selector across all eight ExampleMod targets, preserving independent native builds and pinned loader/Java toolchains.
- Added resource-level music discovery, native sound-registry file discovery, cached Ogg/Vorbis metadata, and the strict longer-than-10-seconds eligibility rule.
- Added searchable visible-row GUIs, configurable M keybinding, per-resource toggles, Enable All/Disable All, and atomic human-readable client preferences that retain missing tracks.
- Integrated equal-weight selection with native background music timing and lifecycle; preserved dragon/credits music and ordinary sound/record playback.
- Added reload handling, restrained discovery/failure logging, correct client metadata, and project-specific archive verification. Retained stock Forge 1.6.4/ASM 4.1 with Java 7 distribution bytecode.

- Corrected the pinned 1.6.4 development launcher's legacy asset layout without changing stock production libraries or the template repository.

2026-10-06 23:03 — Music library and player controls

- Replaced the basic toggle list with a responsive music-player layout, track details, now-playing status, approximate progress, source/name/duration sorting, and bulk controls for the current view.
- Added Music, enabled Soundtrack, and Long audio views. Music identifiers default to enabled; long ambient/cave audio remains available but defaults to disabled. Explicit preferences remain authoritative.
- Added resource preview, per-track pause/resume, stop, and random next-track controls across all eight targets, using the existing background music source and native audio ownership. Closing the screen ends previews and restores normal background timing.

- Added saved named playlists with independent selections, persistent active-playlist state, copy-on-create, confirmation before deletion, and migration of existing selections into Default.

- Used NeoForge's native optional-format mod-pack metadata so client assets do not declare an incompatible server-data format.

2026-10-06 23:25 — Loop and random playback order

- Added persistent Loop and Random controls across all eight targets, both enabled by default. Loop repeats the enabled playlist; Random shuffles each pass, with alphabetical playback when disabled.
- Unified player and background selection around a cached playlist order. Each enabled track plays once per pass; non-looping playback ends at the last track and can be restarted with Preview or Next.
- Player playback advances when the native music source finishes, preserving pause, single-source ownership, and normal background timing outside the player.

2026-10-06 23:46 — Play button and double-click playback

- Renamed Preview to Play across all eight targets and updated player hints and playback messages.
- Added double-click playback on track rows through the existing player path; single clicks select, and checkbox clicks continue to change eligibility without starting audio.
