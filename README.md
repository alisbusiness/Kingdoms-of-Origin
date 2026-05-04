# Kingdoms of Origin

Welcome to **Kingdoms of Origin**! This is a Fabric 1.20.x server-side mod that transforms your multiplayer experience by introducing a player-elected **King** office integrated directly with the **Origins** mod.

Have you ever wanted to rule your server with divine powers? Now you can! Players can nominate themselves, run campaigns, and vote in server-wide elections. The victor is crowned King and granted the **King Zeus** origin (or any custom origin), unlocking unique, god-like powers for the duration of their reign.

---

## 👑 What This Mod Adds

### 1. The Election Cycle
The core of the mod is the democratic process. Elections happen automatically based on configurable term limits (default: 7 real-world days).
- **Nomination Phase:** Players use `/kingdom run [slogan]` to declare their candidacy.
- **Campaign Phase:** A period for candidates to rally support and make promises to the server.
- **Voting Phase:** Players use `/kingdom vote` to securely and anonymously cast their ballot via an interactive GUI.
- **Coronation:** Once voting concludes, the winner is automatically crowned!

### 2. The Power of The King
When a player is crowned King, they are bestowed with **The King** origin. This grants them incredible abilities to enforce their rule, but balances it with severe weaknesses:
- 👑 **Royal Decree (Primary Active):** Rally your subjects! Issue a command that grants Strength II and Resistance I to all players within 15 blocks for 15 seconds (60s Cooldown).
- ⚡ **Wrath of the King (Secondary Active):** Call down a devastating bolt of lightning on whatever block or entity you are looking at up to 30 blocks away (30s Cooldown).
- 💥 **Iron Fist (Passive):** Strike with the weight of your realm (+3 Attack Damage).
- 🛡️ **Crown's Resilience (Passive):** Fortified by the burden of the crown (+10 Max Health, +4 Armor, +2 Armor Toughness).
- 🏃 **Sovereign's March (Passive):** Never slow to act (+15% Movement Speed).
- 🪽 **Divine Flight (Passive):** Take to the skies to survey your domain using creative flight.
- ✨ **Royal Radiance (Drawback):** The crown marks you. All can see where you stand (Permanent Glowing effect).
- 👁️ **Undeniable Presence (Drawback):** A king cannot hide from his people or enemies (Immunity to Invisibility potions).
- 🎯 **Mark of the Crown (Drawback):** Enemies are drawn to power (Reduced Knockback Resistance).
- 🤲 **Soft Hands (Drawback):** Accustomed to ruling, not manual labor (-30% Mining Speed).
- 🏹 **Prime Target (Drawback):** An undeniable presence makes you an easy mark (+50% Projectile Damage Taken).
- 🌑 **Creature of Light (Severe Drawback):** Your sovereign power is tied to the light. In darkness, your max health is halved, you take +50% more damage, and suffer from crippling Weakness, Slowness, and Mining Fatigue.

### 3. Seamless Origin Transitions
The mod integrates perfectly with your existing Origins.
- **Layer Mode (Recommended):** The King origin is added as a secondary "layer" on top of your primary origin. You keep your base powers, and gain the King's powers. When your term ends, the King layer is simply removed.
- **Replace Mode:** The King origin completely replaces your current origin. When your term is up, your original origin is safely restored!
- **Orbs of Origin:** Configurable options allow the server to grant an Orb of Origin upon peaceful transfer of power or abdication.

### 4. Rich Server Integration
- **GUI Menus:** Beautiful, easy-to-use in-game menus (`/kingdom menu`) to view candidates, vote, and see election status.
- **Live Announcements:** Boss bars and scoreboard updates keep everyone informed about ongoing elections.
- **Map Support:** Integrates with BlueMap and Dynmap to highlight the Capital and the current ruler's domain.

---

## 🛠️ Server Administrator Guide

### Requirements
| Requirement | Version |
|---|---|
| Java | 21+ |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.15.0+ |
| Fabric API | 0.92.2+1.20.1 (or compatible) |
| Origins mod | 1.10.x for 1.20.x (**optional** — origin features disabled without it) |
| BlueMap | 3.x+ (**optional** — map marker integration) |
| Dynmap | 3.4+ (**optional** — map marker integration) |

### Installation

1. Drop `kingdoms-of-origin-<version>.jar` into your server's `mods/` folder.
2. Drop the `origins-fabric-*.jar` into `mods/` for origin assignment features.
3. (Optional) Install BlueMap or Dynmap for map markers. Set `map.provider` in config.
4. Copy the `datapack/` folder from this release into `world/datapacks/kingdoms_of_origin/`.
5. Start the server — `config/kingdoms_of_origin/config.yml` is generated automatically.
6. Run `/datapack enable "file/kingdoms_of_origin"` (or restart) to activate the datapack.

### Datapack Setup
The datapack lives at `src/main/resources/datapack/` in the source tree. To install it on a server:
1. Copy the entire `datapack/` directory into `<world>/datapacks/kingdoms_of_origin/`.
2. Either restart the server or run `/datapack enable "file/kingdoms_of_origin"` followed by `/reload`.
3. Verify with `/origin layers` that the `server:office` layer is present.

