package red.man10.man10commerce.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import red.man10.man10commerce.Man10Commerce.Companion.OP
import red.man10.man10commerce.Man10Commerce.Companion.es
import red.man10.man10commerce.Man10Commerce.Companion.plugin
import red.man10.man10commerce.Man10Commerce.Companion.prefix
import red.man10.man10commerce.Utility.format
import red.man10.man10commerce.Utility.sendMsg
import red.man10.man10commerce.data.ItemData
import red.man10.man10commerce.data.ItemData.itemDictionary
import red.man10.man10commerce.data.ItemData.opOrderMap
import red.man10.man10commerce.data.ItemData.orderMap
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

object CommerceMenu : Listener{

    private val playerMenuMap = ConcurrentHashMap<Player,String>()
    private val pageMap = ConcurrentHashMap<Player,Int>()

    private const val ITEM_MENU = "§${prefix}§l出品中のアイテム一覧"
    private const val SELL_MENU = "§${prefix}§l出品したアイテム"
    private const val MAIN_MENU = "§${prefix}§lメニュー"
    private const val BASIC_MENU = "${prefix}§d§lAmanzonBasic"

    fun openMainMenu(p:Player){

        val inv = Bukkit.createInventory(null,9, MAIN_MENU)

        val showItem = ItemStack(Material.GRASS_BLOCK)
        val shouItemMeta = showItem.itemMeta
        shouItemMeta.setDisplayName("§a§l出品されているアイテムをみる")
        setID(shouItemMeta,"ItemMenu")
        showItem.itemMeta = shouItemMeta

        val basic = ItemStack(Material.DIAMOND)
        val basicMeta = basic.itemMeta
        basicMeta.setDisplayName("§a§lAmazonBasic")
        basicMeta.lore = mutableListOf("§f運営が販売しているアイテムを買うことができます")
        setID(basicMeta,"Basic")
        basic.itemMeta = basicMeta

        val sellItem = ItemStack(Material.CHEST)
        val sellItemMeta = sellItem.itemMeta
        sellItemMeta.setDisplayName("§a§l出品したアイテムを確かめる")
        setID(sellItemMeta,"SellMenu")
        sellItem.itemMeta = sellItemMeta

        val selling = ItemStack(Material.COBBLESTONE)
        val sellingMeta = selling.itemMeta
        sellingMeta.setDisplayName("§e§lアイテムを出品する")
        setID(sellingMeta,"Selling")
        selling.itemMeta = sellingMeta

        inv.setItem(1,showItem)
        inv.setItem(3,basic)
        inv.setItem(5,sellItem)
        inv.setItem(7,selling)

        p.openInventory(inv)
        playerMenuMap[p] = MAIN_MENU
    }

    //自分が出品したアイテムを確認する
    private fun openSellItemMenu(p:Player, seller: UUID,page: Int){

        if (p.uniqueId!=seller &&!p.hasPermission("commerce.op")){ return }

        val list = ItemData.sellList(seller)

        val inv = Bukkit.createInventory(null,54, SELL_MENU)

        var inc = 0

        while (inv.getItem(44) == null){

            if (list.size <= inc+page*45)break

            val data = list[inc+page*45]

            inc ++

            val item = itemDictionary[data.itemID]?.clone()?:continue

            val lore = item.lore?: mutableListOf()

            lore.add("§e§l値段:${format(data.price)}")
            lore.add("§e§l個数:${data.amount}")
            lore.add("§e§l${SimpleDateFormat("yyyy-MM/dd").format(data.date)}")
            lore.add("§c§lシフトクリックで出品を取り下げる")

            item.lore = lore

            val meta = item.itemMeta

            meta.persistentDataContainer.set(NamespacedKey(plugin,"order_id"), PersistentDataType.INTEGER,data.id)

            item.itemMeta = meta

            inv.addItem(item)

        }

        if (page!=0){

            val prevItem = ItemStack(Material.PAPER)
            val prevMeta = prevItem.itemMeta
            prevMeta.setDisplayName("§§l前ページへ")
            setID(prevMeta,"prev")

            prevItem.itemMeta = prevMeta

            inv.setItem(45,prevItem)

        }

        if (inc >=44){
            val nextItem = ItemStack(Material.PAPER)
            val nextMeta = nextItem.itemMeta
            nextMeta.setDisplayName("§§l次ページへ")

            setID(nextMeta,"next")

            nextItem.itemMeta = nextMeta

            inv.setItem(53,nextItem)

        }

        Bukkit.getScheduler().runTask(plugin, Runnable {
            p.openInventory(inv)
            playerMenuMap[p] = SELL_MENU
            pageMap[p] = page
        })

    }

