package ru.mirea.sport.service

import ru.mirea.sport.model.Achievement
import ru.mirea.sport.model.CompetitionLevel
import ru.mirea.sport.model.Institute
import ru.mirea.sport.model.SportKind
import ru.mirea.sport.storage.AchievementRepository
import java.time.LocalDate

/**
 * Критерий упорядочивания списка достижений.
 *
 * Каждому элементу перечисления сопоставлен объект [Comparator],
 * что позволяет выбирать способ сортировки во время выполнения программы.
 *
 * @property title наименование критерия, отображаемое в меню
 * @property comparator компаратор, задающий порядок следования записей
 */
enum class SortCriterion(val title: String, val comparator: Comparator<Achievement>) {
    BY_ATHLETE("по фамилии спортсмена (А–Я)", compareBy { it.athlete.lowercase() }),
    BY_PLACE("по занятому месту (возрастание)", compareBy { it.place }),
    BY_RATING("по рейтинговому баллу (убывание)", compareByDescending { it.ratingPoints }),
    BY_DATE("по дате соревнования (убывание)", compareByDescending { it.eventDate }),
    BY_SPORT("по виду спорта", compareBy { it.sport.title }),
    BY_INSTITUTE("по институту", compareBy<Achievement> { it.institute.shortName }
        .thenBy { it.athlete.lowercase() });

    companion object {
        /** Возвращает критерий сортировки по номеру пункта меню. */
        fun fromMenuIndex(index: Int): SortCriterion? = entries.getOrNull(index - 1)
    }
}

/**
 * Агрегированные показатели по совокупности спортивных достижений.
 *
 * @property total общее количество записей
 * @property prizeCount количество призовых мест
 * @property goldCount количество первых мест
 * @property athletes количество уникальных спортсменов
 * @property totalRating суммарный рейтинговый балл
 * @property averageRating средний рейтинговый балл
 * @property averagePlace среднее занятое место
 * @property bestAchievement достижение с максимальным рейтинговым баллом
 * @property worstAchievement достижение с минимальным рейтинговым баллом
 * @property countBySport распределение записей по видам спорта
 * @property countByLevel распределение записей по уровням соревнований
 * @property ratingByInstitute суммарный рейтинговый балл по институтам
 */
data class AchievementStatistics(
    val total: Int,
    val prizeCount: Int,
    val goldCount: Int,
    val athletes: Int,
    val totalRating: Double,
    val averageRating: Double,
    val averagePlace: Double,
    val bestAchievement: Achievement?,
    val worstAchievement: Achievement?,
    val countBySport: Map<SportKind, Int>,
    val countByLevel: Map<CompetitionLevel, Int>,
    val ratingByInstitute: Map<Institute, Double>
) {
    /** Доля призовых мест в общем количестве записей, %. */
    val prizeShare: Double
        get() = if (total == 0) 0.0 else prizeCount * 100.0 / total
}

/**
 * Сервис бизнес-логики приложения.
 *
 * Класс реализует операции над коллекцией спортивных достижений и
 * делегирует сохранение данных объекту [AchievementRepository], что
 * обеспечивает независимость бизнес-логики от способа хранения данных.
 *
 * @property repository хранилище данных
 */
class AchievementService(private val repository: AchievementRepository) {

    private val achievements: MutableList<Achievement> = mutableListOf()

    /** Признак наличия несохранённых изменений в коллекции. */
    var hasUnsavedChanges: Boolean = false
        private set

    /**
     * Загружает данные из файла, замещая текущее содержимое коллекции.
     *
     * @return количество загруженных записей
     */
    fun load(): Int {
        val loaded = repository.load()
        achievements.clear()
        achievements.addAll(loaded)
        hasUnsavedChanges = false
        return achievements.size
    }

    /** Сохраняет текущее состояние коллекции в файл. */
    fun save() {
        repository.save(achievements)
        hasUnsavedChanges = false
    }

    /** Возвращает неизменяемую копию коллекции достижений. */
    fun all(): List<Achievement> = achievements.toList()

