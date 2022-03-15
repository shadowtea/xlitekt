package xlitekt.game

import xlitekt.cache.provider.map.MapEntryTypeProvider
import xlitekt.game.actor.npc.NPC
import xlitekt.game.world.World
import xlitekt.game.world.engine.LoopTask
import xlitekt.game.world.map.collision.CollisionMap
import xlitekt.game.world.map.location.Location
import xlitekt.game.world.map.zone.Zones
import xlitekt.shared.inject

class Game {
    private val loop = LoopTask()
    private val maps by inject<MapEntryTypeProvider>()

    private val world by inject<World>()

    fun start() {
        maps.entries().forEach(CollisionMap::applyCollision)
        println("Created ${Zones.zones.filterNotNull().size}")

        repeat(10) { x ->
            repeat(10) { z ->
                val location = Location(3222 + x, 3222 + z, 0)
                world.addNPC(NPC(0, location))
            }
        }
        loop.start()
    }

    fun shutdown() {
        loop.shutdown()
    }
}
