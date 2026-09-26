# Quest API

The Lua scripting API is exposed to scripts as the global table `envoys`. A script
lives at `world/envoys/lua/<npc_uuid>/main.lua` and is executed when an event for
that NPC fires.

## Global Context

- The API table is always available as `envoys`; no import is required.
- An optional `player` argument means the **current event's target player** when
  omitted. Passing a player explicitly targets that player instead.
- **Suspending functions** (`wait`, `move`, `waitEvent`, `dialogue`, `trade`) pause
  the event coroutine and return when it is resumed. They **must not** be called
  inside a nested `coroutine.create` / `coroutine.wrap`.
- A per-resume instruction limit is enforced by the config key
  `lua.instructionLimit` (default `50000`). A busy loop errors with
  `too long without yielding`.
- `envoys.command` is gated by the config key `lua.allowCommands` (default `true`)
  and runs with **permission level 4**.

## Quest Id Resolution

Quest functions accept a quest id that may be either a `local_id` or a
`quest_uuid`. Resolution first checks the current NPC, then falls back to the
global quest index. UUID comparison ignores dashes and letter case.

Optional `player` always means the current event target player when omitted.

## Status values

`envoys.quest.status` returns one of the following strings:

- `"NOT_STARTED"`
- `"ACTIVE"`
- `"COMPLETED"`

The three quest types (`KILL`, `ITEM`, `BOOLEAN`) and the player-facing interface are described
conceptually in [Quests](../QUESTS.md). In this reference, the `type` field is always one of those
three strings; only the API contract is documented here.

## Functions

### `envoys.quest.status(id [, player])`

- **Parameters**
  - `id` *(string)* — quest `local_id` or `quest_uuid`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — `"NOT_STARTED"`, `"ACTIVE"`, or `"COMPLETED"`.

```lua
if envoys.quest.status("intro") == "NOT_STARTED" then
    envoys.say("Perhaps you can help me.")
end
```

### `envoys.quest.stage(id [, player])`

- **Parameters**
  - `id` *(string)* — quest `local_id` or `quest_uuid`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — the current count *(int)*: kills for `KILL`, completed steps for
  `BOOLEAN`, items in inventory for `ITEM`.

```lua
local kills = envoys.quest.stage("wolf_hunt")
envoys.say("You have slain " .. kills .. " so far.")
```

### `envoys.quest.progress(id [, player])`

- **Parameters**
  - `id` *(string)* — quest `local_id` or `quest_uuid`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — a table:

  | Field     | Type      | Description                                     |
  |-----------|-----------|-------------------------------------------------|
  | `status`  | string    | One of the status strings above.                |
  | `current` | number    | Current progress value.                         |
  | `target`  | number    | Required progress value.                        |
  | `type`    | string    | `KILL`, `BOOLEAN`, or `ITEM`.                   |
  | `steps`   | table     | Per-step state; present only for `BOOLEAN` quests. |

```lua
local p = envoys.quest.progress("intro")
envoys.say("Progress: " .. p.current .. "/" .. p.target)
```

### `envoys.quest.start(id [, player])`

Give and activate the quest for the player.

- **Parameters**
  - `id` *(string)* — quest `local_id` or `quest_uuid`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — nothing.

```lua
envoys.quest.start("intro")
envoys.say("I have marked the quest in your journal.")
```

### `envoys.quest.advance(id [, amount [, player]])`

Advance quest progress.

- **Parameters**
  - `id` *(string)* — quest `local_id` or `quest_uuid`.
  - `amount` *(number, optional)* — for `KILL`, the number of kills to add
    (default `1`); for `BOOLEAN`, the number of unique steps to add.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — nothing.
- **Notes** — for `ITEM` quests this raises an error; item quests advance by the
  player collecting items.

```lua
envoys.quest.advance("wolf_hunt", 1)
```

### `envoys.quest.complete(id [, player])`

Mark the quest as completed.

- **Parameters**
  - `id` *(string)* — quest `local_id` or `quest_uuid`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — nothing.

```lua
if envoys.quest.status("intro") == "ACTIVE" then
    envoys.quest.complete("intro")
    envoys.say("Well done.")
end
```

## Notes

- **Quest rewards are scripted.** The engine does not grant rewards automatically;
  check the quest status yourself and run the reward logic after completing.
- **Item quests do not consume items on completion.** `envoys.quest.complete()` does not remove the
  items collected for an `ITEM` quest. To take them, run a command from the script, for example:
  `envoys.command("clear " .. player.name .. " <item_id> <count>")`.
- **Quest titles and ids are user data** and are not localized.

```lua
envoys.on("click", function(player)
    local id = "wolf_hunt"
    if envoys.quest.status(id) == "ACTIVE"
        and envoys.quest.stage(id) >= envoys.quest.progress(id).target then
        envoys.quest.complete(id)
        envoys.command("give " .. player.name .. " minecraft:diamond 3")
        envoys.say("Reward granted.")
    end
end)
```

---

**Next:** [gui/USER_GUIDE.md →](../gui/USER_GUIDE.md)
