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
/envoys npc resetpoints <target> <branch>
```

- **`/envoys npc resetpoints <target> <branch>`** — сбрасывает сохранённые чекпоинты
  **всех онлайн-игроков** для указанного NPC и ветки. `target` — UUID или `aim`, а
  `branch` — ключ точки сохранения (`savepoint`). Аргумент `branch` обязателен;
  автодополнение предлагает ключи из `savepoint` и уже сохранённые чекпоинты. Если ни у
  одного онлайн-игрока нет чекпоинта для этой ветки, команда сообщает об этом.

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

### Команды из NPC: ограничения безопасности

Команды, выполняемые от имени NPC через `envoys.command(...)` в Lua и через действие
`command` визуального редактора, проходят через фильтр безопасности. Запрещены:

- `op`, `deop`;
- `ban`, `ban-ip`, `banlist`, `pardon`, `pardon-ip`;
- `whitelist`;
- `stop`, `reload`, `datapack`;
- `function`, `schedule`;
- `save-all`, `save-off`, `save-on`;
- `setidletimeout`, `transfer`, `publish`;
- `debug`, `jfr`, `perf`.

Кроме того, внутри `/execute ... run` запрещены побочные эффекты `summon` и `store`.
Команда длиннее **2048** символов или не проходящая полный парсинг отклоняется.
Заблокированная команда не выполняется, а сервер пишет предупреждение в лог.

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
  перезапустить его движок. Требует **OP-4** на сервере; размер файла — не более **1 МБ**.
- **`/envoys lua pull <target>`** — выгрузить `main.lua` NPC в
  `.minecraft/envoys/local/` как `<npc_name>_<uuid4>.lua`. Требует **OP-4** на сервере.

### Анимации

Клиентские команды для управления файлами анимаций. Локальные анимации хранятся в
`.minecraft/envoys/anim/` в виде `.json` файлов; серверные анимации — в `world/envoys/anim/`.

```text
/envoys anim reload
/envoys anim push [имя]
/envoys anim delete <имя>
```

- **`/envoys anim reload`** — пересканировать локальную папку анимаций
  (`.minecraft/envoys/anim/`) и обновить клиентский кэш.
- **`/envoys anim push [имя]`** — выгрузить локальную анимацию на сервер. Без аргумента
  отправляются все локальные анимации, отличающиеся от серверных копий (по SHA-256); с
  именем — только указанный файл. Требуется валидный JSON; размер не более **2 МБ**.
- **`/envoys anim delete <имя>`** — удалить анимацию с сервера. Все подключённые
  игроки получают уведомление и сбрасывают свои кэшированные копии.

Загрузка и удаление анимаций требуют **уровень прав 4 (OP-4)** на сервере.
Полученные файлы валидируются (JSON-синтаксис, размер, безопасность пути) и
сохраняются в `world/envoys/anim/`. Клиенты кэшируют их в
`.minecraft/envoysCache/<id_сервера>/anim/`.

---

**Далее:** [QUESTS.md →](QUESTS.md)
