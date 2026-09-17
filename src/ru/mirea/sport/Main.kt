package ru.mirea.sport

import ru.mirea.sport.service.AchievementService
import ru.mirea.sport.storage.AchievementRepository
import ru.mirea.sport.ui.ConsoleUi
import ru.mirea.sport.ui.InputReader
import java.io.BufferedReader
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PrintStream

/** Имя файла данных, используемое по умолчанию. */
private const val DEFAULT_DATA_FILE = "achievements.json"

/**
 * Точка входа в приложение.
 *
 * Функция выполняет сборку объектов приложения (хранилище, сервис,
 * пользовательский интерфейс) и передаёт управление главному циклу меню.
 *
 * @param args аргументы командной строки; первый аргумент, если он задан,
 *             определяет путь к файлу данных
 */
fun main(args: Array<String>) {
    // Явная установка кодировки UTF-8 для стандартного потока вывода
    // обеспечивает корректное отображение кириллицы в консоли.
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, Charsets.UTF_8))

    val dataFile = File(if (args.isNotEmpty()) args[0] else DEFAULT_DATA_FILE)
    val repository = AchievementRepository(dataFile)
    val service = AchievementService(repository)
    val reader = InputReader(BufferedReader(InputStreamReader(System.`in`, Charsets.UTF_8)))
    val ui = ConsoleUi(service, reader)
    ui.run()
}
