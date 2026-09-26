# Events

An NPC is driven by events. Every NPC runs **one** script, `main.lua`, registered from the
world save at `world/envoys/lua/<npc_uuid>/main.lua`. Inside that script you subscribe to
events with the global `envoys` table:

```lua
envoys.on(event, fn[, arg])
```

There are exactly four event types: `click`, `update`, `range`, and `kick`.

**Only one callback per event exists.** Calling `envoys.on` again with the same event name
*replaces* the previously registered handler; subscriptions do not stack.

Actions (`wait`, `move`, `dialogue`, `trade`, `waitEvent`) can suspend the event coroutine.
While suspended, the NPC keeps running other events; the script resumes where it left off.

## click

**Purpose:** React to a player interacting with the NPC (right-click, `mobInteract`).

**Player context:** the interacting player.

**Parameters:** none.

```lua
envoys.on("click", function(player)
    player:sendMessage("Hello!")
end)
```

## update

**Purpose:** Periodic background tick for NPC life — wandering, ambient logic, internal
state changes.

**Player context:** **none.** `player` is `nil`; `update` runs without a player attached.

**Parameters:** the third argument is the interval in ticks. It is **optional** and defaults to
**1 tick** when omitted.

```lua
envoys.on("update", function(player)
    -- player is nil here
end, INTERVAL_TICKS)
```

**Restriction:** actions that require a player are **forbidden** in `update`:
`dialogue`, `trade`, `lookAt`, quest actions, and `savepoint`/`loadpoint`. Use `click` or
`range` when a player must be present.

## range

**Purpose:** React when a player enters the NPC's radius.

**Player context:** the player who entered the radius.

**Parameters:** the third argument is the radius in blocks.

```lua
envoys.on("range", function(player)
    -- fired once when the player crosses the boundary inward
end, RADIUS_BLOCKS)
```

The event fires on the radius boundary crossing (entering), not continuously while the player
stays inside.

## kick

**Purpose:** React when the NPC takes damage.

**Player context:** the attacking player, which **may be `nil`** (for example environmental or
non-player damage).

**Parameters:** none.

```lua
envoys.on("kick", function(player)
    -- player may be nil
end)
```

---

**Next:** [COMMANDS.md →](COMMANDS.md)
