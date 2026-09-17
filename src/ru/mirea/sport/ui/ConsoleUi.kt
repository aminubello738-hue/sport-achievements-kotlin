package ru.mirea.sport.ui

import ru.mirea.sport.json.JsonException
import ru.mirea.sport.model.Achievement
import ru.mirea.sport.model.CompetitionLevel
import ru.mirea.sport.model.Institute
import ru.mirea.sport.model.SportKind
import ru.mirea.sport.service.AchievementService
import ru.mirea.sport.service.SortCriterion
import java.io.IOException
import java.time.format.DateTimeFormatter

/**
 * Пользовательский интерфейс приложения, реализованный в виде
 * циклического текстового меню.
 *
 * Класс отвечает исключительно за взаимодействие с пользователем:
 * вывод данных на экран и вызов операций сервисного слоя. Бизнес-логика
 * в этом классе не реализуется.
 *
 * @property service сервис бизнес-логики
 * @property reader компонент ввода данных с проверкой корректности
 */
class ConsoleUi(
    private val service: AchievementService,
    private val reader: InputReader
) {

    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Описание колонок таблицы вывода: заголовок и ширина поля. */
    private val columns = listOf(
        "ID" to 3,
        "Спортсмен" to 22,
        "Группа" to 10,
        "Вид спорта" to 15,
        "Соревнование" to 24,
        "Уровень" to 15,
        "Место" to 5,
        "Балл" to 5
    )

    private val topRule: String =
        columns.joinToString("\u252c", prefix = "\u250c", postfix = "\u2510") {
            "\u2500".repeat(it.second + 2)
        }

    private val midRule: String =
        columns.joinToString("\u253c", prefix = "\u251c", postfix = "\u2524") {
            "\u2500".repeat(it.second + 2)
        }

    private val bottomRule: String =
        columns.joinToString("\u2534", prefix = "\u2514", postfix = "\u2518") {
            "\u2500".repeat(it.second + 2)
        }

    private val width: Int = topRule.length

    /**
     * Запускает главный цикл приложения: загружает данные из файла,
     * отображает меню и обрабатывает выбор пользователя до команды выхода.
     */
    fun run() {
        printHeader()
        loadData()
        try {
            mainLoop()
        } catch (e: EndOfInputException) {
            println()
            println("Входной поток завершён. Работа приложения прекращена.")
        }
    }

    private fun mainLoop() {
        while (true) {
            printMenu()
            when (reader.readInt("Выберите пункт меню: ", 0..9)) {
                1 -> showAll()
                2 -> addAchievement()
                3 -> editAchievement()
                4 -> deleteAchievement()
                5 -> searchAchievements()
                6 -> sortAchievements()
                7 -> showStatistics()
                8 -> saveData()
                9 -> reloadData()
                0 -> if (exit()) return
            }
        }
    }

    // ------------------------------------------------------------------
    // Обработчики пунктов меню
    // ------------------------------------------------------------------

    private fun showAll() {
        printTitle("Реестр спортивных достижений")
        printTable(service.all())
    }

    private fun addAchievement() {
        printTitle("Регистрация нового достижения")
        val athlete = reader.readText("Фамилия и инициалы спортсмена: ", maxLength = 40)

        println("Институт: ${Institute.menuHint()}")
        val institute = Institute.fromMenuIndex(
            reader.readInt("Номер института: ", 1..Institute.entries.size)
        )!!

        val group = reader.readText("Учебная группа: ", maxLength = 15)

        println("Вид спорта: ${SportKind.menuHint()}")
        val sport = SportKind.fromMenuIndex(
            reader.readInt("Номер вида спорта: ", 1..SportKind.entries.size)
        )!!

        val competition = reader.readText("Наименование соревнования: ", maxLength = 60)

        println("Уровень соревнования: ${CompetitionLevel.menuHint()}")
        val level = CompetitionLevel.fromMenuIndex(
            reader.readInt("Номер уровня: ", 1..CompetitionLevel.entries.size)
        )!!

        val participants = reader.readInt("Количество участников: ", 1..10_000)
        val place = reader.readInt("Занятое место: ", 1..participants)
        val date = reader.readDate("Дата соревнования (ГГГГ-ММ-ДД): ")
        val coach = reader.readText("Фамилия и инициалы тренера: ", maxLength = 40)

        val created = service.add(
            athlete, institute, group, sport, competition,
            level, place, participants, date, coach
        )
        println()
        println("Достижение зарегистрировано, присвоен идентификатор ${created.id}.")
        println("Начислено рейтинговых баллов: ${formatPoints(created.ratingPoints)}")
    }

    private fun editAchievement() {
        printTitle("Редактирование достижения")
        if (isEmptyCollection()) return
        val id = reader.readInt("Введите идентификатор записи: ", 1..Int.MAX_VALUE)
        val item = service.findById(id)
        if (item == null) {
            println("Запись с идентификатором $id не найдена.")
            return
        }
        printCard(item)
        println("Оставьте поле пустым, чтобы сохранить текущее значение.")

        reader.readOptional("Спортсмен [${item.athlete}]: ")?.let { item.athlete = it }
        reader.readOptional("Учебная группа [${item.studyGroup}]: ")?.let { item.studyGroup = it }
        reader.readOptional("Соревнование [${item.competition}]: ")?.let { item.competition = it }

        println("Уровень соревнования: ${CompetitionLevel.menuHint()}")
        reader.readOptional("Номер уровня [${item.level.title}]: ")?.let { input ->
            val level = input.toIntOrNull()?.let { CompetitionLevel.fromMenuIndex(it) }
            if (level == null) {
                printInlineError("некорректный номер уровня, значение не изменено")
            } else {
                item.level = level
            }
        }

        reader.readOptional("Занятое место [${item.place}]: ")?.let { input ->
            val place = input.toIntOrNull()
            if (place == null || place !in 1..item.participants) {
                printInlineError(
                    "ожидалось число от 1 до ${item.participants}, значение не изменено"
                )
            } else {
                item.place = place
            }
        }

        reader.readOptional("Тренер [${item.coach}]: ")?.let { item.coach = it }

        service.markModified()
        println()
        println("Изменения применены:")
        printCard(item)
    }

    private fun deleteAchievement() {
        printTitle("Удаление достижения")
        if (isEmptyCollection()) return
        val id = reader.readInt("Введите идентификатор записи: ", 1..Int.MAX_VALUE)
        val item = service.findById(id)
        if (item == null) {
            println("Запись с идентификатором $id не найдена.")
            return
        }
        printCard(item)
        if (reader.confirm("Удалить указанную запись?")) {
            service.remove(id)
            println("Запись с идентификатором $id удалена.")
        } else {
            println("Удаление отменено, данные не изменены.")
        }
    }

    private fun searchAchievements() {
        printTitle("Поиск достижений")
        if (isEmptyCollection()) return
        val query = reader.readText(
            "Введите фрагмент фамилии, группы, соревнования или вида спорта: "
        )
        val found = service.search(query)
        println()
        if (found.isEmpty()) {
            println("По запросу «$query» ничего не найдено.")
        } else {
            println("Найдено записей: ${found.size}")
            printTable(found)
        }
    }

    private fun sortAchievements() {
        printTitle("Сортировка достижений")
        if (isEmptyCollection()) return
        SortCriterion.entries.forEachIndexed { index, criterion ->
            println("  ${index + 1}. ${criterion.title}")
        }
        val criterion = SortCriterion.fromMenuIndex(
            reader.readInt("Выберите критерий сортировки: ", 1..SortCriterion.entries.size)
        )!!
        println()
        println("Результат сортировки ${criterion.title}:")
        printTable(service.sorted(criterion))
    }

    private fun showStatistics() {
        printTitle("Агрегированные показатели")
        if (isEmptyCollection()) return
        val stats = service.statistics()
        println("  Всего зарегистрировано достижений .... ${stats.total}")
        println("  Уникальных спортсменов ............... ${stats.athletes}")
        println("  Призовых мест ........................ ${stats.prizeCount} " +
            "(${"%.1f".format(stats.prizeShare)} %)")
        println("  Из них первых мест ................... ${stats.goldCount}")
        println("  Среднее занятое место ................ ${"%.2f".format(stats.averagePlace)}")
        println("  Суммарный рейтинговый балл ........... ${formatPoints(stats.totalRating)}")
        println("  Средний рейтинговый балл ............. ${formatPoints(stats.averageRating)}")
        println("  Лучший результат ..................... ${describe(stats.bestAchievement)}")
        println("  Минимальный результат ................ ${describe(stats.worstAchievement)}")

        println()
        println("  Распределение достижений по видам спорта:")
        stats.countBySport.entries
            .sortedByDescending { it.value }
            .forEach { (sport, count) ->
                println("    ${sport.title.padEnd(18)} ${"#".repeat(count)} ($count)")
            }

        println()
        println("  Распределение по уровням соревнований:")
        CompetitionLevel.entries.forEach { level ->
            val count = stats.countByLevel[level] ?: 0
            if (count > 0) {
                println("    ${level.title.padEnd(18)} ${"#".repeat(count)} ($count)")
            }
        }

        println()
        println("  Рейтинг институтов по сумме баллов:")
        stats.ratingByInstitute.entries
            .sortedByDescending { it.value }
            .forEachIndexed { index, (institute, points) ->
                println(
                    "    ${index + 1}. ${institute.shortName.padEnd(6)} " +
                        "${formatPoints(points).padStart(8)} балла(ов)"
                )
            }
    }

    private fun saveData() {
        printTitle("Сохранение данных")
        try {
            service.save()
            println("Данные сохранены в файл: ${service.dataFilePath()}")
            println("Количество сохранённых записей: ${service.count()}")
        } catch (e: IOException) {
            println("Не удалось сохранить данные: ${e.message}")
        }
    }

    private fun reloadData() {
        printTitle("Перезагрузка данных из файла")
        if (service.hasUnsavedChanges &&
            !reader.confirm("Несохранённые изменения будут потеряны. Продолжить?")
        ) {
            println("Операция отменена.")
            return
        }
        loadData()
    }

    private fun exit(): Boolean {
        if (service.hasUnsavedChanges &&
            reader.confirm("Имеются несохранённые изменения. Сохранить перед выходом?")
        ) {
            saveData()
        }
        println()
        println("Работа приложения завершена.")
        return true
    }

    // ------------------------------------------------------------------
    // Вспомогательные методы
    // ------------------------------------------------------------------

    private fun loadData() {
        try {
            val count = service.load()
            if (count == 0) {
                println("Файл данных пуст или не найден. Создан пустой реестр достижений.")
            } else {
                println("Загружено записей из файла данных: $count")
            }
            println("Файл данных: ${service.dataFilePath()}")
        } catch (e: JsonException) {
            println("Ошибка структуры файла данных: ${e.message}")
            println("Приложение продолжит работу с пустым реестром достижений.")
        } catch (e: IOException) {
            println("Ошибка чтения файла данных: ${e.message}")
            println("Приложение продолжит работу с пустым реестром достижений.")
        }
    }

    private fun isEmptyCollection(): Boolean {
        if (service.count() == 0) {
            println("Реестр достижений пуст. Добавьте хотя бы одну запись.")
            return true
        }
        return false
    }

    private fun printHeader() {
        val inner = MENU_WIDTH - 2
        println("\u2554" + "\u2550".repeat(inner) + "\u2557")
        println("\u2551" + boxed("СИСТЕМА УЧЁТА СПОРТИВНЫХ ДОСТИЖЕНИЙ В ВУЗЕ", inner) + "\u2551")
        println("\u2551" + boxed("Курсовая работа по дисциплине", inner) + "\u2551")
        println("\u2551" + boxed("«Программирование на языке Котлин»", inner) + "\u2551")
        println("\u255a" + "\u2550".repeat(inner) + "\u255d")
    }

    private fun printMenu() {
        val inner = MENU_WIDTH - 2
        val items = listOf(
            "1" to "Показать все достижения",
            "2" to "Зарегистрировать достижение",
            "3" to "Редактировать достижение",
            "4" to "Удалить достижение",
            "5" to "Найти достижения",
            "6" to "Сортировать достижения",
            "7" to "Показать статистику",
            "8" to "Сохранить данные в файл",
            "9" to "Перезагрузить данные из файла"
        )
        println()
        println("\u250c" + "\u2500".repeat(inner) + "\u2510")
        println("\u2502" + boxed("ГЛАВНОЕ МЕНЮ", inner) + "\u2502")
        if (service.hasUnsavedChanges) {
            println("\u2502" + boxed("(есть несохранённые изменения)", inner) + "\u2502")
        }
        println("\u251c" + "\u2500".repeat(inner) + "\u2524")
        items.forEach { (key, title) ->
            println("\u2502" + menuLine(key, title, inner) + "\u2502")
        }
        println("\u251c" + "\u2500".repeat(inner) + "\u2524")
        println("\u2502" + menuLine("0", "Выход", inner) + "\u2502")
        println("\u2514" + "\u2500".repeat(inner) + "\u2518")
    }

    /** Форматирует пункт меню: номер, вертикальная черта, наименование. */
    private fun menuLine(key: String, title: String, inner: Int): String {
        val text = "  " + key.padStart(1) + " \u2551 " + title
        return text.padEnd(inner)
    }

    /** Центрирует текст в поле заданной ширины. */
    private fun boxed(text: String, inner: Int): String {
        val left = (inner - text.length) / 2
        return " ".repeat(maxOf(left, 0)) + text +
            " ".repeat(maxOf(inner - left - text.length, 0))
    }

    private fun printTitle(title: String) {
        val caption = " " + title.uppercase() + " "
        println()
        println("\u2500\u2500" + caption + "\u2500".repeat(maxOf(width - caption.length - 2, 0)))
    }

    private fun printTable(items: List<Achievement>) {
        if (items.isEmpty()) {
            println("Записи отсутствуют.")
            return
        }
        println(topRule)
        println(
            columns.joinToString("\u2502", prefix = "\u2502", postfix = "\u2502") {
                " " + it.first.padEnd(it.second) + " "
            }
        )
        println(midRule)
        items.forEach { item ->
            val cells = listOf(
                item.id.toString(),
                cut(item.athlete, columns[1].second),
                cut(item.studyGroup, columns[2].second),
                cut(item.sport.title, columns[3].second),
                cut(item.competition, columns[4].second),
                cut(item.level.title, columns[5].second),
                item.place.toString(),
                formatPoints(item.ratingPoints)
            )
            println(
                cells.mapIndexed { i, cell -> " " + cell.padEnd(columns[i].second) + " " }
                    .joinToString("\u2502", prefix = "\u2502", postfix = "\u2502")
            )
        }
        println(bottomRule)
        println("Всего записей: ${items.size}")
    }

    private fun printCard(item: Achievement) {
        println()
        println("  Идентификатор ............ ${item.id}")
        println("  Спортсмен ................ ${item.athlete}")
        println("  Институт ................. ${item.institute.shortName} " +
            "(${item.institute.title})")
        println("  Учебная группа ........... ${item.studyGroup}")
        println("  Вид спорта ............... ${item.sport.title}")
        println("  Соревнование ............. ${item.competition}")
        println("  Уровень .................. ${item.level.title} " +
            "(коэффициент ${item.level.weight})")
        println("  Занятое место ............ ${item.place} из ${item.participants}")
        println("  Медаль ................... ${item.medal}")
        println("  Дата соревнования ........ ${item.eventDate.format(dateFormat)}")
        println("  Тренер ................... ${item.coach}")
        println("  Рейтинговый балл ......... ${formatPoints(item.ratingPoints)}")
        println()
    }

    private fun describe(item: Achievement?): String =
        if (item == null) "—"
        else "${formatPoints(item.ratingPoints)} балла(ов) — ${item.athlete}, ${item.competition}"

    private fun printInlineError(message: String) {
        println("  [Ошибка ввода] $message.")
    }

    private fun formatPoints(value: Double): String = "%.1f".format(value)

    private companion object {
        /** Ширина рамки заголовка и главного меню. */
        const val MENU_WIDTH = 64
    }

    private fun cut(text: String, length: Int): String =
        if (text.length <= length) text else text.take(length - 1) + "…"
}
