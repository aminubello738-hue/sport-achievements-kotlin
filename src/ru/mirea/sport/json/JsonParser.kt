package ru.mirea.sport.json

/**
 * Синтаксический анализатор JSON, реализованный методом рекурсивного спуска.
 *
 * Разбор выполняется за один проход по строке: указатель [pos] последовательно
 * перемещается по символам исходного текста, а каждому нетерминалу грамматики
 * JSON соответствует отдельный метод класса.
 *
 * @property source исходный текст JSON-документа
 */
class JsonParser(private val source: String) {

    private var pos = 0

    companion object {
        /**
         * Разбирает переданную строку и возвращает корневой узел документа.
         *
         * @throws JsonException если текст не является корректным JSON
         */
        fun parse(text: String): JsonValue {
            val parser = JsonParser(text)
            parser.skipWhitespace()
            val value = parser.parseValue()
            parser.skipWhitespace()
            if (!parser.isAtEnd()) {
                throw JsonException(
                    "Лишние символы после завершения документа (позиция ${parser.pos})"
                )
            }
            return value
        }
    }

    private fun isAtEnd(): Boolean = pos >= source.length

    private fun peek(): Char {
        if (isAtEnd()) throw JsonException("Неожиданный конец документа")
        return source[pos]
    }

    private fun skipWhitespace() {
        while (!isAtEnd() && source[pos].isWhitespace()) pos++
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (isAtEnd() || source[pos] != expected) {
            val found = if (isAtEnd()) "конец документа" else "«${source[pos]}»"
            throw JsonException("Ожидался символ «$expected», обнаружено $found (позиция $pos)")
        }
        pos++
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.JsonString(parseString())
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> parseNumber()
        }
    }

    private fun parseObject(): JsonValue {
        expect('{')
        val fields = LinkedHashMap<String, JsonValue>()
        skipWhitespace()
        if (peek() == '}') {
            pos++
            return JsonValue.JsonObject(fields)
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            expect(':')
            fields[key] = parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> pos++
                '}' -> {
                    pos++
                    return JsonValue.JsonObject(fields)
                }
                else -> throw JsonException(
                    "Ожидался символ «,» или «}» (позиция $pos)"
                )
            }
        }
    }

    private fun parseArray(): JsonValue {
        expect('[')
        val items = mutableListOf<JsonValue>()
        skipWhitespace()
        if (peek() == ']') {
            pos++
            return JsonValue.JsonArray(items)
        }
        while (true) {
            items.add(parseValue())
            skipWhitespace()
            when (peek()) {
                ',' -> pos++
                ']' -> {
                    pos++
                    return JsonValue.JsonArray(items)
                }
                else -> throw JsonException(
                    "Ожидался символ «,» или «]» (позиция $pos)"
                )
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val builder = StringBuilder()
        while (true) {
            if (isAtEnd()) throw JsonException("Незавершённая строка в JSON-документе")
            val ch = source[pos++]
            when {
                ch == '"' -> return builder.toString()
                ch == '\\' -> builder.append(parseEscape())
                else -> builder.append(ch)
            }
        }
    }

    private fun parseEscape(): Char {
        if (isAtEnd()) throw JsonException("Незавершённая управляющая последовательность")
        return when (val ch = source[pos++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (pos + 4 > source.length) {
                    throw JsonException("Некорректная последовательность \\u")
                }
                val hex = source.substring(pos, pos + 4)
                pos += 4
                hex.toIntOrNull(16)?.toChar()
                    ?: throw JsonException("Некорректный шестнадцатеричный код «$hex»")
            }
            else -> throw JsonException("Недопустимая управляющая последовательность «\\$ch»")
        }
    }

    private fun parseNumber(): JsonValue {
        val start = pos
        if (!isAtEnd() && (source[pos] == '-' || source[pos] == '+')) pos++
        while (!isAtEnd() && (source[pos].isDigit() || source[pos] in ".eE+-")) pos++
        val text = source.substring(start, pos)
        val number = text.toDoubleOrNull()
            ?: throw JsonException("Некорректное число «$text» (позиция $start)")
        return JsonValue.JsonNumber(number)
    }

    private fun parseBoolean(): JsonValue = when {
        source.startsWith("true", pos) -> {
            pos += 4
            JsonValue.JsonBool(true)
        }
        source.startsWith("false", pos) -> {
            pos += 5
            JsonValue.JsonBool(false)
        }
        else -> throw JsonException("Некорректный литерал (позиция $pos)")
    }

    private fun parseNull(): JsonValue {
        if (!source.startsWith("null", pos)) {
            throw JsonException("Некорректный литерал (позиция $pos)")
        }
        pos += 4
        return JsonValue.JsonNull
    }
}
