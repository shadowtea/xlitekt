package xlitekt.game.packet

/**
 * @author Tyler Telis
 */
data class RunClientScriptPacket(
    val id: Int,
    val parameters: List<Any>
) : Packet {
    constructor(id: Int) : this(id, emptyList())
}
