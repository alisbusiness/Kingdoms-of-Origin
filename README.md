# Kingdoms of Origin

A Fabric 1.20.x server-side mod that adds a player-elected **King** office with Origins integration. Players nominate themselves, campaign, and vote during election cycles. The winner receives the **King Zeus** origin (or any custom origin you configure), granting unique powers for the duration of their term.

---

## Requirements

| Requirement | Version |
|---|---|
| Java | 21+ |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.15.0+ |
| Fabric API | 0.92.2+1.20.1 (or compatible) |
| Origins mod | 1.10.x for 1.20.x (**optional** — origin features disabled without it) |
| BlueMap | 3.x+ (**optional** — map marker integration) |
| Dynmap | 3.4+ (**optional** — map marker integration) |

---

## Installation

1. Drop `kingdoms-of-origin-<version>.jar` into your server's `mods/` folder.
2. Drop the `origins-fabric-*.jar` into `mods/` if you want origin assignment features.
3. (Optional) Install BlueMap or Dynmap for map marker integration — set `map.provider` in config.
4. Copy the `datapack/` folder from this release into `world/datapacks/kingdoms_of_origin/`.
5. Start the server — `config/kingdoms_of_origin/config.yml` is generated automatically.
6. Run `/datapack enable "file/kingdoms_of_origin"` (or restart) to activate the datapack.
7. (Optional) Edit `config.yml` and run `/kingdom admin reload` (config re-read on next restart).

---

## Datapack Setup

The datapack lives at `src/main/resources/datapack/` in the source tree. To install it on a server:

1. Copy the entire `datapack/` directory into `<world>/datapacks/kingdoms_of_origin/`.
   The final path should look like `world/datapacks/kingdoms_of_origin/pack.mcmeta`.
2. Either restart the server or run `/datapack enable "file/kingdoms_of_origin"` followed by `/reload`.
3. Verify with `/origin layers` that the `server:office` layer is present.

To change the King origin without touching Java code:
- Edit `data/server/origins/king_zeus.json` to change powers or appearance.
- Or create a new origin file (e.g. `data/server/origins/king_poseidon.json`) and update `origin_mode.king_origin_id` in `config.yml` to point to it.

To add more origins to the random-origin pool:
- Add origin files in `data/server/origins/` **or** reference any existing Origins origin.
- Add the identifier to `random_origin.allowed_origins` in `config.yml`.

---

## config.yml Reference

| Key | Type | Default | Description |
|---|---|---|---|
| `office.id` | string | `king` | Internal DB key for the office. Changing loses existing state. |
| `office.display_name` | string | `King` | Name shown in chat and the GUI. |
| `office.term_days` | int | `7` | Real-world days per term before a new election triggers. |
| `office.election_enabled` | bool | `true` | Disable to manage office entirely via admin commands. |
| `office.nomination_days` | int | `2` | Days the nomination window stays open. |
| `office.campaign_days` | int | `2` | Days between nominations closing and voting opening. |
| `office.voting_days` | int | `1` | Days the voting window stays open. |
| `origin_mode.type` | string | `layer` | `layer` = separate Origins layer; `replace` = replaces primary origin. |
| `origin_mode.king_origin_id` | string | `server:king_zeus` | Origins ID of the king origin. Change freely — no Java edit needed. |
| `origin_mode.king_layer_id` | string | `server:office` | Layer ID used in layer mode. Must match the datapack layer file. |
| `origin_restore.restore_previous_origin_on_term_end` | bool | `true` | Restore the player's pre-office origin when their term ends. |
| `origin_restore.clear_king_origin_on_term_end` | bool | `true` | Remove the king origin when the term ends. |
| `transition.give_orb_on_transfer` | bool | `true` | Give an Orb of Origin to the incoming ruler on election transfer. |
| `transition.give_orb_on_abdication` | bool | `true` | Give an Orb of Origin when the ruler abdicates voluntarily. |
| `transition.give_orb_on_forced_removal` | bool | `false` | Give an Orb of Origin on admin forced removal. |
| `voting.system` | string | `plurality` | Vote tallying system (only `plurality` implemented). |
| `voting.minimum_playtime_minutes` | int | `60` | Minutes of playtime required to cast a vote. |
| `voting.anonymous_votes` | bool | `true` | Hide individual votes; only reveal the final tally. |
| `random_origin.enabled` | bool | `true` | Enable random-origin assignment for new players. |
| `random_origin.assign_on_first_join` | bool | `true` | Assign a random origin on first join. |
| `random_origin.allowed_origins` | list | `[origins:avian, origins:feline]` | Pool of origins for random assignment. |
| `random_origin.excluded_origins` | list | `[server:king_zeus]` | Always excluded from the pool. The king origin is also excluded at runtime. |
| `ui.use_scoreboard_announcements` | bool | `true` | Show announcements on the scoreboard sidebar. |
| `ui.use_bossbar_during_election` | bool | `true` | Display a boss bar during the voting window. |
| `ui.send_chat_broadcasts` | bool | `true` | Broadcast major events to all players in chat. |
| `map.provider` | string | `bluemap` | Map plugin: `bluemap`, `dynmap`, or `none`. |
| `map.show_capital_marker` | bool | `true` | Place a Capital POI marker on the map. |
| `debug.log_origin_transfers` | bool | `true` | Log every origin assignment to the console. |
| `debug.log_gui_actions` | bool | `false` | Log GUI open/close events (verbose). |

