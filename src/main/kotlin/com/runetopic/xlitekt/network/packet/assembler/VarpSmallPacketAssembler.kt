package com.runetopic.xlitekt.network.packet.assembler

import com.runetopic.xlitekt.network.packet.VarpSmallPacket
import com.runetopic.xlitekt.util.ext.writeByteAdd
import io.ktor.utils.io.core.writeShort

class VarpSmallPacketAssembler : PacketAssembler<VarpSmallPacket>(opcode = 94, size = 3) {
    override fun assemblePacket(packet: VarpSmallPacket) = buildPacket {
        writeByteAdd(packet.value.toByte())
        writeShort(packet.id.toShort())
    }
}