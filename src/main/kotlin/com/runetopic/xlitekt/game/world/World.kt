package com.runetopic.xlitekt.game.world

import com.runetopic.xlitekt.game.actor.NPCList
import com.runetopic.xlitekt.game.actor.PlayerList
import com.runetopic.xlitekt.game.actor.player.Player
import com.runetopic.xlitekt.network.packet.NPCInfoPacket
import com.runetopic.xlitekt.network.packet.PlayerInfoPacket
import com.runetopic.xlitekt.shared.Dispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class World {
    val players = PlayerList(MAX_PLAYERS)
    val npcs = NPCList(MAX_NPCs)

    fun process() = runBlocking(Dispatcher.GAME) {
        val job = launch(Dispatcher.UPDATE) {
            val players = players.filterNotNull().filter(Player::online)
            players.parallelStream().forEach {
                it.write(PlayerInfoPacket(it))
                it.write(NPCInfoPacket(it))
                it.flushPool()
            }
            players.parallelStream().forEach(Player::reset)
        }
        job.join()
    }

    companion object {
        const val MAX_PLAYERS = 2048
        const val MAX_NPCs = Short.MAX_VALUE.toInt()
    }
}
