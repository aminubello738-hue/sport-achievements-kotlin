package ru.mirea.sport.model

import java.time.LocalDate

/**
 * Вид спорта, по которому зарегистрировано достижение.
 *
 * @property title наименование вида спорта на русском языке
 */
enum class SportKind(val title: String) {
    ATHLETICS("Лёгкая атлетика"),
    SWIMMING("Плавание"),
    VOLLEYBALL("Волейбол"),
    BASKETBALL("Баскетбол"),
    FOOTBALL("Футбол"),
    CHESS("Шахматы"),
    SKIING("Лыжные гонки"),
    JUDO("Дзюдо");

    companion object {
        /** Возвращает вид спорта по строковому коду или null, если код неизвестен. */
        fun fromCode(code: String): SportKind? =
            entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) }

        /** Возвращает вид спорта по номеру пункта меню (нумерация с единицы). */
        fun fromMenuIndex(index: Int): SportKind? = entries.getOrNull(index - 1)

        /** Формирует строку-подсказку со списком допустимых видов спорта. */
        fun menuHint(): String =
            entries.mapIndexed { i, s -> "${i + 1} - ${s.title}" }.joinToString(", ")
    }
}

/**
 * Уровень спортивного соревнования.
 *
 * Весовой коэффициент [weight] используется при расчёте рейтингового балла:
 * чем выше уровень состязания, тем большую ценность имеет результат.
 *
 * @property title наименование уровня соревнования
 * @property weight весовой коэффициент уровня
 */
enum class CompetitionLevel(val title: String, val weight: Double) {
    UNIVERSITY("Внутривузовский", 1.0),
    CITY("Городской", 2.0),
    REGIONAL("Региональный", 3.0),
    NATIONAL("Всероссийский", 5.0),
    INTERNATIONAL("Международный", 8.0);

    companion object {
        /** Возвращает уровень соревнования по строковому коду. */
        fun fromCode(code: String): CompetitionLevel? =
            entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) }

        /** Возвращает уровень соревнования по номеру пункта меню. */
        fun fromMenuIndex(index: Int): CompetitionLevel? = entries.getOrNull(index - 1)

        /** Формирует строку-подсказку со списком уровней соревнований. */
        fun menuHint(): String =
            entries.mapIndexed { i, l -> "${i + 1} - ${l.title}" }.joinToString(", ")
    }
}

/**
 * Институт университета, в котором обучается спортсмен.
 *
 * @property shortName сокращённое обозначение института
 * @property title полное наименование института
 */
enum class Institute(val shortName: String, val title: String) {
    IIT("ИИТ", "Институт информационных технологий"),
    IKB("ИКБ", "Институт кибербезопасности и цифровых технологий"),
    IPTIP("ИПТИП", "Институт перспективных технологий и индустриального программирования"),
    IRI("ИРИ", "Институт радиоэлектроники и информатики"),
    ITHT("ИТХТ", "Институт тонких химических технологий"),
    ITU("ИТУ", "Институт технологий управления");

    companion object {
        /** Возвращает институт по строковому коду. */
        fun fromCode(code: String): Institute? =
            entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) }

        /** Возвращает институт по номеру пункта меню. */
        fun fromMenuIndex(index: Int): Institute? = entries.getOrNull(index - 1)

        /** Формирует строку-подсказку со списком институтов. */
        fun menuHint(): String =
            entries.mapIndexed { i, inst -> "${i + 1} - ${inst.shortName}" }.joinToString(", ")
    }
}

/**
 * Спортивное достижение студента — основная сущность предметной области.
 *
 * @property id уникальный идентификатор записи
 * @property athlete фамилия и инициалы студента-спортсмена
 * @property institute институт, в котором обучается спортсмен
 * @property studyGroup учебная группа спортсмена
 * @property sport вид спорта
 * @property competition наименование соревнования
 * @property level уровень соревнования
 * @property place занятое место
 * @property participants количество участников соревнования
 * @property eventDate дата проведения соревнования
 * @property coach фамилия и инициалы тренера
 */
data class Achievement(
    val id: Int,
    var athlete: String,
    var institute: Institute,
    var studyGroup: String,
    var sport: SportKind,
    var competition: String,
    var level: CompetitionLevel,
    var place: Int,
    var participants: Int,
    var eventDate: LocalDate,
    var coach: String
) {
    /** Признак призового места (первое, второе или третье). */
    val isPrizeWinner: Boolean
        get() = place in 1..PRIZE_PLACES

    /**
     * Рейтинговый балл достижения.
     *
     * Вычисляется как произведение весового коэффициента уровня соревнования
     * на коэффициент занятого места, что позволяет сопоставлять результаты,
     * полученные на состязаниях разного масштаба.
     */
    val ratingPoints: Double
        get() = level.weight * placeFactor()

    /** Наименование медали, соответствующей занятому месту. */
    val medal: String
        get() = when (place) {
            1 -> "золото"
            2 -> "серебро"
            3 -> "бронза"
            else -> "—"
        }

    private fun placeFactor(): Double = when (place) {
        1 -> 10.0
        2 -> 7.0
        3 -> 5.0
        in 4..6 -> 3.0
        in 7..10 -> 2.0
        else -> 1.0
    }

    private companion object {
        /** Количество призовых мест на соревновании. */
        const val PRIZE_PLACES = 3
    }
}
