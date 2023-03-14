package red.man10.man10commerce.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import kotlin.collections.HashMap

open class MenuFramework(val p:Player,menuSize: Int, title: String) {

    var menu : Inventory
    private var closeAction : OnCloseListener? = null
    private var clickAction : Button.OnClickListener? = null

    init {
        menu = Bukkit.createInventory(null,menuSize, text(title))
    }

    companion object{
        private val menuMap = HashMap<UUID, MenuFramework>()
        private val menuStack = HashMap<UUID,Stack<MenuFramework>>()

        const val CHEST_SIZE = 27
        const val LARGE_CHEST_SIZE= 54

        fun set(p:Player,menu: MenuFramework){
            menuMap[p.uniqueId] = menu
        }

        fun get(p:Player): MenuFramework?{
            return menuMap[p.uniqueId]
        }

        fun delete(p:Player){
            menuMap.remove(p.uniqueId)
        }

    }

    fun open(){
        p.closeInventory()
        set(p,this)
        p.openInventory(menu)
    }

    //slotは0スタート
    fun setButton(button: Button, slot:Int){
        menu.setItem(slot,button.icon())
    }

    //背景として全埋めする
    fun fill(button: Button){
        for (i in 0 until menu.size){
            setButton(button,i)
        }
    }

    fun setCloseListener(action: OnCloseListener){
        closeAction = action
    }

    fun setClickListener(action: Button.OnClickListener){
        clickAction = action
    }

    fun close(e:InventoryCloseEvent){
        delete(e.player as Player)
        closeAction?.closeAction(e)
    }

    fun interface OnCloseListener{
        fun closeAction(e: InventoryCloseEvent)
    }

    //      スタックに追加
    fun push(){
        val stack = menuStack[p.uniqueId]?: Stack()
        stack.push(this)
        menuStack[p.uniqueId] = stack
    }

    //      スタックの取り出し
    fun pop():MenuFramework?{
        val stack = menuStack[p.uniqueId]
        if (stack.isNullOrEmpty())return null
        val menu = stack.pop()
        menuStack[p.uniqueId] = stack
        return menu
    }

    class Button(icon:Material):Cloneable{

        private var buttonItem : ItemStack
        private var actionData : OnClickListener? = null
        private val key = UUID.randomUUID().toString()

        init {
            buttonItem = ItemStack(icon)
            val meta = buttonItem.itemMeta
            meta.persistentDataContainer.set(NamespacedKey.fromString("key")!!
                , PersistentDataType.STRING,key)
            buttonItem.itemMeta = meta
        }

        companion object{

            private val buttonMap = HashMap<String, Button>()

            fun set(button: Button){
                buttonMap[button.key] = button
            }

            fun get(item:ItemStack): Button?{

                if (!item.hasItemMeta())return null

                val meta = item.itemMeta
                val key = meta.persistentDataContainer[NamespacedKey.fromString("key")!!, PersistentDataType.STRING]?:return null

                return buttonMap[key]
            }
        }

        fun fromItemStack(item:ItemStack): Button {
            buttonItem = item.clone()
            val meta = buttonItem.itemMeta
            meta.persistentDataContainer.set(NamespacedKey.fromString("key")!!
                , PersistentDataType.STRING,key)
            buttonItem.itemMeta = meta
            set(this)
            return this
        }

        fun title(text:String): Button {
            val meta = buttonItem.itemMeta
            meta.displayName(text(text))
            buttonItem.itemMeta = meta
            set(this)
            return this
        }

        fun cmd(int:Int): Button {
            val meta = buttonItem.itemMeta
            meta.setCustomModelData(int)
            buttonItem.itemMeta = meta
            set(this)
            return this
        }

        fun lore(lore:List<String>): Button {
            val loreComponent = mutableListOf<Component>()
            lore.forEach { loreComponent.add(text(it)) }

            val meta = buttonItem.itemMeta
            meta.lore(loreComponent)
            buttonItem.itemMeta = meta
            set(this)
            return this

        }

        fun setClickAction(action: OnClickListener): Button {
            actionData = action
            set(this)
            return this
        }

        fun enchant(boolean: Boolean): Button {

            val meta = buttonItem.itemMeta

            if (!boolean){
                meta.enchants.forEach { meta.removeEnchant(it.key) }
                buttonItem.itemMeta = meta
                set(this)
                return this
            }

            meta.addEnchant(Enchantment.LUCK,1,false)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            buttonItem.itemMeta = meta
            set(this)
            return this
        }


        fun click(e:InventoryClickEvent){
            actionData?.action(e)
        }

        fun icon():ItemStack{
            return buttonItem
        }

        public override fun clone(): Button {
            return super.clone() as Button
        }

        fun interface OnClickListener{
            fun action(e:InventoryClickEvent)
        }
    }

    object MenuListener:Listener{

        @EventHandler
        fun clickEvent(e:InventoryClickEvent){

            val p = e.whoClicked

            if (p !is Player)return

            val menu = get(p) ?:return

            menu.clickAction?.action(e)

            val item = e.currentItem?:return
            val data = Button.get(item) ?:return
            e.isCancelled = true

            data.click(e)
        }

        @EventHandler(priority = EventPriority.LOW)
        fun closeEvent(e:InventoryCloseEvent){

            if (e.player !is Player)return
            val menu = get(e.player as Player) ?:return
            menu.close(e)
        }

    }
}

