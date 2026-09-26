# Visual Script Editor — Internals and Safety

This document describes how the Visual Script Editor stores and reconstructs graphs, how it protects hand-written Lua, how emergency backups work, and how save/load points are compiled.

---

## Safe Guard

The decompiler only recognizes the exact structure the editor itself generates. Any **external Lua code** inside an event — any line that is not part of the generated scaffold — marks that event as **⚪️ locked**.

A locked event:

- Disables visual editing for that event.
- Keeps its source **verbatim** when the script is written back.

This prevents the editor from destroying hand-written code that a mod author added outside the GUI.

---

## Meta markers

The editor round-trips between the graph and Lua using `-- @gui:...` comment markers embedded in the generated script. These markers describe the events, actions, branches and list data so the decompiler can rebuild the graph.

### Event markers

```lua
-- @gui:event:start name=click [arg=N] [enabled=false]
...
-- @gui:event:end name=click
```

### Action markers

```lua
-- @gui:action:start id=.. type=.. next=.. <params>
-- @gui:action:end id=..
```

### Branch markers

```lua
-- @gui:branch:start id=.. type=.. options="..",".." keys="..",".."
-- @gui:branch:end id=..
```

### List sub-markers

List data such as commands and trade offers uses dedicated sub-markers:

```lua
-- @gui:command:line text=".."
-- @gui:trade:item ...
```

### Disabled events

A disabled event comments out **every executable line** with a leading `-- `, while the `@gui:` markers are left readable. As a result the event never executes, but it remains fully editable in the GUI.

### Mixing GUI and a text editor

You can edit the generated Lua in a text editor and still use the GUI, as long as you **keep the markers intact**. Breaking or removing a marker causes the event to be treated as external code and therefore **locks** it.

---

## Emergency backups (`.temp.lua`)

On every modification the editor writes a backup to:

```
.minecraft/envoys/local/.temp/<npc_uuid>.temp.lua
```

When a `.temp` file exists and you open the editor, a restore dialog appears:

> **Unsaved changes** — Yes, restore / No, start over.

- **Yes** restores the backup.
- **No** starts over and discards it.

The backup is deleted after a successful **Process scenario** (whether you chose send or local save), and it is flushed on logout.

---

## Save point code generation

Save points and load points compile into a checkpoint-based dispatch scheme:

- A **non-exit save point** emits `envoys.checkpoint.set(...)`, then performs a **tail call into a per-save-point segment**.
- An **exit save point** emits `envoys.checkpoint.set(...)`, then `return`.
- A **load point** reads `envoys.checkpoint.get(key)` and **dispatches to the matching checkpoint's segment**, falling through to the **fallback Next** when nothing matches.

### Validation

A validation error is reported for loops that contain **no waiting action**. Such loops would never yield and would hit the instruction limit, so the compiler rejects them instead of emitting a script that hangs.

---

**Back to:** [Index →](../INDEX.md)
