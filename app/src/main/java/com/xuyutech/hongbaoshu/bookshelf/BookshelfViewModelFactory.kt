package com.xuyutech.hongbaoshu.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xuyutech.hongbaoshu.pack.index.PackIndexStore

class BookshelfViewModelFactory(
    private val packIndexStore: PackIndexStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookshelfViewModel::class.java)) {
            return BookshelfViewModel(packIndexStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