    /** Возвращает количество записей в коллекции. */
    fun count(): Int = achievements.size

    /** Возвращает путь к файлу данных. */
    fun dataFilePath(): String = repository.path()

    /** Находит достижение по идентификатору или возвращает null. */
    fun findById(id: Int): Achievement? = achievements.firstOrNull { it.id == id }

    /**
     * Добавляет новое достижение, автоматически присваивая ему идентификатор.
     *
     * @return созданный объект достижения
     */
    fun add(
        athlete: String,
        institute: Institute,
        studyGroup: String,
        sport: SportKind,
        competition: String,
        level: CompetitionLevel,
        place: Int,
        participants: Int,
        eventDate: LocalDate,
        coach: String
    ): Achievement {
        val achievement = Achievement(
            id = nextId(),
            athlete = athlete,
            institute = institute,
            studyGroup = studyGroup,
            sport = sport,
            competition = competition,
            level = level,
            place = place,
            participants = participants,
            eventDate = eventDate,
            coach = coach
        )
        achievements.add(achievement)
        hasUnsavedChanges = true
        return achievement
    }

    /** Помечает коллекцию как изменённую после редактирования записи. */
    fun markModified() {
        hasUnsavedChanges = true
    }

    /**
     * Удаляет достижение с заданным идентификатором.
     *
     * @return удалённая запись или null, если запись не найдена
     */
    fun remove(id: Int): Achievement? {
        val achievement = findById(id) ?: return null
        achievements.remove(achievement)
        hasUnsavedChanges = true
        return achievement
    }

    /**
     * Выполняет поиск достижений по вхождению подстроки в поля «спортсмен»,
     * «учебная группа», «соревнование», «вид спорта», «институт» и «тренер».
     * Регистр символов при сравнении не учитывается.
     *
     * @param query искомая подстрока
     * @return список найденных записей
     */
    fun search(query: String): List<Achievement> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        return achievements.filter {
            it.athlete.lowercase().contains(normalized) ||
                it.studyGroup.lowercase().contains(normalized) ||
                it.competition.lowercase().contains(normalized) ||
                it.sport.title.lowercase().contains(normalized) ||
                it.institute.shortName.lowercase().contains(normalized) ||
                it.coach.lowercase().contains(normalized)
        }
    }

    /** Возвращает список призёров — записей с местом не ниже третьего. */
    fun prizeWinners(): List<Achievement> = achievements.filter { it.isPrizeWinner }

    /** Возвращает список достижений, упорядоченный согласно критерию. */
    fun sorted(criterion: SortCriterion): List<Achievement> =
        achievements.sortedWith(criterion.comparator)

    /**
     * Вычисляет агрегированные показатели по всей коллекции достижений.
     *
     * @return объект с рассчитанными показателями
     */
    fun statistics(): AchievementStatistics = AchievementStatistics(
        total = achievements.size,
        prizeCount = achievements.count { it.isPrizeWinner },
        goldCount = achievements.count { it.place == 1 },
        athletes = achievements.map { it.athlete.lowercase() }.distinct().size,
        totalRating = achievements.sumOf { it.ratingPoints },
        averageRating = if (achievements.isEmpty()) 0.0
        else achievements.sumOf { it.ratingPoints } / achievements.size,
        averagePlace = if (achievements.isEmpty()) 0.0
        else achievements.sumOf { it.place }.toDouble() / achievements.size,
        bestAchievement = achievements.maxByOrNull { it.ratingPoints },
        worstAchievement = achievements.minByOrNull { it.ratingPoints },
        countBySport = achievements.groupingBy { it.sport }.eachCount(),
        countByLevel = achievements.groupingBy { it.level }.eachCount(),
        ratingByInstitute = achievements
            .groupBy { it.institute }
            .mapValues { (_, items) -> items.sumOf { it.ratingPoints } }
    )

    private fun nextId(): Int = (achievements.maxOfOrNull { it.id } ?: 0) + 1
}
