package red.man10.man10commerce

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10commerce.Utility.sendMsg
import red.man10.man10commerce.data.ItemData
import red.man10.man10commerce.menu.CommerceMenu
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Man10Commerce : JavaPlugin() {

    companion object{
        lateinit var plugin: JavaPlugin
        val es : ExecutorService = Executors.newCachedThreadPool()

        const val prefix = "§l[§a§lA§d§lma§f§ln§a§lzon§f§l]"

        var enable = true

        var minPrice : Double =  10.0
    }

    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()

        plugin = this
        ItemData.loadItemIndex()

        ItemData.fee = config.getDouble("fee")
        minPrice = config.getDouble("minPrice")
        enable = config.getBoolean("enable")

        server.pluginManager.registerEvents(CommerceMenu,this)

    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (args.isNullOrEmpty()){

            if (!sender.hasPermission("commerce.user"))return false

            if (!sender.hasPermission("commerce.op") && !enable){
                sendMsg(sender,"§f現在営業を停止しています")

                return false
            }

            CommerceMenu.openMainMenu(sender)

            return true
        }

        when(args[0]){

            "on" ->{
                if (!sender.hasPermission("commerce.op"))return true

                enable = true
                config.set("enable", enable)
                saveConfig()
            }

            "off" ->{
                if (!sender.hasPermission("commerce.op"))return true

                enable = false
                config.set("enable", enable)
                saveConfig()

            }

            "sell" ->{//mnc sell price
                if (!sender.hasPermission("commerce.user"))return true

                if (!sender.hasPermission("commerce.op") && !enable){
                    sendMsg(sender,"§f現在営業を停止しています")

                    return false
                }

                if (args.size != 2)return false

                val item = sender.inventory.itemInMainHand

                if (item.type == Material.AIR){ return true }

                val price = args[1].toDoubleOrNull()

                if (price == null){

                    sendMsg(sender,"§c§l金額は数字を使ってください！")

                    return true
                }

                if (price< minPrice){
                    sendMsg(sender,"§c§l${minPrice}円以下での出品はできません！")
                    return true
                }

                es.execute {
                    if (ItemData.sell(sender,item,price)){
                        sendMsg(sender,"§e§l出品成功しました！")
                    }
                }

            }

        }

        return false
    }
}