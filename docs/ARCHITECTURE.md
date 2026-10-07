# Music and discovery architecture

## Shared model

The authoritative catalog contains one `Track` per resolved audio-resource ID, not one per event. It retains all readable resources longer than ten seconds, with a music-candidate flag derived from identifier tokens; Music is the default view. Long audio exposes non-candidates for deliberate selection. It records namespace, originating event, readable name, duration, and search text; enabled state lives in `Preferences`. Catalog replacement sorts the rows and rebuilds an enabled array. Playback uses a cached order and cursor shared by native background selection and player controls. Random order uses a Fisher-Yates shuffle once per pass; otherwise tracks sort by display name with resource ID as a stable tie-breaker. Selection is an array lookup; each enabled entry plays once per pass. Loop repeats at the boundary, while non-looping exhaustion returns no selection until a user restarts or changes the catalog/order. GUI filtering is rebuilt only on query/catalog changes; screens draw only the visible rows.

`config/soundtrackselector.json` uses format version 2, an active playlist name, named resource-ID-to-boolean maps, and optional client-wide `loop` and `randomOrder` booleans. Both playback modes default to true when absent, preserving existing files. Format-1 track choices migrate to Default; the old representation is rewritten on the next save. Missing IDs remain in the file. Absent track settings mean enabled for music candidates and disabled for other long audio. Explicit existing choices take precedence. Changes are written as UTF-8 JSON through a temporary file and atomic replacement where supported. Invalid configuration is logged and preserved; changes become session-only until the file is repaired and the client restarted.

## Duration and definitions

For 1.7.10 onward, native adapters enumerate resource-manager namespaces and read each `sounds.json` resource stack in pack order. Definitions honor `replace` and recursively expand `type: event` references (cycle guarded, depth bounded). File references resolve to `<namespace>:sounds/<path>.ogg`; an unqualified file/event name uses Minecraft's default namespace, as in the native sound loader. Duplicate physical IDs are collapsed before opening audio. Namespaces reflect the audio resource, and a music-like referring event is preferred for classification; otherwise the first originating event in stable event order is retained for search.

The active resource manager resolves the winning file from the pack stack. Discovery reads every unique referenced file once per reload epoch, including short files. Ogg identification packets provide the Vorbis sample rate; end-of-stream granule positions provide the sample count. Duration is samples/rate, summed for chained Vorbis streams. Page bodies are skipped, not decoded into PCM. Truncation, unsupported codecs, missing end pages, and unreadable resources are skipped. Parsing failures include resource IDs in debug logs; one discovery summary reports eligible, short, and failed counts.

The mandatory cutoff is strictly `duration > 10.0`. Streaming flags and event names do not bypass it. A subsequent identifier-token heuristic separates music candidates from other long audio without discarding uncommon modded resources. Duration metadata is cached in the catalog until the next resource reload; no audio scanning occurs during music selection or steady-state ticking.

Adapters also inspect the native registered accessor lists for concrete file entries, including directly registered modded audio absent from sounds.json. Event-reference wrappers are expanded through definitions; opaque programmatic wrappers cannot be reliably expanded without a public resource list. No special mod API is required for ordinary resource-defined or directly registered file tracks.

## Native integrations

| Target | Discovery and lifecycle | Background music integration |
|---|---|---|
| Forge 1.6.4 | Enumerate active default/file/folder resource packs and registered sound pools; resolve resources through the active manager or pool URL. Reload listener and SoundLoadEvent invalidate the catalog. | Dedicated PlayBackgroundMusicEvent substitutes one SoundPoolEntry. Vanilla SoundManager retains its interval, music volume, BgMusic source, and record suppression. An empty catalog installs a delay instead of retrying selection each tick. |
| Forge 1.7.10 | Resource domains + sounds.json stacks; legacy reload listener. | Native MusicTicker subclass retains legacy timing/context behavior. Single-file selector-owned SoundRegistry composites expose streamed SoundPoolEntry variants. Only the background ticker uses those private entries. |
| Forge 1.8.9 | Resource domains + sounds.json stacks; legacy reload listener. | Native MusicTicker subclass, including its start/stop methods; single-file selector-owned streamed registry composites. |
| Forge 1.12.2 | Resource domains + sounds.json stacks; legacy reload listener. | Native MusicTicker subclass uses a PositionedSound whose accessor resolves directly to one streamed Sound. |
| Forge 1.16.5 | Resource namespaces + sounds.json stacks; reload listener. | MusicTicker.startPlaying override and LocatableSound direct resolution. Vanilla tick owns delays and replacement rules. |
| Forge 1.18.2 | Resource namespaces + sounds.json stacks; RegisterClientReloadListenersEvent. | MusicManager.startPlaying override and AbstractSoundInstance direct resolution. Vanilla tick owns delays and replacement rules. |
| Forge 1.20.1 | Resource namespaces + sounds.json stacks; RegisterClientReloadListenersEvent. | MusicManager.startPlaying override and AbstractSoundInstance with SampledFloat sound parameters. Vanilla tick owns delays and replacement rules. |
| NeoForge 1.21.11 | Resource namespaces + sounds.json stacks; AddClientReloadListenersEvent. | MusicManager.startPlaying override, Identifier-based direct sound. Vanilla tick retains SelectMusicEvent, frequency, and gain behavior. |

