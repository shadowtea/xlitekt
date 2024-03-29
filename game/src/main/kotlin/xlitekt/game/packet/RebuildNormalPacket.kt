package xlitekt.game.packet

import xlitekt.game.actor.player.Viewport
import xlitekt.game.world.map.location.Location

/**
 * @author Jordan Abraham
 */
data class RebuildNormalPacket(
    val viewport: Viewport,
    val location: Location,
    val update: Boolean
) : Packet
