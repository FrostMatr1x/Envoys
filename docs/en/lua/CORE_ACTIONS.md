# Core Actions

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

### `envoys.say(message [, player])`

Send a chat message to the event's target player.

- **Parameters**
  - `message` *(string)* — the text to send.
  - `player` *(player proxy, optional)* — recipient; defaults to the current event
    target player.
- **Returns** — nothing.
- **Notes** — the message is sent only if the player is online.

```lua
envoys.say("Welcome, traveler.")
```

### `envoys.command(command)`

Run a server command as the NPC.

- **Parameters**
  - `command` *(string)* — the command to run. A leading `/` is optional.
- **Returns** — nothing.
- **Notes** — requires config `lua.allowCommands` (default `true`). Executes with
  **permission level 4** and suppresses command output. Throws if commands are
  disabled.

```lua
envoys.command("time set day")
```

### `envoys.lookAt(target)`

Turn the NPC toward a player proxy or a position.

- **Parameters**
  - `target` *(player proxy | `{x=,y=,z=}`)* — the point to look at.
- **Returns** — nothing.
- **Notes** — for a player proxy the player's current position is resolved at call
  time.

```lua
envoys.lookAt({ x = 10, y = 64, z = -20 })
```

### `envoys.move(x, y, z [, speed])`

Pathfind the NPC to a position. **Suspending.**

- **Parameters**
  - `x`, `y`, `z` *(numbers)* — destination coordinates.
  - `speed` *(number, optional)* — movement speed modifier.
- **Returns** — `true` on arrival, `false` if no path was found or the move was
  cancelled.
- **Notes** — suspends the event coroutine until the NPC arrives or the move ends.

```lua
local arrived = envoys.move(120, 65, 30, 1.0)
if not arrived then
    envoys.say("I cannot reach you.")
end
```

### `envoys.teleport(x, y, z [, yaw, pitch])`

Instantly teleport the NPC. **Does not suspend.**

- **Parameters**
  - `x`, `y`, `z` *(numbers)* — destination coordinates.
  - `yaw` *(number, optional)* — defaults to the NPC's current yaw.
  - `pitch` *(number, optional)* — defaults to `0`.
- **Returns** — nothing.
- **Notes** — stops any active pathfinding.

```lua
envoys.teleport(120, 65, 30)
```

### `envoys.wait(ticks)`

Pause execution for N ticks. **Suspending.**

- **Parameters**
  - `ticks` *(number)* — number of game ticks (20 ticks = 1 second).
- **Returns** — nothing.

```lua
envoys.say("Wait for it...")
envoys.wait(40)
envoys.say("Now!")
```

### `envoys.sound(soundId [, volume, pitch])`

Play a sound at the NPC.

- **Parameters**
  - `soundId` *(string)* — the sound event id, e.g. `"minecraft:entity.villager.yes"`.
  - `volume` *(number, optional)* — defaults to `1.0`.
  - `pitch` *(number, optional)* — defaults to `1.0`.
- **Returns** — nothing.
- **Notes** — an unknown sound id raises an error.

```lua
envoys.sound("minecraft:entity.villager.yes", 1.0, 1.0)
```

### `envoys.particle(particleId, count [, speed, dx, dy, dz])`

Spawn particles at the NPC.

- **Parameters**
  - `particleId` *(string)* — the particle type id.
  - `count` *(number)* — number of particles.
  - `speed` *(number, optional)* — particle speed.
  - `dx`, `dy`, `dz` *(numbers, optional)* — spread offset.
- **Returns** — nothing.

```lua
envoys.particle("minecraft:happy_villager", 10, 0.1, 0.5, 0.5, 0.5)
```

### `envoys.effect(effectId, durationTicks [, amplifier, showParticles])`

Apply a mob effect to the NPC.

- **Parameters**
  - `effectId` *(string)* — the effect id, e.g. `"minecraft:speed"`.
  - `durationTicks` *(number)* — effect duration in ticks.
  - `amplifier` *(number, optional)* — effect amplifier (0 = level I).
  - `showParticles` *(boolean, optional)* — whether particles are shown.
- **Returns** — nothing.

```lua
envoys.effect("minecraft:speed", 200, 1, false)
```

### `envoys.pos()`

Get the NPC's current position.

- **Parameters** — none.
- **Returns** — a table `{x=,y=,z=}`.

```lua
local p = envoys.pos()
envoys.say("I am at " .. p.x .. ", " .. p.y .. ", " .. p.z)
```

### `envoys.name()`

Get the NPC display name.

- **Parameters** — none.
- **Returns** — the NPC display name *(string)*.

```lua
envoys.say("My name is " .. envoys.name())
```

### `envoys.playersInRange(radius)`

Find players near the NPC.

- **Parameters**
  - `radius` *(number)* — search radius in blocks.
- **Returns** — an array of player proxies within the radius.

```lua
for _, p in ipairs(envoys.playersInRange(16)) do
    p:sendMessage("Hello, " .. p.name)
end
```

### `envoys.on(event, function(player) ... end [, arg])`

Register the event callback for this script.

- **Parameters**
  - `event` *(string)* — the event name (see the Events doc).
  - `function(player)` *(function)* — callback invoked when the event fires; `player`
    is the player proxy associated with the event.
  - `arg` *(number, optional)* — event-specific argument: for `update` it is the
    interval in ticks; for `range` it is the radius in blocks.
- **Returns** — nothing.
- **Notes** — the callback runs as a new event coroutine, so it may use suspending
  functions.

```lua
envoys.on("update", function(player)
    player:sendMessage("Still here?")
end, 200)
```

## Player Proxy

A player proxy is returned by event callbacks, `envoys.playersInRange`, and similar
APIs. It is a lightweight handle, not a snapshot.

**Fields**

| Field  | Type            | Description                         |
|--------|-----------------|-------------------------------------|
| `name` | string          | The player's display name.          |
| `uuid` | string          | The player's UUID.                  |
| `pos`  | `{x=,y=,z=}`    | The player's current position.      |

**Methods**

- `sendMessage(msg)` — send a chat message to this player.
- `distanceTo(target)` — distance in blocks to `target`, where `target` is a player
  proxy or a `{x=,y=,z=}` table.

**Notes** — field and method access resolves the online player each time. If the
player is offline, accessing the proxy raises an error.

```lua
envoys.on("click", function(player)
    if player:distanceTo({ x = 0, y = 64, z = 0 }) < 10 then
        player:sendMessage("You are close to the origin.")
    end
end)
```

---

**Next:** [INTERACTION_API.md →](INTERACTION_API.md)
