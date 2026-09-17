package ru.mirea.sport.storage

import ru.mirea.sport.json.JsonException
import ru.mirea.sport.json.JsonParser
import ru.mirea.sport.json.JsonValue
import ru.mirea.sport.json.JsonWriter
import ru.mirea.sport.json.requireInt
import ru.mirea.sport.json.requireString
import ru.mirea.sport.model.Achievement
import ru.mirea.sport.model.CompetitionLevel
import ru.mirea.sport.model.Institute
import ru.mirea.sport.model.SportKind
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Хранилище спортивных достижений, обеспечивающее их загрузку
 * из JSON-файла и сохранение в JSON-файл.
 *
 * Класс инкапсулирует все детали взаимодействия с файловой системой
 * и преобразования объектов предметной области в дерево JSON и обратно.
 *
 * @property file файл данных приложения
 */
class AchievementRepository(private val file: File) {

    /**
     * Загружает список достижений из файла.
     *
     * Отсутствие файла не считается ошибкой: при первом запуске приложения
     * возвращается пустой список, который затем будет сохранён на диск.
     *
     * @return изменяемый список загруженных достижений
     * @throws IOException при ошибке чтения файла
     * @throws JsonException при нарушении структуры документа
     */
    fun load(): MutableList<Achievement> {
        if (!file.exists()) return mutableListOf()
        val text = file.readText(Charsets.UTF_8)
        if (text.isBlank()) return mutableListOf()
        val root = JsonParser.parse(text)
        val result = mutableListOf<Achievement>()
        root.asArray().forEachIndexed { index, element ->
            try {
                result.add(toAchievement(element))
            } catch (e: JsonException) {
                throw JsonException("Запись №${index + 1}: ${e.message}")
            }
        }
        checkUniqueIdentifiers(result)
        return result
    }

    /**
     * Сохраняет список достижений в файл.
     *
     * Перед перезаписью создаётся резервная копия файла с расширением .bak,
     * что позволяет восстановить данные при сбое во время записи.
     *
     * @param achievements сохраняемая коллекция достижений
     * @throws IOException при ошибке записи файла
     */
    fun save(achievements: List<Achievement>) {
        if (file.exists()) {
            file.copyTo(File(file.absolutePath + ".bak"), overwrite = true)
        }
        val json = JsonValue.JsonArray(achievements.map { toJson(it) })
        file.writeText(JsonWriter.write(json), Charsets.UTF_8)
    }

    /** Возвращает абсолютный путь к файлу данных. */
    fun path(): String = file.absolutePath

    private fun toJson(item: Achievement): JsonValue = JsonValue.JsonObject(
        linkedMapOf(
            "id" to JsonValue.JsonNumber(item.id.toDouble()),
            "athlete" to JsonValue.JsonString(item.athlete),
            "institute" to JsonValue.JsonString(item.institute.name),
            "studyGroup" to JsonValue.JsonString(item.studyGroup),
            "sport" to JsonValue.JsonString(item.sport.name),
            "competition" to JsonValue.JsonString(item.competition),
            "level" to JsonValue.JsonString(item.level.name),
            "place" to JsonValue.JsonNumber(item.place.toDouble()),
            "participants" to JsonValue.JsonNumber(item.participants.toDouble()),
            "eventDate" to JsonValue.JsonString(item.eventDate.toString()),
            "coach" to JsonValue.JsonString(item.coach)
        )
    )

    private fun toAchievement(value: JsonValue): Achievement {
        val fields = value.asObject()

        val instituteCode = fields.requireString("institute")
        val institute = Institute.fromCode(instituteCode)
            ?: throw JsonException("Неизвестный институт «$instituteCode»")

        val sportCode = fields.requireString("sport")
        val sport = SportKind.fromCode(sportCode)
            ?: throw JsonException("Неизвестный вид спорта «$sportCode»")

        val levelCode = fields.requireString("level")
        val level = CompetitionLevel.fromCode(levelCode)
            ?: throw JsonException("Неизвестный уровень соревнования «$levelCode»")

        val dateText = fields.requireString("eventDate")
        val date = try {
            LocalDate.parse(dateText)
        } catch (e: DateTimeParseException) {
            throw JsonException("Некорректная дата проведения «$dateText»")
        }

        val achievement = Achievement(
            id = fields.requireInt("id"),
            athlete = fields.requireString("athlete"),
            institute = institute,
            studyGroup = fields.requireString("studyGroup"),
            sport = sport,
            competition = fields.requireString("competition"),
            level = level,
            place = fields.requireInt("place"),
            participants = fields.requireInt("participants"),
            eventDate = date,
            coach = fields.requireString("coach")
        )
        validate(achievement)
        return achievement
    }

    private fun validate(item: Achievement) {
        if (item.id <= 0) {
            throw JsonException("Идентификатор должен быть положительным числом")
        }
        if (item.athlete.isBlank()) {
            throw JsonException("Фамилия спортсмена не может быть пустой")
        }
        if (item.studyGroup.isBlank()) {
            throw JsonException("Учебная группа не может быть пустой")
        }
        if (item.competition.isBlank()) {
            throw JsonException("Наименование соревнования не может быть пустым")
        }
        if (item.place !in 1..MAX_PLACE) {
            throw JsonException("Занятое место должно быть в диапазоне 1..$MAX_PLACE")
        }
        if (item.participants !in 1..MAX_PARTICIPANTS) {
            throw JsonException(
                "Количество участников должно быть в диапазоне 1..$MAX_PARTICIPANTS"
            )
        }
        if (item.place > item.participants) {
            throw JsonException("Занятое место не может превышать количество участников")
        }
    }

    private fun checkUniqueIdentifiers(items: List<Achievement>) {
        val duplicate = items.groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }
        if (duplicate != null) {
            throw JsonException("Идентификатор ${duplicate.key} встречается более одного раза")
        }
    }

    private companion object {
        const val MAX_PLACE = 100
        const val MAX_PARTICIPANTS = 10_000
    }
}
