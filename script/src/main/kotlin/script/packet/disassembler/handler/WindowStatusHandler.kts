package script.packet.disassembler.handler

import xlitekt.game.content.ui.InterfaceLayout
import xlitekt.game.content.vars.VarBit
import xlitekt.game.packet.WindowStatusPacket
import xlitekt.game.packet.disassembler.handler.onPacketHandler

/**
 * @author Jordan Abraham
 */
onPacketHandler<WindowStatusPacket> {
    var interfaceLayout = InterfaceLayout.values().find { it.id == packet.displayMode }
        ?: throw IllegalStateException("Unhandled display mode sent from client. Mode=${packet.displayMode} Width=${packet.width} Height=${packet.height}")
    if (interfaceLayout == InterfaceLayout.RESIZABLE && player.vars[VarBit.SideStonesArrangement] == 1) {
        interfaceLayout = InterfaceLayout.RESIZABLE_MODERN
    }
    if (player.interfaces.currentInterfaceLayout == interfaceLayout) return@onPacketHandler
    player.interfaces.switchLayout(interfaceLayout)
}