    //出品アイテム一覧を見る
    private fun openItemMenu(p:Player, page:Int){

        val inv = Bukkit.createInventory(null,54, ITEM_MENU)

        val keys = orderMap.keys().toList()

        var inc = 0

        while (inv.getItem(44) ==null){

            if (keys.size <= inc+page*45)break

            val itemID = keys[inc+page*45]

            inc ++

            val data = orderMap[itemID]
            val item = itemDictionary[itemID]?.clone()?:continue

            val lore = item.lore?: mutableListOf()

            if (data==null){

                lore.add("§c§l売り切れ")

                item.lore = lore

                inv.addItem(item)
                continue
            }

            lore.add("§e§l値段:${format(floor(data.price))}")
            lore.add("§e§l単価:${format(floor(data.price/data.amount))}")
            lore.add("§e§l出品者${Bukkit.getOfflinePlayer(data.seller!!).name}")
            lore.add("§e§l個数:${data.amount}")
            lore.add("§e§l${SimpleDateFormat("yyyy-MM/dd").format(data.date)}")
            if (data.isOp) lore.add("§d§l公式出品アイテム")
            lore.add("§cシフトクリックで1-Click購入")

            val meta = item.itemMeta
            meta.persistentDataContainer.set(NamespacedKey(plugin,"order_id"), PersistentDataType.INTEGER,data.id)
            meta.persistentDataContainer.set(NamespacedKey(plugin,"item_id"), PersistentDataType.INTEGER,data.itemID)
            item.itemMeta = meta

            item.lore = lore

            inv.addItem(item)

        }

        val reloadItem = ItemStack(Material.COMPASS)
        val reloadMeta = reloadItem.itemMeta
        reloadMeta.setDisplayName("§6§lリロード")
        setID(reloadMeta,"reload")
        reloadItem.itemMeta = reloadMeta
        inv.setItem(49,reloadItem)


        if (page!=0){

            val prevItem = ItemStack(Material.PAPER)
            val prevMeta = prevItem.itemMeta
            prevMeta.setDisplayName("§§l前ページへ")
            setID(prevMeta,"prev")

            prevItem.itemMeta = prevMeta

            inv.setItem(45,prevItem)

        }

        if (inc >=44){
            val nextItem = ItemStack(Material.PAPER)
            val nextMeta = nextItem.itemMeta
            nextMeta.setDisplayName("§§l次ページへ")

            setID(nextMeta,"next")

            nextItem.itemMeta = nextMeta

            inv.setItem(53,nextItem)

        }

        p.openInventory(inv)
        playerMenuMap[p] = ITEM_MENU
        pageMap[p] = page

    }

    //Amanzon Basic
    private fun openOPMenu(p:Player, page:Int){

        val inv = Bukkit.createInventory(null,54, BASIC_MENU)

        val keys = opOrderMap.keys().toList()

        var inc = 0

        while (inv.getItem(44) ==null){

            if (keys.size <= inc+page*45)break

            val itemID = keys[inc+page*45]

            inc ++

            val data = orderMap[itemID]
            val item = itemDictionary[itemID]?.clone()?:continue

            val lore = item.lore?: mutableListOf()

            if (data==null){

                lore.add("§c§l売り切れ")

                item.lore = lore

                inv.addItem(item)
                continue
            }

            lore.add("§e§l値段:${format(floor(data.price))}")
            lore.add("§e§l単価:${format(floor(data.price/data.amount))}")
            lore.add("§e§l出品者${Bukkit.getOfflinePlayer(data.seller!!).name}")
            lore.add("§e§l個数:${data.amount}")
            lore.add("§e§l${SimpleDateFormat("yyyy-MM/dd").format(data.date)}")
            if (data.isOp) lore.add("§d§l公式出品アイテム")
            lore.add("§cシフトクリックで1-Click購入")

            val meta = item.itemMeta
            meta.persistentDataContainer.set(NamespacedKey(plugin,"order_id"), PersistentDataType.INTEGER,data.id)
            meta.persistentDataContainer.set(NamespacedKey(plugin,"item_id"), PersistentDataType.INTEGER,data.itemID)
            item.itemMeta = meta

            item.lore = lore

            inv.addItem(item)

        }

        val reloadItem = ItemStack(Material.COMPASS)
        val reloadMeta = reloadItem.itemMeta
        reloadMeta.setDisplayName("§6§lリロード")
        setID(reloadMeta,"reload")
        reloadItem.itemMeta = reloadMeta
        inv.setItem(49,reloadItem)


        if (page!=0){

            val prevItem = ItemStack(Material.PAPER)
            val prevMeta = prevItem.itemMeta
            prevMeta.setDisplayName("§§l前ページへ")
            setID(prevMeta,"prev")

            prevItem.itemMeta = prevMeta

            inv.setItem(45,prevItem)

        }

        if (inc >=44){
            val nextItem = ItemStack(Material.PAPER)
            val nextMeta = nextItem.itemMeta
            nextMeta.setDisplayName("§§l次ページへ")

            setID(nextMeta,"next")

            nextItem.itemMeta = nextMeta

            inv.setItem(53,nextItem)

        }


        p.openInventory(inv)
        playerMenuMap[p] = BASIC_MENU
        pageMap[p] = page

    }

    private fun setID(meta:ItemMeta, value:String){
        meta.persistentDataContainer.set(NamespacedKey(plugin,"id"), PersistentDataType.STRING,value)
    }

    private fun getID(itemStack: ItemStack):String{
        return itemStack.itemMeta?.persistentDataContainer?.get(NamespacedKey(plugin,"id"), PersistentDataType.STRING)
            ?:""
    }

