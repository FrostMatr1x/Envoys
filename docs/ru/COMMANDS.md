# Команды

Envoys регистрирует серверное дерево команд под `/envoys` и отдельное клиентское дерево
`/envoys` для локального управления скриптами. Серверные сообщения локализованы (EN/RU) и
окрашены кодами форматирования `§`.

## Селектор `target`

Несколько подкоманд принимают аргумент `target`. Цель — это либо:

- **UUID NPC**, либо
- `aim` — серверный рейкаст из направления взгляда выполняющего команду игрока, вплоть до
  **16 блоков**.

## Серверные команды

Корень: `/envoys`. Требуется уровень прав **4 (OP-4)**.

### Обслуживание

```text
/envoys cleanup npcs|skins|all
/envoys regen <npc_uuid>
/envoys tp <npc_uuid>
```

### Квесты

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

`/envoys lua stop <target>` **отключает движок Lua для этого NPC**. NPC остаётся
отключённым, пока не произойдёт одно из следующего:

- `/envoys lua reload`,
- загрузка скрипта для этого NPC,
- перезапуск сервера.

## Клиентские команды

Регистрируются на клиенте для управления локальными файлами скриптов. Локальные скрипты лежат в
`.minecraft/envoys/local/`.

```text
/envoys lua list
/envoys lua send <target> <file>
/envoys lua pull <target>
```

- **`/envoys lua list`** — вывести список локальных `.lua`-файлов.
- **`/envoys lua send <target> <file>`** — загрузить локальный файл как `main.lua` NPC и
  перезапустить его движок.
- **`/envoys lua pull <target>`** — выгрузить `main.lua` NPC в
  `.minecraft/envoys/local/` как `<npc_name>_<uuid4>.lua`.

---

**Далее:** [QUESTS.md →](QUESTS.md)
