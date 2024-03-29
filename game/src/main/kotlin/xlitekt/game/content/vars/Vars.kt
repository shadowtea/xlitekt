package xlitekt.game.content.vars

import xlitekt.cache.provider.config.varbit.VarBitEntryType
import xlitekt.cache.provider.config.varbit.VarBitEntryTypeProvider
import xlitekt.cache.provider.config.varbit.VarBitEntryTypeProvider.Companion.mersennePrime
import xlitekt.game.actor.player.Player
import xlitekt.game.actor.player.sendVarp
import xlitekt.shared.inject

/**
 * @author Tyler Telis
 * @author Jordan Abraham
 */
class Vars(
    private val player: Player,
    private val vars: MutableMap<Int, Int> = mutableMapOf()
) : MutableMap<Int, Int> by vars {

    fun login() {
        if (isEmpty()) return
        forEach { player.sendVarp(it.key, it.value) }
    }

    operator fun set(key: Var, value: Int): Int = when (key.varType) {
        VarType.VAR_PLAYER -> {
            vars[key.info.id] = value
            player.sendVarp(key.info.id, value)
            value
        }
        VarType.VAR_BIT -> {
            varbits.entryType(key.info.id)?.run {
                val parentValue = toVarpParent(this, value)
                vars[index] = parentValue
                player.sendVarp(index, parentValue)
                parentValue
            } ?: -1
        }
    }

    operator fun get(key: Var): Int = when (key.varType) {
        VarType.VAR_PLAYER -> vars[key.info.id] ?: 0
        VarType.VAR_BIT -> varbits.entryType(key.info.id)?.let(::fromVarpParent) ?: 0
    }

    fun flip(key: Var) = if (get(key) == 0) set(key, 1) else set(key, 0)

    private fun fromVarpParent(entryType: VarBitEntryType): Int {
        if (!vars.containsKey(entryType.index)) return 0
        return vars[entryType.index]!! shr entryType.leastSignificantBit and mersennePrime[entryType.mostSignificantBit - entryType.leastSignificantBit]
    }

    private fun toVarpParent(entryType: VarBitEntryType, value: Int): Int {
        val prime = mersennePrime[entryType.mostSignificantBit - entryType.leastSignificantBit] shl entryType.leastSignificantBit
        return (vars[entryType.index] ?: 0) and prime.inv() or (if (value < 0 || value > prime) 0 else value) shl entryType.leastSignificantBit and prime
    }

    private companion object {
        val varbits by inject<VarBitEntryTypeProvider>()
    }
}