To change the King origin without touching Java code:
- Edit `data/server/origins/king_zeus.json` to change powers or appearance.
- Or create a new origin file and update `origin_mode.king_origin_id` in `config.yml` to point to it.

---

## ⚙️ Configuration (`config.yml` Reference)

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
| `ui.use_scoreboard_announcements` | bool | `true` | Show announcements on the scoreboard sidebar. |
| `ui.use_bossbar_during_election` | bool | `true` | Display a boss bar during the voting window. |
| `ui.send_chat_broadcasts` | bool | `true` | Broadcast major events to all players in chat. |
| `map.provider` | string | `bluemap` | Map plugin: `bluemap`, `dynmap`, or `none`. |
| `map.show_capital_marker` | bool | `true` | Place a Capital POI marker on the map. |
| `debug.log_origin_transfers` | bool | `true` | Log every origin assignment to the console. |
| `debug.log_gui_actions` | bool | `false` | Log GUI open/close events (verbose). |

---

## 📜 Command Reference

### Player Commands
These commands are available to all players:

| Command | Description |
|---|---|
| `/kingdom status` | Show current ruler, term end, and election phase. |
| `/kingdom ruler` | Show the current ruler's name, term details, and active perks. |
| `/kingdom candidates` | List candidates in the current election. |
| `/kingdom run [slogan]` | Register yourself as a candidate (nomination phase only). |
| `/kingdom promise <perks...>` | Declare your campaign promises as a candidate (comma-separated IDs). |
| `/kingdom setperks <perks...>` | Set the active policies for the server (King only, comma-separated IDs). |
| `/kingdom vote` | Open the voting GUI (voting phase only). |
| `/kingdom menu` | Open the main kingdom GUI to navigate all features. |
| `/kingdom help` | Show help text and available commands. |

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
| `/kingdom admin debug-sync` | Validate and re-sync the current ruler's king origin. |
| `/kingdom admin reload` | Placeholder — config is currently read at startup only; restart to apply changes. |

---


### 3. Royal Perks & Policies
The King can enact global policies that apply to all their subjects! When a player runs for office, they can use `/kingdom promise <perks>` to declare what perks they will enact if they win. Once elected, the King can use `/kingdom setperks <perks>` to officially enforce their chosen policies (they may choose to honor their promises or lie!). 

Active perks are granted automatically when players join the server, and revoked when the King's term expires.

Currently, the following 65 perks and disadvantages are available. Use the **ID** (e.g., `miners_haste_i`) in the command.

#### ⛏️ Mining Perks
- **Miner's Haste I** (`kingdom:perks/miners_haste_i`): +10% Mining Speed.
- **Miner's Haste II** (`kingdom:perks/miners_haste_ii`): +20% Mining Speed.
- **Miner's Haste III** (`kingdom:perks/miners_haste_iii`): +30% Mining Speed.
- **Deep Dark Resistance** (`kingdom:perks/deep_dark_resistance`): Immunity to Darkness and Blindness.
- **Ore Doubling** (`kingdom:perks/ore_doubling`): Ores drop twice as much.
- **Diamond Luck** (`kingdom:perks/diamond_luck`): Diamonds drop an extra gem.
- **Unbreaking Tools** (`kingdom:perks/unbreaking_tools`): Tools take 10% less damage.
- **Lava Immunity** (`kingdom:perks/lava_immunity`): Fire resistance while below Y=15.
- **Spelunker's Glow** (`kingdom:perks/spelunkers_glow`): Night Vision underground.
- **Obsidian Breaker** (`kingdom:perks/obsidian_breaker`): +50% mining speed on Obsidian.

#### 🌾 Farming Perks
- **Bountiful Harvest I** (`kingdom:perks/bountiful_harvest_i`): Extra crop drops.
- **Green Thumb** (`kingdom:perks/green_thumb`): Always have Saturation in farmland.
- **Butcher's Blade** (`kingdom:perks/butchers_blade`): +2 damage to animals.
- **Vegan's Grace** (`kingdom:perks/vegans_grace`): Vegetables give more nourishment.
- **Tractor** (`kingdom:perks/tractor`): +20% movement speed on dirt/grass.
- **Shepherd** (`kingdom:perks/shepherd`): Sheep drop extra wool.
- **Lumberjack** (`kingdom:perks/lumberjack`): +20% wood chopping speed.
- **Apple Picker** (`kingdom:perks/apple_picker`): Leaves drop apples more often.
- **Mushroom Forager** (`kingdom:perks/mushroom_forager`): Mushrooms give regeneration.
- **Honey Lover** (`kingdom:perks/honey_lover`): Immune to bee poison.

