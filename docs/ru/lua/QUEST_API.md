# API квестов

API Lua-скриптов доступен скриптам как глобальная таблица `envoys`. Скрипт
располагается по пути `world/envoys/lua/<npc_uuid>/main.lua` и выполняется при
срабатывании события для этого NPC.

## Глобальный контекст

- Таблица API всегда доступна как `envoys`; импорт не требуется.
- Необязательный аргумент `player` означает **целевого игрока текущего события**,
  если он опущен. Если передать игрока явно, действие нацелено на него.
- **Приостанавливающие функции** (`wait`, `move`, `waitEvent`, `dialogue`, `trade`)
  останавливают корутину события и возвращают управление при её возобновлении. Их
  **нельзя** вызывать внутри вложенных `coroutine.create` / `coroutine.wrap`.
- Лимит инструкций на одно возобновление задаётся ключом конфигурации
  `lua.instructionLimit` (по умолчанию `50000`). Занятый цикл приводит к ошибке
  `too long without yielding`.
- `envoys.command` управляется ключом конфигурации `lua.allowCommands` (по умолчанию `true`)
  и выполняется с **уровнем прав 4**.

## Разрешение id квеста

Функции квестов принимают id квеста, который может быть либо `local_id`, либо
`quest_uuid`. Разрешение сначала проверяет текущего NPC, затем переходит к
глобальному индексу квестов. Сравнение UUID игнорирует дефисы и регистр букв.

Необязательный `player` всегда означает целевого игрока текущего события, если опущен.

## Значения статуса

`envoys.quest.status` возвращает одну из следующих строк:

- `"NOT_STARTED"`
- `"ACTIVE"`
- `"COMPLETED"`

Три типа квестов (`KILL`, `ITEM`, `BOOLEAN`) и интерфейс для игрока описаны
концептуально (см. [Квесты](../QUESTS.md)). В этом справочнике поле `type` всегда принимает одно
из этих трёх строковых значений; здесь документируется только контракт API.

## Функции

### `envoys.quest.status(id [, player])`

- **Параметры**
  - `id` *(string)* — `local_id` или `quest_uuid` квеста.
  - `player` *(player proxy, необязательно)* — по умолчанию целевой игрок текущего события.
- **Возвращает** — `"NOT_STARTED"`, `"ACTIVE"` или `"COMPLETED"`.

```lua
if envoys.quest.status("intro") == "NOT_STARTED" then
    envoys.say("Perhaps you can help me.")
end
```

### `envoys.quest.stage(id [, player])`

- **Параметры**
  - `id` *(string)* — `local_id` или `quest_uuid` квеста.
  - `player` *(player proxy, необязательно)* — по умолчанию целевой игрок текущего события.
- **Возвращает** — текущий счётчик *(int)*: убийства для `KILL`, выполненные этапы для
  `BOOLEAN`, предметы в инвентаре для `ITEM`.

```lua
local kills = envoys.quest.stage("wolf_hunt")
envoys.say("You have slain " .. kills .. " so far.")
```

### `envoys.quest.progress(id [, player])`

- **Параметры**
  - `id` *(string)* — `local_id` или `quest_uuid` квеста.
  - `player` *(player proxy, необязательно)* — по умолчанию целевой игрок текущего события.
- **Возвращает** — таблицу:

  | Поле      | Тип       | Описание                                        |
  |-----------|-----------|-------------------------------------------------|
  | `status`  | string    | Одна из строк статуса выше.                     |
  | `current` | number    | Текущее значение прогресса.                     |
  | `target`  | number    | Требуемое значение прогресса.                   |
  | `type`    | string    | `KILL`, `BOOLEAN` или `ITEM`.                   |
  | `steps`   | table     | Состояние по каждому этапу; присутствует только для квестов `BOOLEAN`. |

```lua
local p = envoys.quest.progress("intro")
envoys.say("Progress: " .. p.current .. "/" .. p.target)
```

### `envoys.quest.start(id [, player])`

Выдать и активировать квест для игрока.

- **Параметры**
  - `id` *(string)* — `local_id` или `quest_uuid` квеста.
  - `player` *(player proxy, необязательно)* — по умолчанию целевой игрок текущего события.
- **Возвращает** — ничего.

```lua
envoys.quest.start("intro")
envoys.say("I have marked the quest in your journal.")
```

### `envoys.quest.advance(id [, amount [, player]])`

Продвинуть прогресс квеста.

- **Параметры**
  - `id` *(string)* — `local_id` или `quest_uuid` квеста.
  - `amount` *(number, необязательно)* — для `KILL` количество добавляемых убийств
    (по умолчанию `1`); для `BOOLEAN` количество добавляемых уникальных этапов.
  - `player` *(player proxy, необязательно)* — по умолчанию целевой игрок текущего события.
- **Возвращает** — ничего.
- **Примечания** — для квестов `ITEM` вызывает ошибку; квесты на предметы продвигаются
  по мере сбора предметов игроком.

```lua
envoys.quest.advance("cake_truth", 1)
```

### `envoys.quest.complete(id [, player])`

Отметить квест как выполненный.

- **Параметры**
  - `id` *(string)* — `local_id` или `quest_uuid` квеста.
  - `player` *(player proxy, необязательно)* — по умолчанию целевой игрок текущего события.
- **Возвращает** — ничего.

```lua
if envoys.quest.status("intro") == "ACTIVE" then
    envoys.quest.complete("intro")
    envoys.say("Well done.")
end
```

## Примечания

- **Награды за квесты задаются скриптами.** Движок не выдаёт награды автоматически;
  проверяйте статус квеста сами и запускайте логику награды после завершения.
- **Квесты на предметы не расходуют предметы при завершении.** `envoys.quest.complete()` не удаляет
  предметы, собранные для квеста `ITEM`. Чтобы забрать их, выполните команду из скрипта, например:
  `envoys.command("clear " .. player.name .. " <item_id> <count>")`.
- **Названия и id квестов — это пользовательские данные**, они не локализуются.

```lua
envoys.on("click", function(player)
    local id = "cake_truth"
    if envoys.quest.status(id) == "ACTIVE"
        and envoys.quest.stage(id) >= envoys.quest.progress(id).target then
        envoys.quest.complete(id)
        -- Забираем ингредиенты и выдаем обещанную награду
        envoys.command("clear " .. player.name .. " minecraft:sugar 2")
        envoys.command("clear " .. player.name .. " minecraft:egg 1")
        envoys.command("give " .. player.name .. " minecraft:cake 1")
        envoys.say("Протокол тестирования завершён. Как видите, торт вполне настоящий.")
    end
end)
```

---

**Далее:** [gui/USER_GUIDE.md →](../gui/USER_GUIDE.md)
