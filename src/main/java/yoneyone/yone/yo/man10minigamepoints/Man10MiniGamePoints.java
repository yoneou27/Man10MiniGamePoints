package yoneyone.yone.yo.man10minigamepoints;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Man10MiniGamePoints extends JavaPlugin {

    boolean isOpenExchange;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Man10MiniGameGUI(this),this);
        isOpenExchange = getConfig().getBoolean("openexchange");
        Man10MiniGameDateBase dateBase = new Man10MiniGameDateBase(this);
        dateBase.reloadGame_Index();
        dateBase.reloadExchange_Items();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0){
            commandHelpMessage(sender);
            return true;
        }
        Man10MiniGameDateBase dateBase = new Man10MiniGameDateBase(this);
        if (args[0].equals("get")){
            if (!(sender instanceof Player)){
                sender.sendMessage("§4Execute this command from the player");
                return true;
            }
            Player player = (Player) sender;
            if (args.length == 1){//全表示（page=1)
                dateBase.DBOperationUserGet(player);
            }else if (args.length == 2) {
                int page;
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    dateBase.DBOperationUserGet(player, args[1]);
                    return true;
                }
                if (page > 0) {
                    dateBase.DBOperationUserGet(player, page);//数字ならば全表示
                }else {
                    player.sendMessage("§4Use a number greater than 1");
                }
            }
            return true;
        }
        if (args[0].equals("exchange")){
            if (!(sender instanceof Player)){
                commandHelpMessage(sender);
                return true;
            }
            Player player = (Player) sender;
            if (!isOpenExchange){
                player.sendMessage("§4The exchange is currently closed");
                return true;
            }
            Man10MiniGameGUI GUI = new Man10MiniGameGUI(this);
            GUI.openExchangeMenu(player,1);
            return true;
        }
        //ここから権限必須
        if (!sender.hasPermission("red.man10.mgscore.admin")){
            sender.sendMessage("§4§lYou do not have permission");
            return true;
        }
        if (args[0].equals("open")){
            getConfig().set("openexchange",true);
            saveConfig();
            reloadConfig();
            isOpenExchange = getConfig().getBoolean("openexchange");
            sender.sendMessage("§f§lopened");
            return true;
        }else if (args[0].equals("close")){
            getConfig().set("openexchange",false);
            saveConfig();
            reloadConfig();
            isOpenExchange = getConfig().getBoolean("openexchange");
            sender.sendMessage("§f§lclosed");
            return true;
        }
        if (args.length == 1){
            commandHelpMessage(sender);
            return true;
        }
        /*//テスト用
        if (args[0].equals("test")){
            if (!(sender instanceof Player)){
                sender.sendMessage("このコマンドはプレイヤーから実行してください");
                return true;
            }
            Player senderPleyer = (Player) sender;
            PlayerInventory inventory = senderPleyer.getInventory();
            ItemStack itemStack = new ItemStack(Material.SKULL_ITEM);
            net.minecraft.server.v1_12_R1.ItemStack itemStack2 = CraftItemStack.asNMSCopy(itemStack);
            itemStack2.setData(3);
            NBTTagCompound compound = (itemStack2.hasTag()) ? itemStack2.getTag() : new NBTTagCompound();
            if (compound == null){
                senderPleyer.sendMessage("§7There was an error, please try again");
                return true;
            }
            compound.set("SkullOwner",new NBTTagString(args[1]));
            itemStack2.setTag(compound);
            itemStack = CraftItemStack.asBukkitCopy(itemStack2);
            inventory.addItem(itemStack);
            senderPleyer.sendMessage("§eヘッドをインベントリに追加しました");
            return true;
        }
         */
        if (args.length <= 2){
            commandHelpMessage(sender);
            return true;
        }
        if (args[0].equals("register")){//登録
            dateBase.DBOperationGameSet(sender,args[1],args[2]);
            return true;
        }
        if (args.length <= 3){
            commandHelpMessage(sender);
            return true;
        }
        if (args[0].equals("item")){
            if (!(sender instanceof Player)){
                sender.sendMessage("§4Execute this command from the player");
                return true;
            }
            Player player = (Player) sender;
            int point;
            try {
                point = Integer.parseInt(args[3]);
            }catch (NumberFormatException e) {
                player.sendMessage("§4The score is invalid");
                return true;
            }
            if (point <= 0){
                sender.sendMessage("§4Use a number greater than 1");
                return true;
            }
            dateBase.DBOperationExchangeSet(player,args[2],args[1],point);//ゲームキー、アイテム名のキー、必要ポイント
            return true;
        }
        Player player = Bukkit.getServer().getPlayer(args[2]);
        int scoreInt;
        try {
            scoreInt = Integer.parseInt(args[3]);
        }catch (NumberFormatException e){
            player.sendMessage("§4The score is invalid");
            return true;
        }
        if (scoreInt <= 0){
            sender.sendMessage("§4Use a number greater than 1");
            return true;
        }
        if (player == null){
            sender.sendMessage("§4That player doesn't exist");
            return true;
        }
        switch (args[0]) {//追加、剝奪、設定
            case "add":
                dateBase.DBOperationUser(player, scoreInt, args[1], sender, "add");
                break;
            case "take":
                dateBase.DBOperationUser(player, scoreInt, args[1], sender, "take");
                break;
            case "set":
                dateBase.DBOperationUser(player, scoreInt, args[1], sender, "set");
                break;
            default:
                sender.sendMessage("§4Use add, take, or set arguments");
                sender.sendMessage("§4Check with /mgscore for details");
                break;
        }
        return true;
    }
    private void commandHelpMessage(CommandSender sender){
        sender.sendMessage("§eCommand usage:/mgscore get [game name|page]");
        sender.sendMessage("§eCommand usage:/mgscore exchange");
        if (sender.hasPermission("red.man10.mgscore.admin")){
            sender.sendMessage("§5Command usage:/mgscore <add|take|set> <game key> <user name> <score>");
            sender.sendMessage("§5Command usage:/mgscore register <game name> <key>");
            sender.sendMessage("§5Command usage:/mgscore <open|close>");
            sender.sendMessage("§5Command usage:/mgscore item <game key> <item name> <point>");
        }
    }
}
