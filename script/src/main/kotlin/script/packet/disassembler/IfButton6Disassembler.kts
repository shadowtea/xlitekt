package script.packet.disassembler

import io.ktor.utils.io.core.readInt
import io.ktor.utils.io.core.readUShort
import xlitekt.game.packet.IfButtonPacket
import xlitekt.game.packet.disassembler.onPacketDisassembler

/**
 * @author Jordan Abraham
 */
onPacketDisassembler(opcode = 64, size = 8) {
    IfButtonPacket(
        index = 6,
        packedInterface = readInt(),
        slotId = readUShort().toInt(),
        itemId = readUShort().toInt()
    )
}
