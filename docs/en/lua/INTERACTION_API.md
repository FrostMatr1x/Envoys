# Interaction API

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

## Functions

### `envoys.dialogue(message, answers [, player])`

Show the NPC dialog screen and wait for a choice. **Suspending.**

- **Parameters**
  - `message` *(string)* — the NPC line to display.
  - `answers` *(table)* — a map of `{ ["Answer text"] = "choice_key", ... }`.
    Each entry needs a non-empty display text and a non-empty key.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — the chosen `choice_key` *(string)*, or `nil` if the dialog was closed
  without a choice.
- **Notes** — the screen shows up to a few options. `answers` must not be empty.

```lua
local choice = envoys.dialogue("What do you need?", {
    ["I need supplies."] = "supplies",
    ["Just passing through."] = "leave",
})

if choice == "supplies" then
    envoys.say("Here, take these.")
else
    envoys.say("Safe travels.")
end
```

### `envoys.trade(offers [, player])`

Open the trade UI and wait until it is closed. **Suspending.**

- **Parameters**
  - `offers` *(array of tables)* — the offers to list. Each offer supports:
    - `in1` *(string)* — id of the first input item. **Required.**
    - `in1count` *(number, optional)* — default `1`.
    - `in2` *(string, optional)* — id of the second input item.
    - `in2count` *(number, optional)* — default `1`.
    - `out` *(string)* — id of the output item. **Required.**
    - `outcount` *(number, optional)* — default `1`.
    - `priceMultiplier` *(number, optional)* — default `1.0`, clamped to `0.1..10`.
    - `demand` *(number, optional)* — default `0`.
    - `maxTrades` *(number, optional)* — default `-1` (unlimited).
    - `resetTime` *(number, optional)* — seconds until restock; default `-1` (never).
    - `requiredLevel` *(number, optional)* — hides the offer unless
      `envoys.merchant.getUnlocked()` is `>= requiredLevel`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — nothing; resumes when the UI is closed.
- **Notes** — unknown item ids raise an error. If an offer's total price exceeds **64 items**, the
  excess is automatically moved into the second payment slot (`in2`), provided `in2` is empty or
  contains the same item.

```lua
envoys.trade({
    {
        in1 = "minecraft:emerald",
        in1count = 3,
        out = "minecraft:bread",
        outcount = 1,
        maxTrades = 12,
    },
    {
        in1 = "minecraft:emerald",
        in1count = 1,
        out = "minecraft:arrow",
        outcount = 16,
        priceMultiplier = 1.25,
    },
})
```

### `envoys.waitEvent(name [, timeoutTicks])`

Wait for an event by name. **Suspending.**

- **Parameters**
  - `name` *(string)* — one of `"click"`, `"kick"`, `"range"`, `"update"`.
  - `timeoutTicks` *(number, optional)* — maximum wait time in ticks.
- **Returns** — the player proxy for player events, or `nil` on timeout.

```lua
local player = envoys.waitEvent("click", 600)
if player == nil then
    envoys.say("Nobody came.")
end
```

## Checkpoints

Checkpoints store a string per `(NPC, player)` pair and persist across logout and
server restart.

### `envoys.checkpoint.get(key [, player])`

- **Parameters**
  - `key` *(string)* — must match `[a-zA-Z0-9_]{1,64}`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — the stored string, or `nil` if unset.

### `envoys.checkpoint.set(key, value [, player])`

- **Parameters**
  - `key` *(string)* — must match `[a-zA-Z0-9_]{1,64}`.
  - `value` *(string)* — must be 1..256 characters.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — nothing.

```lua
envoys.checkpoint.set("met_before", "true")

if envoys.checkpoint.get("met_before") == "true" then
    envoys.say("Welcome back.")
else
    envoys.say("Nice to meet you.")
end
```

## Merchant

### `envoys.merchant.getUnlocked([player])`

- **Parameters**
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — the number of unlocked trade slots *(int)*.

### `envoys.merchant.addUnlocked([amount [, player]])`

Unlock additional trade slots for this NPC.

- **Parameters**
  - `amount` *(number, optional)* — number of slots to add; default `1`, must be `> 0`.
  - `player` *(player proxy, optional)* — defaults to the current event target player.
- **Returns** — nothing.

```lua
envoys.merchant.addUnlocked(1)
envoys.say("New wares unlocked (" .. envoys.merchant.getUnlocked() .. " slots).")
```

---

**Next:** [QUEST_API.md →](QUEST_API.md)
