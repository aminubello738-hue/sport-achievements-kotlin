package ru.mirea.sport.json

/**
 * Исключение, возникающее при синтаксических или семантических
 * ошибках разбора JSON-документа.
 */
class JsonException(message: String) : Exception(message)

/**
 * Узел дерева JSON-документа.
 *
 * Иерархия реализована с помощью запечатанного (sealed) класса, что
 * позволяет исчерпывающе обрабатывать все варианты в конструкции when.
 */
sealed class JsonValue {

    /** Литерал null. */
    data object JsonNull : JsonValue()

    /** Логическое значение true или false. */
    data class JsonBool(val value: Boolean) : JsonValue()

    /** Числовое значение. */
    data class JsonNumber(val value: Double) : JsonValue()

    /** Строковое значение. */
    data class JsonString(val value: String) : JsonValue()

    /** Массив значений. */
    data class JsonArray(val items: List<JsonValue>) : JsonValue()

    /** Объект — набор пар «ключ — значение». */
    data class JsonObject(val fields: Map<String, JsonValue>) : JsonValue()

    /** Приводит узел к массиву или выбрасывает JsonException. */
    fun asArray(): List<JsonValue> = when (this) {
        is JsonArray -> items
        else -> throw JsonException("Ожидался массив JSON, получено: ${typeName()}")
    }

    /** Приводит узел к объекту или выбрасывает JsonException. */
    fun asObject(): Map<String, JsonValue> = when (this) {
        is JsonObject -> fields
        else -> throw JsonException("Ожидался объект JSON, получено: ${typeName()}")
    }

    /** Человекочитаемое название типа узла (для сообщений об ошибках). */
    fun typeName(): String = when (this) {
        is JsonNull -> "null"
        is JsonBool -> "логическое значение"
        is JsonNumber -> "число"
        is JsonString -> "строка"
        is JsonArray -> "массив"
        is JsonObject -> "объект"
    }
}

/** Извлекает обязательное строковое поле объекта. */
fun Map<String, JsonValue>.requireString(key: String): String {
    val value = this[key] ?: throw JsonException("Отсутствует обязательное поле «$key»")
    return (value as? JsonValue.JsonString)?.value
        ?: throw JsonException("Поле «$key» должно быть строкой")
}

/** Извлекает обязательное вещественное поле объекта. */
fun Map<String, JsonValue>.requireDouble(key: String): Double {
    val value = this[key] ?: throw JsonException("Отсутствует обязательное поле «$key»")
    return (value as? JsonValue.JsonNumber)?.value
        ?: throw JsonException("Поле «$key» должно быть числом")
}

/** Извлекает обязательное целочисленное поле объекта. */
fun Map<String, JsonValue>.requireInt(key: String): Int {
    val number = requireDouble(key)
    if (number != Math.floor(number)) {
        throw JsonException("Поле «$key» должно быть целым числом")
    }
    return number.toInt()
}
