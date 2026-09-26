# Visual Script Editor — User Guide

The Visual Script Editor is the in-game GUI used to build NPC scenarios for the **Envoys** mod without writing Lua by hand. It turns a graph of nodes into the generated `main.lua` script that drives the NPC's behaviour, and it can round-trip that script back into an editable graph.

This guide covers the scenario-creation workflow: how chains work, how to open the editor, what the event states mean, and how the "Process scenario" flow saves your work.

---

## Graph chains

Every event has a **chain** of nodes. A chain always begins at the special `start` node, which is the entry anchor for that event.

- Each node has a numeric **ID**.
- Each node has a **Next** field (labelled "Далее" / Next) that holds the ID of the next node in the chain.
- An empty **Next** field means the node is the **end of the chain**.

When the event fires, execution walks the chain from `start`, following each node's Next pointer until it reaches a node with no Next, or until a branch node redirects execution to another head node.

Nodes are edited in the **"Visual Script"** screen, which is opened from the NPC settings.

---

## Opening the editor

When you open the Visual Script screen:

1. The client fetches the NPC's `main.lua` from the server.
2. The script is **decompiled** into a graph.
3. The screen shows the four events that the script can contain.

If the file contains code that was not produced by the editor, the affected event is protected by the Safe Guard (see [INTERNALS_AND_SAFETY.md](INTERNALS_AND_SAFETY.md)) rather than being rewritten.

---

## Event states

Each event row shows a status indicator:

| State | Indicator | Meaning |
| --- | --- | --- |
| Synced / saved | 🟢 green | The on-screen graph matches the saved script. |
| Unsaved edits | 🟡 yellow | The graph has been modified and differs from the saved script. |
| Locked (external Lua) | ⚪️ grey | Non-GUI Lua code was detected in this event. Visual editing is disabled and the **Configure** button is unavailable for this event. |

The **Configure** button is disabled only for ⚪️ grey (locked) events.

---

## The "Process scenario" button

The **Process scenario** button is enabled only when there are modifications — that is, when at least one event is 🟡 yellow.

When you press it, the editor:

1. Compiles the graph into Lua.
2. Pre-validates the generated Lua for syntax errors.
3. Shows a modal with three save modes.

### Save modes

- **Send and restart** — writes the NPC's `main.lua` to the server and restarts the NPC's Lua engine so the new behaviour takes effect immediately.
- **Send only** — writes `main.lua` to the server without restarting the engine. The new script takes effect the next time the engine starts.
- **Local only** — saves the file on the client under `.minecraft/envoys/local/` without touching the server.

After a successful send or local save, the editor removes the `.temp` backup and the affected events become 🟢 green.

---

## Configuring an event

Besides the graph itself, each event has a configuration screen where you can:

- **Enable / disable** the event.
- Set the event's **argument** — for example the **update interval in ticks**, or the **range radius in blocks**, depending on the event.

A disabled event keeps its chain intact, but every executable line of the generated Lua is **emitted commented-out**, so the event never runs while remaining fully editable.

---

**Next:** [ACTIONS.md →](ACTIONS.md)