#### 🎣 Fishing & Ocean Perks
- **Master Angler I** (`kingdom:perks/master_angler_i`): Constant Lure I effect.
- **Sea's Bounty I** (`kingdom:perks/seas_bounty_i`): Constant Luck of the Sea I effect.
- **Deep Sea Diver** (`kingdom:perks/deep_sea_diver`): Water Breathing underwater.
- **Aqua Affinity** (`kingdom:perks/aqua_affinity`): Normal mining speed underwater.
- **Dolphin's Grace** (`kingdom:perks/dolphins_grace`): Swim speed boost.
- **Fisher's Diet** (`kingdom:perks/fishers_diet`): Eating fish gives regeneration.
- **Guardian Slayer** (`kingdom:perks/guardian_slayer`): +20% damage to aquatic mobs.
- **Ocean Treasure** (`kingdom:perks/ocean_treasure`): Bonus XP when swimming.
- **Squid's Ink** (`kingdom:perks/squids_ink`): Immune to blindness.
- **Current Rider** (`kingdom:perks/current_rider`): Move faster in flowing water.

#### ⚔️ Combat Perks
- **Undead Slayer** (`kingdom:perks/undead_slayer`): +20% damage to undead.
- **Arachnid Bane** (`kingdom:perks/arachnid_bane`): +20% damage to spiders.
- **Creeper Resistance** (`kingdom:perks/creeper_resistance`): -20% damage from explosions.
- **Enderman Slayer** (`kingdom:perks/enderman_slayer`): +20% damage to endermen.
- **Royal Guard I** (`kingdom:perks/royal_guard_i`): +2 Armor.
- **Royal Guard II** (`kingdom:perks/royal_guard_ii`): +4 Armor.
- **Royal Vitality I** (`kingdom:perks/royal_vitality_i`): +2 Max Health.
- **Royal Vitality II** (`kingdom:perks/royal_vitality_ii`): +4 Max Health.
- **Swift Striker** (`kingdom:perks/swift_striker`): +10% attack speed.
- **Iron Skin** (`kingdom:perks/iron_skin`): Immune to poison.

#### 💰 Economic & Misc Perks
- **Hero of the Realm I** (`kingdom:perks/hero_of_the_realm_i`): Constant Hero of the Village I.
- **Hero of the Realm II** (`kingdom:perks/hero_of_the_realm_ii`): Constant Hero of the Village II.
- **Swift Messenger** (`kingdom:perks/swift_messenger`): +10% Movement Speed.
- **Night Owl** (`kingdom:perks/night_owl`): +20% Speed at night.
- **Early Bird** (`kingdom:perks/early_bird`): +20% Speed in morning.
- **Enchanter's Wisdom** (`kingdom:perks/enchanters_wisdom`): +20% XP gained.
- **Acrobat** (`kingdom:perks/acrobat`): -50% Fall Damage.
- **Mountaineer** (`kingdom:perks/mountaineer`): Step height increased.
- **Blessed Realm** (`kingdom:perks/blessed_realm`): Passive regeneration.
- **Stout Heart** (`kingdom:perks/stout_heart`): Immune to fear (weakness).

#### ⛓️ Dictator Disadvantages (Debuffs)
- **Heavy Taxes I** (`kingdom:perks/heavy_taxes_i`): -10% XP gained.
- **Heavy Taxes II** (`kingdom:perks/heavy_taxes_ii`): -20% XP gained.
- **Forced Labor I** (`kingdom:perks/forced_labor_i`): Constant Mining Fatigue I.
- **Forced Labor II** (`kingdom:perks/forced_labor_ii`): Constant Mining Fatigue II.
- **Starvation Diet I** (`kingdom:perks/starvation_diet_i`): Hunger drains 10% faster.
- **Starvation Diet II** (`kingdom:perks/starvation_diet_ii`): Hunger drains 20% faster.
- **Oppression I** (`kingdom:perks/oppression_i`): Constant Weakness I.
- **Oppression II** (`kingdom:perks/oppression_ii`): Constant Weakness II.
- **Curfew** (`kingdom:perks/curfew`): Take 20% more damage at night.
- **Disarmed Populace** (`kingdom:perks/disarmed_populace`): -20% Attack Damage.
- **Frail Subjects** (`kingdom:perks/frail_subjects`): -2 Max Health.
- **Fragile Armor** (`kingdom:perks/fragile_armor`): Take 10% more damage.
- **Slowed Masses** (`kingdom:perks/slowed_masses`): Constant Slowness I.
- **Unlucky Realm** (`kingdom:perks/unlucky_realm`): Constant Bad Omen I.
- **Censorship** (`kingdom:perks/censorship`): Cannot use chat.


---

## 🚧 Known Limitations / Roadmap

- Config is loaded **once at startup**; `/kingdom admin reload` does not yet re-read `config.yml`. Restart the server to apply config changes.
- Only the **plurality** voting system is implemented. Ranked-choice and runoff are planned.
- Map marker coordinates (Capital, Election Hall) are currently placeholders. Custom location configuration is planned.
- The Orb of Origin crafting recipe (`data/server/recipes/orb_of_origin.json`) works as a standard Minecraft datapack recipe. Verify your Origins version registers `origins:orb_of_origin`.
