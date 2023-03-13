package red.man10.man10commerce.menu

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.man10commerce.Man10Commerce
import red.man10.man10commerce.Utility
import red.man10.man10commerce.data.ItemDataOld
import red.man10.man10commerce.data.Transaction

class AllItemMenu(p:Player) :ListMenu("§l出品中のアイテム一覧",p){

    override fun open() {

        val keys = ItemDataOld.orderMap.keys().toList()

        listInventory(keys)

        p.openInventory(menu)

        pushStack()
    }

    override fun click(e: InventoryClickEvent, menu: Menu, id: String, item: ItemStack) {

        if (id != ""){
            CategorizedMenu(p,id).open()
            return
        }

        val meta = item.itemMeta?:return

        val orderID = meta.persistentDataContainer[NamespacedKey(Man10Commerce.plugin,"order_id"), PersistentDataType.INTEGER]?:-1
        val itemID = meta.persistentDataContainer[NamespacedKey(Man10Commerce.plugin,"item_id"), PersistentDataType.INTEGER]?:-1

        if (orderID == -1)return

        //マウスホイールクリックで取り下げ
        if (p.hasPermission(Man10Commerce.OP) && e.action == InventoryAction.CLONE_STACK){
            Transaction.close(p,orderID)
            Bukkit.getScheduler().runTask(Man10Commerce.plugin, Runnable { menu.open() })
            return
        }

        if (e.action != InventoryAction.MOVE_TO_OTHER_INVENTORY){
            Man10Commerce.es.execute { OneItemList(p,itemID).open() }
            return
        }

        Transaction.buy(p,itemID, orderID) {
            Bukkit.getScheduler().runTask(Man10Commerce.plugin, Runnable { menu.open() })
        }

        return


    }
}