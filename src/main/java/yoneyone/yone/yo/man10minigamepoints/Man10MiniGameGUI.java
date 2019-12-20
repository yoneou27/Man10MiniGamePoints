package yoneyone.yone.yo.man10minigamepoints;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Man10MiniGameGUI implements Listener {
    JavaPlugin plugin;
    public Man10MiniGameGUI(JavaPlugin plugin){
        this.plugin = plugin;
    }
    public void openExchangeMenu(Player player,int page){
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> {
            List<String> gameNames = new ArrayList<>();
            List<game_index> games = Man10MiniGameDates.games;
            for (game_index game:games){
                gameNames.add(game.name);
            }
            menuGUIOpen(gameNames,player,page);
        });
    }
    private void menuGUIOpen(List<String> gameNames,Player player,int page){
        if (page <= 0){
            page = 1;
        }
        int allPages = gameNames.size() / 18 + 1;
        if (allPages < page){
            page = allPages;
        }
        int openPage = page;
        Bukkit.getScheduler().runTask(this.plugin,() -> {
            Inventory inv = Bukkit.createInventory(null,27,"§e§lポイント交換所メニュー");
            Iterator<String> iterator = gameNames.iterator();
            for (int i = 1; i < (allPages + 1); i++) {
                if (openPage != i){
                    for (int l = 0;l < 18;l++){
                        iterator.next();
                    }
                    continue;
                }
                for (int l = 0; l < 18; l++) {
                    if (iterator.hasNext()) {
                        String gameName = iterator.next();
                        ItemStack itemStack = new ItemStack(Material.CHEST);
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName("§e交換所:" + gameName);
                        itemStack.setItemMeta(itemMeta);
                        inv.setItem(l,itemStack);
                    }
                }
                break;
            }

            ItemStack buck = new ItemStack(Material.APPLE);
            ItemMeta buckItemMeta = buck.getItemMeta();
            buckItemMeta.setDisplayName("§f§l前ページへ");
            buck.setItemMeta(buckItemMeta);
            inv.setItem(18,buck);

            ItemStack pageItem = new ItemStack(Material.BEACON);
            ItemMeta pageMeta = pageItem.getItemMeta();
            pageMeta.setDisplayName(String.valueOf(openPage));
            pageItem.setItemMeta(pageMeta);
            inv.setItem(22,pageItem);

            ItemStack next = new ItemStack(Material.APPLE);
            ItemMeta nextItemMeta = next.getItemMeta();
            nextItemMeta.setDisplayName("§f§l次ページへ");
            next.setItemMeta(nextItemMeta);

            inv.setItem(26,next);
            player.openInventory(inv);
        });
    }
    private void openExchangeCheck(Player player,String gameName){
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> {
            String key = null;
            List<game_index> games = Man10MiniGameDates.games;
            for (game_index game:games){
                if (game.name.equals(gameName)){
                    key = game.key;
                    break;
                }
            }
            if (key == null){
                return;
            }
            List<exchange_items> goDate = new ArrayList<>();
            List<exchange_items> itemsList = Man10MiniGameDates.items;
            for (exchange_items item:itemsList){
                if (item.game_key.equals(key)){
                    goDate.add(item);
                }
            }
            openExchange(player,goDate);
        });
    }
    private void openExchange(Player player,List<exchange_items> items){
        Bukkit.getScheduler().runTask(this.plugin,() -> {
            Inventory inv = Bukkit.createInventory(null,54,"§e§lポイント交換所");
            for (exchange_items item:items){
                int point = item.point;
                String game = item.game_key;
                String name = item.name;
                try {
                    ItemStack[] itemStacks = itemStackArrayFromBase64(item.item);
                    ItemStack itemStack = itemStacks[0];
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    List<String> lore = itemMeta.getLore();
                    if (lore == null) {
                        lore = new ArrayList<>();
                    }
                    lore.add("§f§l必要ポイント:"+ point);
                    lore.add("§f§lGame:"+ game);
                    lore.add("§f§lKey name:"+ name);
                    itemMeta.setLore(lore);
                    itemStack.setItemMeta(itemMeta);
                    inv.addItem(itemStack);
                } catch (IOException e) {
                    player.sendMessage("§4An error has occurred");
                    return;
                }
            }
            ItemStack itemStack = new ItemStack(Material.APPLE);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName("§f§lメニューへ");
            itemStack.setItemMeta(itemMeta);
            inv.setItem(53,itemStack);
            player.openInventory(inv);
        });
    }
    private void checkExchangeGUI(Player player,int point,String game_key,String name){
        String itemString = "";
        for (exchange_items item:Man10MiniGameDates.items){
            if (item.game_key.equals(game_key) && item.name.equals(name) && item.point == point){
                itemString = item.item;
                break;
            }
        }
        if (itemString.equals("")) {
            return;
        }
        String finalItemString = itemString;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> exchange(player, finalItemString,point,game_key));
    }
    synchronized private void exchange(Player player,String item,int point,String game_key){
        player.sendMessage("§b§l交換処理を開始しています§kxxxxx");
        MySQLManager SQL = new MySQLManager(this.plugin,"Man10Score");
        int gameID = -1;
        //ゲームidを取得
        try (ResultSet date = SQL.query("SELECT * FROM game_index WHERE `key` = '"+ game_key +"';")){
            if (date.next()){
                gameID = date.getInt("id");
            }
        }catch (SQLException e) {
            player.sendMessage("§4An error has occurred");
            SQL.close();
            return;
        }
        if (gameID == -1){//見つからなければ-1
            player.sendMessage("§4The game doesn't exist");
            SQL.close();
            return;
        }
        int score;
        try (ResultSet date = SQL.query("SELECT * FROM user_score WHERE uuid = '" + player.getUniqueId().toString() + "' AND game_id = "+ gameID +";")){
            if (!date.next()){
                player.sendMessage("§4§lこのゲームのポイントのデータがありません");
                return;
            }
            score = date.getInt("score");
            if (score < point){
                player.sendMessage("§4§lスコアが足りません");
                return;
            }
        }catch (SQLException e){
            player.sendMessage("§4§4An error has occurred");
            return;
        }
        ItemStack[] itemStacks;
        try {
            itemStacks = itemStackArrayFromBase64(item);
        } catch (IOException e) {
            player.sendMessage("§4§4An error has occurred");
            return;
        }
        int result = score - point;
        if (!SQL.execute("UPDATE user_score SET player = '"+ player.getName() +"', score = "+ result +" WHERE uuid = '"+ player.getUniqueId().toString() +"';")){
            player.sendMessage("§4§4An error has occurred");
            return;
        }
        player.getInventory().addItem(itemStacks);
        player.sendMessage("§b§lアイテムを付与しました");
    }
    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent event){
        String title = event.getView().getTitle();
        if (title.equals("§e§lポイント交換所メニュー")){
            event.setCancelled(true);
            if (!checkItemAndPlayer(event)){
                return;
            }
            int slot = event.getSlot();
            String itemName = event.getView().getItem(slot).getItemMeta().getDisplayName();
            Player player = (Player) event.getView().getPlayer();
            if (itemName != null) {
                if (itemName.equals("§f§l前ページへ")) {
                    int page = Integer.parseInt(event.getView().getItem(22).getItemMeta().getDisplayName());
                    page -= 1;
                    openExchangeMenu(player, page);
                    return;
                }
                if (itemName.equals("§f§l次ページへ")) {
                    int page = Integer.parseInt(event.getView().getItem(22).getItemMeta().getDisplayName());
                    page += 1;
                    openExchangeMenu(player, page);
                    return;
                }
                String[] gameNames = itemName.split(":");
                if (gameNames.length == 1) {
                    return;
                }
                String gameName = gameNames[1];
                openExchangeCheck(player, gameName);
            }
        }else if (title.equals("§e§lポイント交換所")){
            event.setCancelled(true);
            if (!checkItemAndPlayer(event)){
                return;
            }
            int slot = event.getSlot();
            String itemName = event.getView().getItem(slot).getItemMeta().getDisplayName();
            Player player = (Player) event.getView().getPlayer();
            if (itemName != null) {
                if (itemName.equals("§f§lメニューへ")) {
                    openExchangeMenu(player, 1);
                    return;
                }
            }
            if (event.getView().getItem(slot).getItemMeta().getLore() == null) return;
            List<String> lore = event.getView().getItem(slot).getItemMeta().getLore();
            if (lore.size() <= 2) return;
            String splitPoint = lore.get(lore.size() - 3);
            String splitGame_key = lore.get(lore.size() - 2);
            String splitName = lore.get(lore.size() - 1);
            String[] Points = splitPoint.split(":");//§f§l必要ポイント:
            String[] game_keys = splitGame_key.split(":");//§f§lGame:
            String[] names = splitName.split(":");//§f§lKey name:
            if (Points.length == 1 || game_keys.length == 1 || names.length == 1){
                return;
            }
            String gamePointString = Points[1];
            String game_key = game_keys[1];
            String name = names[1];
            int gamePoint;
            try {
                gamePoint = Integer.parseInt(gamePointString);
            }catch (NumberFormatException e){
                return;
            }
            checkExchangeGUI(player,gamePoint,game_key,name);
            event.getView().close();
        }
    }

    private boolean checkItemAndPlayer(InventoryClickEvent event){
        int slot = event.getSlot();
        if (event.getView() == null) return false;
        if (event.getView().getItem(slot) == null) return false;
        if (event.getView().getItem(slot).getItemMeta() == null) return false;
        return event.getView().getPlayer() instanceof Player;
    }

    /////////////////////////
    //以下たかさんからのコピペ
    /////////////////////////
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
