package ru.mirea.sport.ui

import java.io.BufferedReader
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Исключение, сигнализирующее об исчерпании входного потока
 * (например, при нажатии Ctrl+D или при работе в пакетном режиме).
 */
class EndOfInputException : Exception("Входной поток завершён")

/**
 * Компонент ввода данных с проверкой корректности.
 *
 * Все методы циклически запрашивают значение до тех пор, пока пользователь
 * не введёт корректные данные, что исключает аварийное завершение программы
 * при ошибочном вводе.
 *
 * @property input источник входных данных
 */
class InputReader(private val input: BufferedReader) {

    /**
     * Признак пакетного режима работы: если стандартный ввод перенаправлен
     * из файла, введённые данные дублируются в протокол работы программы.
     */
    private val echoInput: Boolean = System.console() == null

    /** Считывает строку ввода без дополнительных проверок. */
    fun readRaw(prompt: String): String {
        print(prompt)
        System.out.flush()
        val line = input.readLine() ?: throw EndOfInputException()
        if (echoInput) println(line)
        return line.trim()
    }

    /** Считывает непустую строку заданной максимальной длины. */
    fun readText(prompt: String, maxLength: Int = 60): String {
        while (true) {
            val value = readRaw(prompt)
            when {
                value.isEmpty() -> printError("значение не может быть пустым")
                value.length > maxLength -> printError("допустимо не более $maxLength символов")
                else -> return value
            }
        }
    }

    /** Считывает целое число из заданного диапазона. */
    fun readInt(prompt: String, range: IntRange): Int {
        while (true) {
            val value = readRaw(prompt)
            val number = value.toIntOrNull()
            when {
                number == null -> printError("введите целое число")
                number !in range -> printError(
                    "значение должно быть в диапазоне от ${range.first} до ${range.last}"
                )
                else -> return number
            }
        }
    }

    /** Считывает вещественное число из заданного диапазона. */
    fun readDouble(prompt: String, min: Double, max: Double): Double {
        while (true) {
            val value = readRaw(prompt).replace(',', '.')
            val number = value.toDoubleOrNull()
            when {
                number == null -> printError("введите число, например 45000 или 45000.50")
                number < min || number > max -> printError(
                    "значение должно быть в диапазоне от ${format(min)} до ${format(max)}"
                )
                else -> return number
            }
        }
    }

    /** Считывает дату в формате ГГГГ-ММ-ДД. */
    fun readDate(prompt: String): LocalDate {
        while (true) {
            val value = readRaw(prompt)
            try {
                return LocalDate.parse(value)
            } catch (e: DateTimeParseException) {
                printError("используйте формат ГГГГ-ММ-ДД, например 2026-07-15")
            }
        }
    }

    /**
     * Считывает необязательное значение: пустая строка означает,
     * что текущее значение поля сохраняется без изменений.
     */
    fun readOptional(prompt: String): String? {
        val value = readRaw(prompt)
        return value.ifEmpty { null }
    }

    /** Запрашивает подтверждение действия (д/н). */
    fun confirm(prompt: String): Boolean {
        while (true) {
            when (readRaw("$prompt (д/н): ").lowercase()) {
                "д", "да", "y", "yes" -> return true
                "н", "нет", "n", "no" -> return false
                else -> printError("введите «д» или «н»")
            }
        }
    }

    private fun printError(message: String) {
        println("  [Ошибка ввода] $message. Повторите ввод.")
    }

    private fun format(value: Double): String =
        if (value == Math.floor(value)) value.toLong().toString() else value.toString()
}