    ////////////////////////////////////////////////////

    @EventHandler
    fun inventoryClick(e:InventoryClickEvent){

        val p = e.whoClicked as Player

        val menuName = playerMenuMap[p]?:return

        e.isCancelled = true

        val item = e.currentItem?:return
        val action = e.action
        val id = getID(item)

        when(menuName){

            ITEM_MENU ->{

                val page = pageMap[p]?:0

                when(id){
                    "prev" ->{ openItemMenu(p,page-1) }

                    "next" ->{ openItemMenu(p,page+1) }

                    "reload" ->{ openItemMenu(p,page) }

                    else ->{

                        val meta = item.itemMeta?:return

                        val orderID = meta.persistentDataContainer[NamespacedKey(plugin,"order_id"), PersistentDataType.INTEGER]?:-1
                        val itemID = meta.persistentDataContainer[NamespacedKey(plugin,"item_id"), PersistentDataType.INTEGER]?:-1

                        if (orderID == -1)return

                        if (p.hasPermission(OP) && action == InventoryAction.CLONE_STACK){
                            ItemData.close(orderID,p)
                            sendMsg(p,"§c§l出品を取り下げました")
                            Bukkit.getScheduler().runTask(plugin, Runnable { openItemMenu(p,page) })
                            return
                        }

                        if (action != InventoryAction.MOVE_TO_OTHER_INVENTORY)return

                        es.execute {

                            when(val ret = ItemData.buy(p,itemID,orderID)){
                                0 -> { sendMsg(p,"§c§l購入失敗！電子マネーが足りません！") }
                                1 -> {sendMsg(p,"§a§l購入成功！")}
                                else ->{ sendMsg(p,"エラー:${ret} サーバー運営者、GMに報告してください")}
                            }
//                            if (ItemData.buy(p,itemID,orderID)){
//                                sendMsg(p,"§a§l購入成功！")
//                            }else{
//                                sendMsg(p,"§c§l購入失敗!銀行にお金がないか、売り切れています！")
//                            }

                            Bukkit.getScheduler().runTask(plugin, Runnable { openItemMenu(p,page) })
                        }

                        return
                    }
                }
            }

            SELL_MENU ->{

                if (action != InventoryAction.MOVE_TO_OTHER_INVENTORY)return

                val page = pageMap[p]?:0

                val meta = item.itemMeta!!
                val orderID = meta.persistentDataContainer[NamespacedKey(plugin,"order_id"), PersistentDataType.INTEGER]?:0

                es.execute {

                    if (id=="prev"){
                        openSellItemMenu(p,p.uniqueId,page-1)
                        return@execute
                    }

                    if (id=="next"){
                        openSellItemMenu(p,p.uniqueId,page+1)
                        return@execute
                    }

                    if (ItemData.close(orderID,p)){
                        sendMsg(p,"出品を取り下げました")
                        openSellItemMenu(p,p.uniqueId,page)
                    }
                }

                return
            }

            MAIN_MENU ->{

                when(id){
                    "ItemMenu" -> openItemMenu(p,0)
                    "Basic" -> openOPMenu(p,0)
                    "SellMenu" -> es.execute { openSellItemMenu(p,p.uniqueId,0) }
                    "Selling"    -> {
                        p.closeInventory()
                        p.sendMessage(Component.text("${prefix}§a§n売るアイテムを手に持って、/amsell <金額> を入力してください")
                            .clickEvent(ClickEvent.suggestCommand("/amsell ")))
                    }
                }

            }

            BASIC_MENU ->{

                val page = pageMap[p]?:0

                when(id){
                    "prev" ->{ openOPMenu(p,page-1) }

                    "next" ->{ openOPMenu(p,page+1) }

                    "reload" ->{ openOPMenu(p,page) }

                    else ->{
                        if (action != InventoryAction.MOVE_TO_OTHER_INVENTORY)return

                        val meta = item.itemMeta!!

                        val orderID = meta.persistentDataContainer[NamespacedKey(plugin,"order_id"), PersistentDataType.INTEGER]?:-1
                        val itemID = meta.persistentDataContainer[NamespacedKey(plugin,"item_id"), PersistentDataType.INTEGER]?:-1

                        if (orderID == -1)return

                        es.execute {

                            when(val ret = ItemData.buy(p,itemID,orderID)){
                                0 -> { sendMsg(p,"§c§l購入失敗！電子マネーが足りません！") }
                                1 -> {sendMsg(p,"§a§l購入成功！")}
                                else ->{ sendMsg(p,"エラー:${ret} サーバー運営者、GMに報告してください")}
                            }

                            Bukkit.getScheduler().runTask(plugin, Runnable { openOPMenu(p,page) })
                        }

                        return
                    }
                }

            }
        }

    }

    @EventHandler
    fun closeInventory(e:InventoryCloseEvent){

        val p = e.player

        if (playerMenuMap.containsKey(p)) playerMenuMap.remove(p)
        if (pageMap.containsKey(p)) pageMap.remove(p)
    }
}