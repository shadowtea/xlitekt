package com.runetopic.xlitekt.cache.provider

import com.runetopic.cache.store.Js5Store
import com.runetopic.xlitekt.plugin.koin.inject

abstract class EntryTypeProvider<T : EntryType> {
    protected val entries = mutableSetOf<T>()
    protected val js5Store by inject<Js5Store>()

    abstract fun load(): Unit
    abstract fun loadEntryType(data: ByteArray, type: T)

    fun entryType(id: Int): T? = entries.find { it.id == id }

    companion object {
        const val CONFIG_INDEX = 2
        const val VARBIT_GROUP_ID = 14
    }
}
