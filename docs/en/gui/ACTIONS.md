# Visual Script Editor — Action Reference

This is the reference for every node type the Visual Script Editor can produce. Each section describes the node's purpose, its editable fields, and which setting screen edits it.

All linear nodes share a **Next** field (labelled "Далее" / Next) that holds the ID of the next node in the chain; an empty Next ends the chain. Branch nodes instead dispatch to one or more **branches**, each identified by the head node ID.

---

## `start`

**Chain entry anchor.**

- Purpose: the entry point of an event's chain. Execution always begins here when the event fires.
- Fields: **Next** ID.
- Cannot be deleted.

---

## `say` — Chat message

Sends a chat message.

- Fields:
  - **Message text**.
  - **Global** flag (whether the message is broadcast globally rather than sent to the event player).
  - **Next** ID.

---

## `wait` — Delay

Pauses the chain for a duration.

- Fields:
  - **Duration** value.
  - **Unit**: ticks / seconds / minutes / hours.
  - **Next** ID.

---

## `move`

Moves the NPC to a target position.

- Fields:
  - **Target X / Y / Z**.
  - **Next** ID.

---

## `command`

Runs one or more server commands.

- Fields:
  - A **list of server commands**, added one by one.
  - **Next** ID.

---

## `lookAt`

Makes the NPC look at a target.

- Fields:
  - **Target mode**:
    - **"event player"** — looks at the player who triggered the event; no coordinates are used.
    - **"coordinates"** — uses explicit **X / Y / Z**.
  - **Next** ID.

---

## `trade`

Opens a trade offer with the player.

- Fields:
  - A **list of offers**. Each offer contains:
    - **Input 1** (item, with optional **count**).
    - Optional **Input 2** (item, with optional **count**).
    - **Output** (item, with optional **count**).
    - **`priceMultiplier`**.
    - **`demand`**.
    - **`maxTrades`**.
    - **`resetTime`** (in seconds).
    - Optional **`requiredLevel`**.
  - **Next** ID.

---

## Branch nodes

Branch nodes do not have a single Next pointer. Instead they select one of several **branches**, each pointing to a head node ID.

### `dialogue_choice` — Dialogue

Shows an NPC phrase with answer options.

- Fields:
  - **NPC phrase text**.
  - **Answer options** — each option has a **key** and a **branch** (the head ID to jump to).
- The selected option's **key** is returned to the script, so the continuation can react to which answer the player picked.

### `random_choice` — Randomizer

Picks one of **N** branches randomly.

- Fields: **N branches**, selected with `math.random(N)`.

### `quest_check` — Quest check

Branches based on quest completion state.

- Fields:
  - **Quest id**.
  - Exactly **two branches**: **completed** and **not completed**.

---

## Quest actions

### `quest_start` — Give quest

- Fields: **Quest id**, **Next** ID.

### `quest_advance` — Advance step

- Fields: **Quest id**, **amount**, **Next** ID.

### `quest_complete` — Complete quest

- Fields: **Quest id**, **Next** ID.

---

## `savepoint` — Save point

Stores a per-player checkpoint that a `loadpoint` can later return to.

- Fields:
  - **Key** — restricted to `[a-zA-Z0-9_]`.
  - **Internal UUID** — auto-generated and read-only.
  - **"Abort after save"** toggle.
  - **Next** ID.

When the chain reaches a save point it saves a checkpoint for the player. Depending on the abort toggle the chain either continues or aborts. Either way, the saved continuation is reachable from a matching `loadpoint`.

---

## `loadpoint` — Load point

Returns the player to a previously saved checkpoint.

- Fields:
  - **Key** — the key of the save point to return to.
  - **Next** ID — used as a **fallback** when the player has no checkpoint yet.

---

## Branches, options and validation

Inside a **branch editor** you edit the branches of an option — that is, the **head IDs** each option jumps to. An option's **text** is what the player sees; the option's **key** is what the script receives when it is chosen. Keep both consistent with how your continuation logic reads the returned key.

Validation rules enforced at compile time:

- Node **IDs must be unique**.
- Every **Next** value must reference a **valid node id**.
- Branch heads must reference valid node ids.

If any of these are violated, **compilation reports an error** instead of producing Lua.

---

**Next:** [INTERNALS_AND_SAFETY.md →](INTERNALS_AND_SAFETY.md)
