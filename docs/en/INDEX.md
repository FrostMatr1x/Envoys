Change language? EN, [RU](../ru/INDEX.md)

# Envoys — Documentation

Envoys is a NeoForge 1.21.1 mod that adds fully configurable NPCs. Each NPC can be given a
passport (name, size, speed, health, visibility and behaviour flags), a custom skin, a trade list, and a quest
line. On top of that, Envoys ships a Lua scripting engine and a visual graph editor, so NPC
logic can be written either as code (`main.lua`) or as a node graph that compiles down to the
same Lua.

The mod's capabilities:

- NPC passports, skins, trades, and quests stored in the world save.
- A sandboxed Lua engine with a script per NPC and an event-driven API.
- Actions that suspend a script (`wait`, `move`, `dialogue`, `trade`, `waitEvent`).
- A visual editor that compiles graphs to Lua and decompiles Lua back to graphs.
- Network transport for uploading/downloading NPC scripts.
- Items **NPC Tuner** and **NPC Spawn Egg**, grouped in the **Envoys** creative tab; the NPC entity is named **NPC**.

## How to read this documentation

**Section 1 is required reading to understand the mod's logic; Sections 2 and 3 are optional
and can be studied in any order.** Events, commands, and quests define the contract every script
and every operator relies on; the Lua API and the visual editor are built on top of that contract.

## Table of contents

### Section 1 — Events, Commands & Quests (required)

- [Events](EVENTS.md)
- [Commands](COMMANDS.md)
- [Quests](QUESTS.md)

### Section 2 — Lua API (optional)

- [Core actions](lua/CORE_ACTIONS.md)
- [Interaction API](lua/INTERACTION_API.md)
- [Quest API](lua/QUEST_API.md)

### Section 3 — Visual Editor (optional)

- [User guide](gui/USER_GUIDE.md)
- [Actions reference](gui/ACTIONS.md)
- [Internals & safety](gui/INTERNALS_AND_SAFETY.md)

## Concept

NPC configuration is stored in the world save, so a world carries its own NPCs with no extra
files to copy around. Lua scripts live at `world/envoys/lua/<npc_uuid>/main.lua` — exactly one
script per NPC. The visual editor compiles graphs to Lua and decompiles Lua back into graphs,
so both authoring modes edit the same source of truth.

---

**Next:** [EVENTS.md →](EVENTS.md)
