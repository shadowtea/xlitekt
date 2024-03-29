package script.packet.assembler

import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.buildPacket
import script.packet.assembler.NPCInfoPacketAssembler.ActivityUpdateType.Adding
import script.packet.assembler.NPCInfoPacketAssembler.ActivityUpdateType.Moving
import script.packet.assembler.NPCInfoPacketAssembler.ActivityUpdateType.Removing
import script.packet.assembler.NPCInfoPacketAssembler.ActivityUpdateType.Updating
import xlitekt.game.actor.movement.MovementStep
import xlitekt.game.actor.movement.isValid
import xlitekt.game.actor.npc.NPC
import xlitekt.game.actor.player.Player
import xlitekt.game.actor.player.Viewport
import xlitekt.game.actor.render.block.buildNPCUpdateBlocks
import xlitekt.game.packet.NPCInfoPacket
import xlitekt.game.packet.assembler.onPacketAssembler
import xlitekt.game.world.map.location.Location
import xlitekt.game.world.map.location.withinDistance
import xlitekt.game.world.map.location.zones
import xlitekt.shared.buffer.BitAccess
import xlitekt.shared.buffer.withBitAccess

/**
 * @author Jordan Abraham
 * @author Tyler Telis
 */
onPacketAssembler<NPCInfoPacket>(opcode = 78, size = -2) {
    buildPacket {
        val blocks = BytePacketBuilder()
        withBitAccess {
            writeBits(8, viewport.npcs.size)
            highDefinition(viewport, blocks, playerLocations, steps)
            lowDefinition(viewport, blocks, playerLocations)
            if (blocks.size > 0) {
                writeBits(15, Short.MAX_VALUE.toInt())
            }
        }
        writePacket(blocks.build())
    }
}

fun BitAccess.highDefinition(
    viewport: Viewport,
    blocks: BytePacketBuilder,
    playerLocations: Map<Player, Location>,
    steps: Map<NPC, MovementStep>
) {
    val playerLocation = playerLocations[viewport.player] ?: viewport.player.location
    viewport.npcs.forEach {
        // Check the activities this npc is doing.
        val activity = highDefinitionActivities(it, playerLocation, steps)
        if (activity == null) {
            // This npc has no activity update (false).
            writeBit(false)
            return@forEach
        }
        // This npc has an activity update (true).
        writeBit(true)
        val updating = it.hasPendingUpdate()
        activity.writeBits(this, it, updating, playerLocation, steps[it])
        if (updating) blocks.buildNPCUpdateBlocks(it)
    }
    viewport.npcs.removeAll { !it.location.withinDistance(playerLocation) }
}

fun BitAccess.lowDefinition(viewport: Viewport, blocks: BytePacketBuilder, playerLocations: Map<Player, Location>) {
    val playerLocation = playerLocations[viewport.player] ?: viewport.player.location
    playerLocation.zones().forEach { zone ->
        zone.npcs.forEach {
            // Check the activities this npc is doing.
            val activity = lowDefinitionActivities(viewport, it, playerLocation)
            if (activity != null) {
                // Write the corresponding activity bits depending on what the npc is doing.
                activity.writeBits(this, it!!, playerLocation = playerLocation)
                when (activity) {
                    Adding -> {
                        viewport.npcs += it
                        if (it.hasPendingUpdate()) blocks.buildNPCUpdateBlocks(it)
                    }
                    else -> throw IllegalStateException("Low definition npc had an activity type of $activity")
                }
            }
        }
    }
}

fun highDefinitionActivities(
    npc: NPC,
    playerLocation: Location,
    steps: Map<NPC, MovementStep?>
): ActivityUpdateType? {
    return when {
        // If the npc is not within normal distance of the player.
        !npc.location.withinDistance(playerLocation) -> Removing
        // If the npc has a block update.
        npc.hasPendingUpdate() -> Updating
        // If the npc is moving.
        steps[npc]?.isValid() == true -> Moving
        else -> null
    }
}

fun lowDefinitionActivities(
    viewport: Viewport,
    npc: NPC?,
    playerLocation: Location
): ActivityUpdateType? {
    return when {
        // If the npc is within our normal distance and the server has not added them to the player viewport.
        npc != null && npc !in viewport.npcs && npc.location.withinDistance(playerLocation) -> Adding
        else -> null
    }
}

sealed class ActivityUpdateType {
    object Removing : ActivityUpdateType() {
        override fun writeBits(bits: BitAccess, npc: NPC, updating: Boolean, playerLocation: Location, step: MovementStep?) {
            bits.writeBits(2, 3)
        }
    }

    object Moving : ActivityUpdateType() {
        override fun writeBits(bits: BitAccess, npc: NPC, updating: Boolean, playerLocation: Location, step: MovementStep?) {
            bits.writeBits(2, 1)
            bits.writeBits(3, step!!.direction!!.npcOpcode())
            bits.writeBit(updating)
        }
    }

    object Updating : ActivityUpdateType() {
        override fun writeBits(bits: BitAccess, npc: NPC, updating: Boolean, playerLocation: Location, step: MovementStep?) {
            bits.writeBits(2, 0)
        }
    }

    object Adding : ActivityUpdateType() {
        override fun writeBits(bits: BitAccess, npc: NPC, updating: Boolean, playerLocation: Location, step: MovementStep?) {
            bits.writeBits(15, npc.index)
            bits.writeBits(1, 0) // if 1 == 1 read 32 bits they just don't use it atm. Looks like they're working on something
            val x = (npc.location.x - playerLocation.x).let { if (it < 15) it + 32 else it }
            val z = (npc.location.z - playerLocation.z).let { if (it < 15) it + 32 else it }
            bits.writeBits(1, 0)
            bits.writeBits(3, 0) // TODO orientation
            bits.writeBits(5, z)
            bits.writeBits(1, 0) // TODO handle teleporting
            bits.writeBits(14, npc.id)
            bits.writeBits(5, x)
        }
    }

    abstract fun writeBits(
        bits: BitAccess,
        npc: NPC,
        updating: Boolean = false,
        playerLocation: Location,
        step: MovementStep? = null
    )
}
