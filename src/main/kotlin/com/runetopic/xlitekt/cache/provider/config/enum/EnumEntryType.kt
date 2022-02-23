package com.runetopic.xlitekt.cache.provider.config.enum

import com.runetopic.xlitekt.cache.provider.EntryType
import com.runetopic.xlitekt.cache.provider.config.ScriptType

data class EnumEntryType(
    override val id: Int,
    var keyType: ScriptType? = null,
    var valType: ScriptType? = null,
    var defaultString: String = "null",
    var defaultInt: Int = 0,
    var size: Int = 0,
    var params: Map<Int, Any> = mapOf()
) : EntryType(id)
