package xlitekt.game.loop.task

import io.ktor.utils.io.core.ByteReadPacket
import xlitekt.game.actor.movement.MovementStep
import xlitekt.game.actor.npc.NPC
import xlitekt.game.actor.player.Player

/**
 * @author Jordan Abraham
 */
class SequentialSynchronizationTask : Task() {

    override fun run() {
        val players = world.players.filterNotNull().filter(Player::online)
        val npcs = world.npcs.toList().filterNotNull()
        val playerSteps = mutableMapOf<Player, MovementStep>()
        val npcSteps = mutableMapOf<NPC, MovementStep>()
        val updates = mutableMapOf<Player, ByteReadPacket>()

        players.forEach {
            playerSteps[it] = it.processMovement()
            updates[it] = it.processUpdateBlocks(it.pendingUpdates())
        }

        npcs.forEach {
            npcSteps[it] = it.processMovement()
        }

        val previousLocations = players.associateWith(Player::previousLocation)
        val currentLocations = players.associateWith(Player::location)

        players.forEach {
            it.sync(updates, previousLocations, currentLocations, playerSteps, npcSteps)
        }
    }
}
