package com.runetopic.xlitekt.game.ui

import com.runetopic.xlitekt.cache.Cache.entryType
import com.runetopic.xlitekt.cache.provider.config.enum.EnumEntryType
import com.runetopic.xlitekt.game.actor.player.Player
import com.runetopic.xlitekt.game.actor.player.message
import com.runetopic.xlitekt.game.item.Item
import com.runetopic.xlitekt.game.ui.InterfaceMapping.addInterfaceListener
import com.runetopic.xlitekt.network.packet.IfCloseSubPacket
import com.runetopic.xlitekt.network.packet.IfMoveSubPacket
import com.runetopic.xlitekt.network.packet.IfOpenSubPacket
import com.runetopic.xlitekt.network.packet.IfOpenTopPacket
import com.runetopic.xlitekt.network.packet.IfSetEventsPacket
import com.runetopic.xlitekt.network.packet.IfSetTextPacket
import com.runetopic.xlitekt.network.packet.UpdateContainerFullPacket
import com.runetopic.xlitekt.network.packet.VarpSmallPacket
import com.runetopic.xlitekt.shared.packInterface

/**
 * @author Tyler Telis
 */
class Interfaces(
    private val player: Player,
    private val interfaces: MutableList<UserInterface> = mutableListOf()
) : MutableList<UserInterface> by interfaces {
    val listeners = mutableListOf<UserInterfaceListener>()

    var currentInterfaceLayout = InterfaceLayout.FIXED

    fun login() {
        openTop(currentInterfaceLayout.interfaceId)
        gameInterfaces.forEach(::openInterface)
        player.write(VarpSmallPacket(1737, -1)) // TODO TEMP until i write a var system
        player.message("Welcome to Xlitekt.")
    }

    fun closeModal() {
        val openModal = interfaces.findLast { it.interfaceInfo.resizableChildId == MODAL_CHILD_ID } ?: return
        this -= openModal
    }

    fun closeInventory() {
        val openInventory = interfaces.findLast { it.interfaceInfo.resizableChildId == INVENTORY_CHILD_ID } ?: return
        this -= openInventory
    }

    private fun modalOpen() = interfaces.findLast { it.interfaceInfo.resizableChildId == MODAL_CHILD_ID } != null
    private fun inventoryOpen() = interfaces.findLast { it.interfaceInfo.resizableChildId == INVENTORY_CHILD_ID } != null

    private fun UserInterface.isModal() = interfaceInfo.resizableChildId == MODAL_CHILD_ID
    private fun UserInterface.isInventory() = interfaceInfo.resizableChildId == INVENTORY_CHILD_ID

    operator fun plusAssign(userInterface: UserInterface) {
        if (modalOpen() && userInterface.isModal()) closeModal()
        if (inventoryOpen() && userInterface.isInventory()) closeInventory()
        this.add(userInterface)
        openInterface(userInterface)
    }

    operator fun minusAssign(userInterface: UserInterface) {
        this.remove(userInterface)
        closeInterface(userInterface)
    }

    fun switchLayout(toLayout: InterfaceLayout) {
        if (toLayout == currentInterfaceLayout) return
        openTop(toLayout.interfaceId)
        if (modalOpen()) closeModal()
        gameInterfaces.forEach { moveSub(it, toLayout) }
        currentInterfaceLayout = toLayout
    }

    fun setText(packedInterface: Int, text: String) = player.write(
        IfSetTextPacket(
            packedInterface = packedInterface,
            text = text
        )
    )

    fun setEvent(packedInterface: Int, ifEvent: UserInterfaceEvent.IfEvent) = player.write(
        IfSetEventsPacket(
            packedInterface = packedInterface,
            fromSlot = ifEvent.slots.first,
            toSlot = ifEvent.slots.last,
            event = ifEvent.event.value
        )
    )

    fun setContainerUpdateFull(containerKey: Int, interfaceId: Int, childId: Int = 65536, items: List<Item?>) {
        player.write(
            UpdateContainerFullPacket(
                packedInterface = interfaceId.packInterface(childId),
                containerKey = containerKey,
                items = items
            )
        )
    }

    private fun openTop(id: Int) = player.write(IfOpenTopPacket(interfaceId = id))

    private fun openInterface(userInterface: UserInterface) = userInterface.apply {
        interfaces.add(userInterface)

        val derivedChildId = userInterface.interfaceInfo.resizableChildId
        val childId = derivedChildId.enumChildForLayout(
            currentInterfaceLayout
        )

        val listener = addInterfaceListener(this, player)

        listeners += listener

        listener.init(
            UserInterfaceEvent.InitEvent(
                interfaceId = userInterface.interfaceInfo.id,
            )
        )

        player.write(
            IfOpenSubPacket(
                interfaceId = interfaceInfo.id,
                toPackedInterface = currentInterfaceLayout.interfaceId.packInterface(childId),
                alwaysOpen = true
            )
        )

        listener.open(
            UserInterfaceEvent.OpenEvent(
                interfaceId = userInterface.interfaceInfo.id,
            )
        )
    }

    private fun closeInterface(userInterface: UserInterface) = userInterface.apply {
        interfaces.remove(userInterface)

        val childId = userInterface.interfaceInfo.resizableChildId

        player.write(
            IfCloseSubPacket(
                packedInterface = currentInterfaceLayout.interfaceId.packInterface(childId.enumChildForLayout(currentInterfaceLayout))
            )
        )

        val listener = listeners.find { listener -> listener.userInterface == this } ?: return@apply

        listener.close(
            UserInterfaceEvent.CloseEvent(
                interfaceId = userInterface.interfaceInfo.id,
            )
        )

        listeners.remove(listener)
    }

    private fun moveSub(userInterface: UserInterface, toLayout: InterfaceLayout) = userInterface.apply {
        val derivedChildId = userInterface.interfaceInfo.resizableChildId
        val fromChildId = derivedChildId.enumChildForLayout(currentInterfaceLayout)
        val toChildId = derivedChildId.enumChildForLayout(toLayout)
        val listener = listeners.find { listener -> listener.userInterface == this }

        listener?.init(
            UserInterfaceEvent.InitEvent(
                interfaceId = userInterface.interfaceInfo.id,
            )
        )

        player.write(
            IfMoveSubPacket(
                fromPackedInterface = currentInterfaceLayout.interfaceId.packInterface(fromChildId),
                toPackedInterface = toLayout.interfaceId.packInterface(toChildId)
            )
        )
        listener?.open(
            UserInterfaceEvent.OpenEvent(
                interfaceId = userInterface.interfaceInfo.id,
            )
        )
    }

    private fun Int.enumChildForLayout(layout: InterfaceLayout): Int =
        entryType<EnumEntryType>(layout.enumId)
            ?.params
            ?.entries
            ?.find { it.key == InterfaceLayout.RESIZABLE.interfaceId.packInterface(this) }
            ?.value as Int and 0xffff

    private companion object {
        val gameInterfaces = setOf(
            UserInterface.AccountManagement,
            UserInterface.Settings,
            UserInterface.Inventory,
            UserInterface.MiniMap,
            UserInterface.ChatBox,
            UserInterface.Logout,
            UserInterface.Emotes,
            UserInterface.Magic,
            UserInterface.MusicPlayer,
            UserInterface.Skills,
            UserInterface.WornEquipment,
            UserInterface.Friends,
            UserInterface.Prayer,
            UserInterface.CombatOptions,
            UserInterface.CharacterSummary,
            UserInterface.UnknownOverlay,
            UserInterface.ChatChannel
        )

        const val MODAL_CHILD_ID = 16
        const val INVENTORY_CHILD_ID = 73
    }
}
