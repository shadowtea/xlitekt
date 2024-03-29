package xlitekt.game.world

import io.ktor.util.collections.ConcurrentList
import xlitekt.cache.provider.map.MapEntryTypeProvider
import xlitekt.game.actor.NPCList
import xlitekt.game.actor.PlayerList
import xlitekt.game.actor.npc.NPC
import xlitekt.game.actor.player.Client
import xlitekt.game.actor.player.Player
import xlitekt.game.world.map.collision.CollisionMap
import xlitekt.game.world.map.location.Location
import xlitekt.game.world.map.zone.Zones
import xlitekt.shared.inject
import xlitekt.shared.resource.NPCSpawns
import java.util.concurrent.ConcurrentHashMap

class World(
    val players: PlayerList = PlayerList(MAX_PLAYERS),
    val npcs: NPCList = NPCList(MAX_NPCs),
    val loginRequests: ConcurrentHashMap<Player, Client> = ConcurrentHashMap(),
    val logoutRequests: ConcurrentList<Player> = ConcurrentList()
) {
    private val maps by inject<MapEntryTypeProvider>()
    private val npcSpawns by inject<NPCSpawns>()

    /**
     * Builds the game world including but not limited to map collision and npc spawns.
     */
    fun build() {
        // Apply collision map.
        maps.entries().forEach(CollisionMap::applyCollision)
        // Apply npc spawns.
        npcSpawns.forEach {
            val location = Location(it.x, it.z, it.level)
            val npc = NPC(it.id, location)
            npc.previousLocation = location
            spawn(npc)
        }
    }

    /**
     * Spawn a npc into the game world and update the corresponding zone.
     */
    fun spawn(npc: NPC) {
        npcs.add(npc)
        Zones[npc.location]?.npcs?.add(npc)
    }

    /**
     * Request login into the game world. Only use this for logging in a player.
     * This is because login requests are synchronized with the game tick and this is
     * how login requests are pooled.
     *
     * Login requests are processed by the WorldLoginSynchronizer.
     */
    fun requestLogin(player: Player, client: Client) {
        loginRequests[player] = client
    }

    /**
     * Request logout out the game world. Only use this for logging out a player.
     * This is because logout requests are synchronized with the game tick and this
     * is how logout requests are pooled.
     *
     * Logout requests are processed by the WorldLogoutSynchronizer.
     */
    fun requestLogout(player: Player) {
        logoutRequests += player
    }

    companion object {
        const val MAX_PLAYERS = 2048
        const val MAX_NPCs = Short.MAX_VALUE.toInt()
    }
}
