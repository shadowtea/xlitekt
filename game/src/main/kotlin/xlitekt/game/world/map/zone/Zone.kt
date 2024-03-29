package xlitekt.game.world.map.zone

import xlitekt.game.actor.npc.NPC
import xlitekt.game.world.map.obj.GameObject
import java.util.Collections

class Zone(
    val npcs: MutableList<NPC?> = Collections.synchronizedList(mutableListOf()),
    val objects: MutableList<GameObject?> = Collections.synchronizedList(mutableListOf())
)
