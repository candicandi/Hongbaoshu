package com.xuyutech.hongbaoshu.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuyutech.hongbaoshu.pack.index.PackIndexStore
import com.xuyutech.hongbaoshu.pack.model.PackIndex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BookshelfViewModel(
    private val packIndexStore: PackIndexStore
) : ViewModel() {

    val packs: StateFlow<List<PackIndex>> = packIndexStore.packs
        .map { it.sortedByDescending { p -> p.lastOpenedAt ?: p.importedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