---

## Command Reference

### Player Commands

| Command | Permission | Description |
|---|---|---|
| `/kingdom status` | any player | Show current ruler, term end, and election phase. |
| `/kingdom ruler` | any player | Show the current ruler's name and term details. |
| `/kingdom candidates` | any player | List candidates in the current election. |
| `/kingdom run [slogan]` | any player | Register yourself as a candidate (nomination phase only). |
| `/kingdom vote` | any player | Open the voting GUI (voting phase only). |
| `/kingdom menu` | any player | Open the main kingdom GUI. |
| `/kingdom help` | anyone | Show help text. |

### Admin Commands

All admin commands require operator level 2 (`isOp`).

| Command | Description |
|---|---|
| `/kingdom admin start-election` | Force-start an election for the current office. |
| `/kingdom admin end-election` | Force-advance the current election to the next phase. |
| `/kingdom admin set-ruler <player>` | Directly appoint a player as ruler. |
| `/kingdom admin remove-ruler` | Remove the current ruler without transferring office. |
| `/kingdom admin force-transfer <player>` | Transfer office from current ruler to the target player. |
| `/kingdom admin give-orb <player>` | Give an Orb of Origin to a player. |
| `/kingdom admin set-phase <phase>` | Manually set the election phase (`NOMINATION`, `VOTING`, etc.). |
| `/kingdom admin assign-random-origin <player>` | Assign a random origin from the configured pool to a player. |
| `/kingdom admin debug-sync` | Validate and re-sync the current ruler's king origin. |
| `/kingdom admin reload` | Placeholder — config is currently read at startup only; restart to apply changes. |

---

## Origin Mode Explanation

### Layer mode (`origin_mode.type: layer`)

The King Zeus origin is assigned to a **separate Origins layer** (`server:office`). The player's primary origin (e.g., Avian) is untouched. When the term ends, the office layer is cleared. This is the recommended mode — it preserves player identity.

### Replace mode (`origin_mode.type: replace`)

The King Zeus origin **replaces** the player's primary origin while they hold office. The previous origin is stored in the database and restored when the term ends (if `origin_restore.restore_previous_origin_on_term_end: true`).

---

## King Zeus Powers

| Power | Type | Description |
|---|---|---|
| Divine Wings | Passive | Elytra-style flight without needing an elytra equipped. |
| Wrath of Zeus | Active (primary key) | Summon a lightning bolt ahead of you. 60 s cooldown. |
| Divine Presence | Passive | Emits enchantment particles visible to nearby players. |
| Divine Resilience | Conditional passive | Resistance I while above 7.5 hearts (15 HP). |
| Child of the Sky | Drawback | Hunger I while in complete darkness (underground). |

---

## Known Limitations / MVP Scope

- Config is loaded **once at startup**; `/kingdom admin reload` does not yet re-read `config.yml`. Restart the server to apply config changes.
- Only the **plurality** voting system is implemented. Ranked-choice and runoff are planned.
- Map marker coordinates (Capital, Election Hall) are hardcoded placeholders in `MapIntegrationService.java` with a `TODO` comment. A `locations.yml` file is planned.
- Dynmap `onRulerChanged` update requires storing the API reference after `apiEnabled()` — see the `TODO` comment in `MapIntegrationService.java`.
- The Orb of Origin crafting recipe (`data/server/recipes/orb_of_origin.json`) works as a standard Minecraft datapack recipe. Verify that your Origins version registers `origins:orb_of_origin` as a valid item.
- No tests exist yet. `./gradlew build` (compilation) is the primary correctness check.