The narrowly scoped manager substitution is installed once on the client. Fields are resolved by exact native type, so private-field access is independent of mapped names; ambiguous layouts fail visibly rather than guessing. Forge 1.6.4's delay has an explicit MCP/SRG name pair because its manager contains several integer fields. A pre-existing custom manager replacement is detected and reported instead of overwritten.

From 1.16.5 onward, a selected sound retains the situational event ID while resolving the chosen audio file directly. This keeps vanilla replacement/isPlayingMusic decisions coherent and allows the manager and sound engine to track the same sound object. The native manager owns stopping that object; no parallel background scheduler is introduced. World identity changes, reloads, and disabled-current-track settings stop controlled playback. Sound-engine restart is handled by the native inactive-sound check. Selections are made only when a new track is required.

Menu/game transitions retain native contexts and delays. Tracks are drawn from the same enabled catalog in ordinary contexts; there are no biome/dimension playlists. Dragon and credits contexts bypass replacement. Disabling an entry controls its future selection through this mod's background path, not independent music played by another mod or record playback.

1.6.4 lacks sounds.json and resource listing. Its native pack/file enumeration also finds file variants that old pools group under one name. Opaque custom packs have only their discoverable registrations available. Its vanilla background system has no modern situational music manager or menu/game playlist distinction.

## Sides and ownership

There is no network channel, server command, item, block, or server config. Legacy proxies isolate all client types; modern entrypoints only register client listeners on the physical client. Legacy remote-version declarations and modern display tests accept servers without this mod. Dedicated servers can safely ignore client behavior.

Reload callbacks only mark a volatile flag. The client tick performs one discovery pass after invalidation, publishes the new catalog, and installs old-version private sound entries. Resource streams and opened archives are closed. Minecraft/sound-system mutation and GUI actions occur on the client thread. Discovery does not run continuously or create a background world-mutating worker.

The controlled-path compatibility boundary is the stock music manager/background event. Mods that replace the whole manager, bypass the normal sound engine, or mutate definitions without a resource reload need explicit follow-up integration.

## Player interface

The shared `PlayerView` contains only layout, cached list actions, and drawing primitives; every target supplies native widgets, font rendering, input, and screen lifecycle. The responsive interface has Music/Soundtrack/Long audio views, search, name/source/duration sorting, selected-resource details, now-playing status, and approximate wall-clock progress (frozen during player pause). Bulk operations snapshot the full filtered view, so enabled-only views can safely disable all their members in one operation. Screens draw only visible rows.

Player playback uses the same native background source/manager as automatic music and stops the previous source first. Play or a double-click on a track row starts at the selected resource and can audition disabled tracks without changing preferences; subsequent selections use the enabled playlist. Native inactive-sound checks advance the player queue, with no timer-based audio restarts or parallel scheduler. Unexpected early termination stops player playback instead of creating a muted/failed-audio retry loop. While playing or paused, automatic manager selection is suspended; world changes, reloads, or closing the screen release it and restore normal timing. Ordinary background tracks stop when disabled. Closing a screen stops player playback but resumes a paused ordinary background track.

Pause addresses only the current instance's channel: Paulscode's per-source API through the native instance/channel map on 1.7.10-1.12.2, the named BgMusic source on 1.6.4, and ChannelHandle/Entry operations queued through the native audio executor on 1.16.5 onward. No whole-engine pause, global sound stop, extra scheduler, or parallel playback system is used.

Saved playlists are the authoritative preference sets. Creating one copies the current explicit choices; selecting one rebuilds the enabled cache and stops current music. Deleting a named playlist returns to the protected Default playlist. Names are unique, trimmed, at most 48 characters, and cannot contain control characters. Every playlist independently retains missing resource IDs. No networking or server policy is involved.

NeoForge 1.21.11 uses its native optional-format mod-pack metadata (description only). The exact supported game version remains in mod metadata. A single vanilla resource-format bound would be invalid when the same mod pack is inspected as server data because the resource/data format versions differ; NeoForge supplies native compatibility metadata for both pack types.
