# VanishPlusPlusPlus

A Folia-safe Paper vanish plugin with per-player vanish rules, staff visibility controls, fake join/quit messages, and optional integrations for ProtocolLib, LuckPerms, TAB, and Simple Voice Chat.

## Features

- Folia-compatible vanish handling.
- Hide vanished players from normal players while allowing staff to see them.
- Optional fake join and quit messages when players vanish or unvanish.
- Configurable vanished-player behavior, including block breaking, placing, interaction, chat, item pickup, entity hits, mob targeting, potion effects, advancements, and world event triggering.
- Staff-only vanished player list.
- Vanished chat confirmation to prevent accidental public messages.
- Optional silent container opening.
- Optional server list, tab list, tab-complete, and message-command hiding.
- Optional action bar reminder while vanished.
- Optional double-sneak spectator toggle while vanished.
- Persistent per-player vanish state and rule overrides in `data.yml`.

## Requirements

- Paper or Folia-compatible server API for Minecraft `1.21+`.
- Java `25`.

Optional soft dependencies:

- `ProtocolLib` for stronger packet-level hiding.
- `LuckPerms` for permission refresh integration.
- `TAB` for tab-list integration.
- `Simple Voice Chat` for isolating vanished players from voice chat.

The plugin can run without the optional dependencies, but packet-level hiding is best-effort without ProtocolLib.

## Building

```bash
./gradlew build
```

The compiled plugin jar will be created under:

```text
build/libs/
```

For local testing with the Gradle Paper runner:

```bash
./gradlew runServer
```

You can override the runner Minecraft version:

```bash
./gradlew runServer -PrunMinecraftVersion=26.1.2
```

## Installation

1. Build the jar with `./gradlew build`.
2. Copy the generated jar from `build/libs/` into your server's `plugins/` folder.
3. Install any optional dependencies you want to use.
4. Start the server once to generate `plugins/VanishPlusPlusPlus/config.yml`.
5. Edit the config and restart the server.

## Commands

| Command | Aliases | Description |
| --- | --- | --- |
| `/vanish [player]` | `/v` | Toggle vanish for yourself or another player. |
| `/vanishrules [player] <rule\|reset> [true\|false]` | `/vrules`, `/vsettings` | View, change, or reset vanish rules. |
| `/vanishchat confirm` | `/vchat` | Send a pending chat message while vanished. |
| `/vanishpickup [player]` | `/vpickup` | Toggle item pickup while vanished. |
| `/vanishignore` | `/vignore` | Toggle the ProtocolLib missing warning on join. |
| `/vlist` | `/vanishlist` | List currently vanished online players. |

## Permissions

| Permission | Description |
| --- | --- |
| `vanishppp.*` | Grants all VanishPlusPlusPlus permissions. |
| `vanishppp.vanish` | Allows a player to vanish themselves. |
| `vanishppp.vanish.others` | Allows vanishing other players. |
| `vanishppp.see` | Allows seeing vanished players. |
| `vanishppp.tabcompleteandmessage` | Allows tab-completing and messaging vanished players without full see access. |
| `vanishppp.rules` | Allows using `/vanishrules` for yourself. |
| `vanishppp.rules.others` | Allows modifying rules for other players. |
| `vanishppp.chat` | Allows chatting while vanished. |
| `vanishppp.pickup` | Allows picking up items while vanished. |
| `vanishppp.pickup.others` | Allows toggling pickup for other players. |
| `vanishppp.ignorewarning` | Allows using `/vignore`. |
| `vanishppp.list` | Allows using `/vlist`. |

All permissions default to operators.

## Vanish Rules

Rules can be configured globally in `config.yml` under `default-rules`, then changed per player with `/vanishrules`.

Available rule keys:

```text
can_break_blocks
can_place_blocks
can_interact
can_hit_entities
can_pickup_items
can_drop_items
can_chat
can_trigger_world_events
can_be_targeted_by_mobs
can_open_containers_silently
can_use_visible_items
can_receive_potion_effects
broadcast_fake_messages
show_in_tablist_and_playercount
show_in_tab_complete
double_sneak_spectator
force_survival_on_unvanish
block_advancements
```

Examples:

```text
/vrules
/vrules can_break_blocks true
/vrules Steve can_chat true
/vrules Steve reset
```

## Configuration

The main config is generated at:

```text
plugins/VanishPlusPlusPlus/config.yml
```

Important sections:

- `messages`: command and status messages.
- `vanish-appearance`: staff tab prefix, nametag prefix, and vanished action bar.
- `vanish-effects`: real join/quit hiding and fake join/quit broadcasts.
- `invisibility-features`: global hiding and protection features.
- `message-commands`: commands that should treat vanished players as offline for non-staff.
- `hooks.simple-voice-chat`: voice isolation settings.
- `heartbeat.interval-seconds`: how often visibility and permission state refreshes.
- `warnings.protocollib-missing`: whether operators are warned when ProtocolLib is missing.
- `default-rules`: default behavior for vanished players.

Per-player vanish state and rule overrides are saved in:

```text
plugins/VanishPlusPlusPlus/data.yml
```

## Recommended Setup

For a staff vanish setup, grant trusted staff:

```text
vanishppp.vanish
vanishppp.see
vanishppp.list
vanishppp.chat
vanishppp.pickup
vanishppp.rules
```

For administrators who manage other players, also grant:

```text
vanishppp.vanish.others
vanishppp.rules.others
vanishppp.pickup.others
vanishppp.ignorewarning
```

Install ProtocolLib if you want the most complete hiding behavior for tab-complete, server-list, and packet-level visibility.

## Development Notes

This project uses Gradle and the Paper run plugin. The main plugin class is:

```text
org.bacon.vanishPlusPlusPlus.VanishPlusPlusPlus
```

Plugin metadata is defined in:

```text
src/main/resources/plugin.yml
```

Default configuration is defined in:

```text
src/main/resources/config.yml
```
