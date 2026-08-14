/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.book

import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.repository.BookRepository
import javax.inject.Inject

class PreParseTextUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {

    suspend operator fun invoke(books: List<Book>) {
        if (books.isEmpty()) return
        bookRepository.preParseText(books)
    }
}
