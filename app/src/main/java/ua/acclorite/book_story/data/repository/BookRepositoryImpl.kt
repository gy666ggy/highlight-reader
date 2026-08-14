/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.local.cache.TextDiskCache
import ua.acclorite.book_story.data.local.room.BookDatabase
import ua.acclorite.book_story.data.mapper.book.BookMapper
import ua.acclorite.book_story.data.mapper.file.FileMapper
import ua.acclorite.book_story.data.parser.cover.CoverParser
import ua.acclorite.book_story.data.parser.text.TextParser
import ua.acclorite.book_story.domain.model.file.File
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.repository.BookRepository
import ua.acclorite.book_story.domain.service.FileProvider
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BookRepository"

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val database: BookDatabase,
    private val bookMapper: BookMapper,
    private val fileMapper: FileMapper,
    private val coverParser: CoverParser,
    private val textParser: TextParser,
    private val fileProvider: FileProvider,
    private val textDiskCache: TextDiskCache
) : BookRepository {

    private val textCache = LruCache<Int, List<ReaderText>>(5)

    override suspend fun searchBooks(query: String): Result<List<Book>> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookDao.searchBooks(query).map { bookMapper.toBook(it) }
        }
    }

    override suspend fun getBook(bookId: Int): Result<Book> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookDao.findBookById(bookId).let {
                if (it == null) throw NoSuchElementException("Couldn't get book [$bookId].")
                else bookMapper.toBook(it)
            }
        }
    }

    override suspend fun getText(bookId: Int): Result<List<ReaderText>> {
        textCache[bookId]?.let { return Result.success(it) }

        // Try disk cache
        textDiskCache.load(bookId)?.let { cached ->
            textCache.put(bookId, cached)
            return Result.success(cached)
        }

        return withContext(Dispatchers.IO) {
            getBook(bookId)
                .mapCatching { fileProvider.getFileFromBook(it).getOrThrow() }
                .mapCatching { textParser.parse(it) }
        }.also { result ->
            result.onSuccess {
                textCache.put(bookId, it)
                textDiskCache.save(bookId, it)
            }
        }
    }

    override suspend fun getTextForBook(book: Book): Result<List<ReaderText>> {
        textCache[book.id]?.let { return Result.success(it) }

        // Try disk cache
        textDiskCache.load(book.id)?.let { cached ->
            textCache.put(book.id, cached)
            return Result.success(cached)
        }

        return withContext(Dispatchers.IO) {
            runCatching { fileProvider.getFileFromBook(book).getOrThrow() }
                .mapCatching { textParser.parse(it) }
        }.also { result ->
            result.onSuccess {
                textCache.put(book.id, it)
                textDiskCache.save(book.id, it)
            }
        }
    }

    override suspend fun preParseText(books: List<Book>) {
        withContext(Dispatchers.IO) {
            books.forEach { book ->
                // Skip if already cached in memory or disk
                if (textCache[book.id] != null) return@forEach
                if (textDiskCache.hasCache(book.id)) return@forEach

                try {
                    logI(TAG, "Pre-parsing book: [${book.title}]")
                    val file = fileProvider.getFileFromBook(book).getOrThrow()
                    val text = textParser.parse(file)
                    textDiskCache.save(book.id, text)
                    textCache.put(book.id, text)
                    logI(TAG, "Pre-parsed book: [${book.title}] with ${text.size} items")
                } catch (e: Exception) {
                    logI(TAG, "Could not pre-parse book [${book.title}]: ${e.message}")
                }
            }
        }
    }

    override suspend fun getFileFromBook(bookId: Int): Result<File> {
        return withContext(Dispatchers.IO) {
            getBook(bookId)
                .mapCatching { fileProvider.getFileFromBook(it).getOrThrow() }
                .mapCatching { fileMapper.toFile(it) }
        }
    }

    override suspend fun addBook(book: Book): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookDao.insertBook(bookMapper.toBookEntity(book))
        }
    }

    override suspend fun updateBook(book: Book): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookDao.updateBook(bookMapper.toBookEntity(book)).also {
                if (it == 0) throw Exception("Could not update book in database.")
            }
        }
    }

    override suspend fun deleteBook(book: Book): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookDao.deleteBook(bookMapper.toBookEntity(book)).also {
                if (it == 0) throw Exception("Could not delete book in database.")
            }
            textCache.remove(book.id)
            textDiskCache.remove(book.id)
        }
    }

    override suspend fun getDefaultCover(book: Book): Result<CoverImage?> = runCatching {
        return withContext(Dispatchers.IO) {
            fileProvider.getFileFromBook(book).mapCatching {
                coverParser.parse(it)
            }
        }
    }
}
