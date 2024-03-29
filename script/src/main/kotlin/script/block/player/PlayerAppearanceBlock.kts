package script.block.player

import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.writeShort
import xlitekt.game.actor.player.kit.BodyPartColor
import xlitekt.game.actor.player.kit.PlayerIdentityKit
import xlitekt.game.actor.render.Render.Appearance
import xlitekt.game.actor.render.block.onPlayerUpdateBlock
import xlitekt.shared.buffer.writeBytesAdd
import xlitekt.shared.buffer.writeStringCp1252NullTerminated

/**
 * @author Jordan Abraham
 */
onPlayerUpdateBlock<Appearance>(5, 0x1) {
    buildPacket {
        val data = buildPacket {
            writeByte(gender.mask.toByte())
            writeByte(skullIcon.toByte())
            writeByte(headIcon.toByte())
            if (transform != -1) writeTransmogrification(this@onPlayerUpdateBlock) else writeIdentityKit(this@onPlayerUpdateBlock)
            colour(bodyPartColors.entries)
            animate(this@onPlayerUpdateBlock)
            writeStringCp1252NullTerminated(displayName)
            writeByte(126) // Combat level
            writeShort(0) // Total level
            writeByte(0) // Hidden
        }
        writeByte(data.remaining.toByte())
        writeBytesAdd(data.readBytes())
    }
}

fun BytePacketBuilder.animate(render: Appearance) = if (render.transform == -1) {
    shortArrayOf(808, 823, 819, 820, 821, 822, 824).forEach(::writeShort)
} else {
    // TODO load npc defs for walking and stand anims for transmog.
}

fun BytePacketBuilder.writeTransmogrification(render: Appearance) {
    writeShort(65535.toShort())
    writeShort(render.transform.toShort())
}

fun BytePacketBuilder.writeIdentityKit(render: Appearance) = enumValues<PlayerIdentityKit>()
    .sortedBy { it.info.index }
    .forEach {
        // TODO We will need to add support for the item worn in the specific body slot.
        it.info.build(this, render.gender, render.bodyParts.getOrDefault(it.bodyPart, 0))
    }

fun BytePacketBuilder.colour(colours: Set<Map.Entry<BodyPartColor, Int>>) = colours.sortedBy { it.key.id }.forEach { writeByte(it.value.toByte()) }
