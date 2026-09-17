package ru.mirea.sport.json

/**
 * Генератор текстового представления JSON-документа.
 *
 * Выполняет обход дерева [JsonValue] и формирует форматированный текст
 * с отступами, пригодный для чтения человеком.
 */
object JsonWriter {

    private const val INDENT = "  "

    /**
     * Преобразует дерево JSON в форматированную строку.
     *
     * @param value корневой узел документа
     * @return текстовое представление документа
     */
    fun write(value: JsonValue): String {
        val builder = StringBuilder()
        writeValue(value, builder, 0)
        return builder.toString()
    }

    private fun writeValue(value: JsonValue, out: StringBuilder, level: Int) {
        when (value) {
            is JsonValue.JsonNull -> out.append("null")
            is JsonValue.JsonBool -> out.append(value.value)
            is JsonValue.JsonNumber -> out.append(formatNumber(value.value))
            is JsonValue.JsonString -> out.append(escape(value.value))
            is JsonValue.JsonArray -> writeArray(value, out, level)
            is JsonValue.JsonObject -> writeObject(value, out, level)
        }
    }

    private fun writeArray(value: JsonValue.JsonArray, out: StringBuilder, level: Int) {
        if (value.items.isEmpty()) {
            out.append("[]")
            return
        }
        out.append("[\n")
        value.items.forEachIndexed { index, item ->
            out.append(INDENT.repeat(level + 1))
            writeValue(item, out, level + 1)
            if (index != value.items.size - 1) out.append(',')
            out.append('\n')
        }
        out.append(INDENT.repeat(level)).append(']')
    }

    private fun writeObject(value: JsonValue.JsonObject, out: StringBuilder, level: Int) {
        if (value.fields.isEmpty()) {
            out.append("{}")
            return
        }
        out.append("{\n")
        val entries = value.fields.entries.toList()
        entries.forEachIndexed { index, (key, item) ->
            out.append(INDENT.repeat(level + 1))
            out.append(escape(key)).append(": ")
            writeValue(item, out, level + 1)
            if (index != entries.size - 1) out.append(',')
            out.append('\n')
        }
        out.append(INDENT.repeat(level)).append('}')
    }

    private fun formatNumber(number: Double): String =
        if (number == Math.floor(number) && !number.isInfinite()) {
            number.toLong().toString()
        } else {
            number.toString()
        }

    private fun escape(text: String): String {
        val builder = StringBuilder("\"")
        for (ch in text) {
            when (ch) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(ch)
            }
        }
        return builder.append('"').toString()
    }
}
