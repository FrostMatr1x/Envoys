# Quests

Quests give players long-term goals with tracked progress. They can be authored in two ways:

- through the NPC quest menu in-game (the **Quests** button on the NPC settings screen), or
- entirely from a Lua script, using the `envoys.quest.*` API (see
  [Quest API](lua/QUEST_API.md)).

Every quest has two identifiers:

- a **`local_id`** — a short, human-readable identifier that is unique within the owning NPC
  (for example `intro` or `wolf_hunt`);
- a global **`quest_uuid`** — a stable unique id across the whole world, used to reference a
  quest from any script or command.

Scripts and commands accept either form; resolution first checks the current NPC and then
falls back to the global quest index, and UUID comparison ignores dashes and letter case.

Quest titles, ids, and reward logic are **user data** — they are not localized and the engine
never grants rewards by itself. The script decides what a quest gives when it completes.

## Quest types

There are three quest types. They differ only in how progress is measured.

### KILL

Destroy a set number of a specific mob type (identified by its entity id, e.g. `minecraft:zombie`).
Progress counts the qualifying kills credited to the player until the required count is reached.

### ITEM

Collect a set number of a specific item into the player's inventory. Progress is **dynamic**: it
is computed from the items the player is holding right now, so progress rises and falls as items
are gained or lost. Nothing is consumed just by holding the items — see the note in
[Quest API](lua/QUEST_API.md) about consuming items on completion.

### BOOLEAN

A checklist of scripted **steps** (stages) for story/scenario progression. Progress counts the
number of unique steps the script has marked complete. Use this for multi-stage questlines where
the script decides exactly when each stage is done.

## Player-facing interface

- **Quest Wall (journal):** press **J** (default, rebindable in Controls) to open the Quest Wall
  screen, which lists the player's quests and their progress.
- **Pin:** click a quest in the Quest Wall to **pin** it for tracking.
- **HUD overlay:** the pinned quest is displayed in the **top-right corner** of the screen with a
  live progress value that updates as the quest advances.
- **`visible_in_gui`:** a per-quest flag. When disabled, the quest is hidden from the HUD overlay,
  but remains visible in the Quest Wall journal.

## Relationship to scripts

The concept above describes *what a quest is* and *how the player sees it*. The scripting contract
— status values, progress tables, and the functions to start, advance, and complete a quest — lives
in [Quest API](lua/QUEST_API.md), and the visual editor's quest nodes are documented in
[Actions reference](gui/ACTIONS.md).

---

**Next:** [lua/CORE_ACTIONS.md →](lua/CORE_ACTIONS.md)
