# Commands

Envoys registers a server command tree under `/envoys` and a separate client-side `/envoys`
tree for local script management. Server messages are localized (EN/RU) and colored with `§`
formatting codes.

## The `target` selector

Several subcommands take a `target` argument. A target is either:

- an **NPC UUID**, or
- `aim` — a server-side raycast from the executing player's view, up to **16 blocks**.

## Server commands

Root: `/envoys`. Requires permission level **4 (OP-4)**.

### Maintenance

```text
/envoys cleanup npcs|skins|all
/envoys regen <npc_uuid>
/envoys tp <npc_uuid>
```

### Quests

```text
/envoys quest list
/envoys quest find <text>
/envoys quest delete <quest_uuid>
/envoys quest reset <player> <quest_uuid>
```

### NPC

```text
/envoys npc passport save
/envoys npc passport load
/envoys npc kill <target>
```

### Lua

```text
/envoys lua reload
/envoys lua stop <target>
```

`/envoys lua stop <target>` **disables the Lua engine for that NPC persistently**. The NPC
stays disabled until one of the following occurs:

- `/envoys lua reload`,
- a script upload for that NPC,
- a server restart.

## Client commands

Registered on the client for managing local script files. Local scripts live in
`.minecraft/envoys/local/`.

```text
/envoys lua list
/envoys lua send <target> <file>
/envoys lua pull <target>
```

- **`/envoys lua list`** — list local `.lua` files.
- **`/envoys lua send <target> <file>`** — upload a local file as the NPC's `main.lua` and
  restart its engine.
- **`/envoys lua pull <target>`** — download the NPC's `main.lua` into
  `.minecraft/envoys/local/` as `<npc_name>_<uuid4>.lua`.

---

**Next:** [QUESTS.md →](QUESTS.md)
