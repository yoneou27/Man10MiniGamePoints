package yoneyone.yone.yo.man10minigamepoints;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class Man10MiniGameDateBase {
    private JavaPlugin plugin;
    public Man10MiniGameDateBase(JavaPlugin plugin){
        this.plugin = plugin;
    }

    public void DBOperationUser(Player player, int score, String gameName, CommandSender sender, String mode){
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> {
            MySQLManager SQL = new MySQLManager(this.plugin,"Man10Score");
            int gameID = -1;
            String gameName2 = "";
            //ゲームidを取得
            try (ResultSet date = SQL.query("SELECT * FROM game_index WHERE `key` = '"+ gameName +"';")){
                if (date.next()){
                    gameID = date.getInt("id");
                    gameName2 = date.getString("name");
                }
            }catch (SQLException e) {
                sender.sendMessage("§4An error has occurred");
                SQL.close();
                return;
            }
            if (gameID == -1){//見つからなければ-1
                sender.sendMessage("§4The game doesn't exist");
                SQL.close();
                return;
            }
            //取得完了
            try (ResultSet date = SQL.query("SELECT * FROM user_score WHERE uuid = '" + player.getUniqueId().toString() + "' AND game_id = "+ gameID +";")) {
                int oldUserScore = 0;
                if (!date.next()) {
                    SQL.execute("INSERT INTO user_score (uuid, game_id) VALUES ('"+ player.getUniqueId().toString() +"', "+ gameID +");");
                }else {
                    oldUserScore = date.getInt("score");
                }
                int newUserScore;
                switch (mode) {
                    case "add":
                        newUserScore = oldUserScore + score;
                        break;
                    case "take":
                        newUserScore = oldUserScore - score;
                        break;
                    case "set":
                        newUserScore = score;
                        break;
                    default:
                        sender.sendMessage("§4An error has occurred");
                        SQL.close();
                        return;
                }
                SQL.execute("UPDATE user_score SET player = '"+ player.getName() +"', score = "+ newUserScore +" WHERE uuid = '"+ player.getUniqueId().toString() +"';");
            } catch (SQLException e) {
                sender.sendMessage("§4An error has occurred");
                SQL.close();
                return;
            }
            switch (mode) {
                case "add":
                    player.sendMessage("§a§l" + gameName2 + "のスコアを" + score + "付与しました");
                    sender.sendMessage("§a§l" + gameName2 + "の" + player.getName() + "のスコアを" + score + "付与しました");
                    break;
                case "take":
                    player.sendMessage("§4§l" + gameName2 + "のスコアを" + score + "剝奪しました");
                    sender.sendMessage("§4§l" + gameName2 + "の" + player.getName() + "のスコアを" + score + "剝奪しました");
                    break;
                case "set":
                    player.sendMessage("§e§l" + gameName2 + "のスコアを" + score + "にしました");
                    sender.sendMessage("§e§l" + gameName2 + "の" + player.getName() + "のスコアを" + score + "にしました");
                    break;
            }
            SQL.close();
        });
    }
    public void DBOperationUserGet(Player player){
        DBOperationUserGet(player,1);//1で再呼び出し
    }
    public void DBOperationUserGet(Player player,String gameName){
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> {
            if (gameName.contains(":") || gameName.contains("'")){
                player.sendMessage("§4The key name is invalid, : and ' cannot be used");
                return;
            }
            MySQLManager SQL = new MySQLManager(this.plugin, "Man10Score");
            int gameID = -1;
            String gameName2 = "";
            //ゲームidを取得
            try (ResultSet date = SQL.query("SELECT * FROM game_index WHERE `key` = '" + gameName + "';")) {
                if (date.next()) {
                    gameID = date.getInt("id");
                    gameName2 = date.getString("name");
                }
            } catch (SQLException e) {
                player.sendMessage("§4An error has occurred");
                SQL.close();
                return;
            }
            if (gameID == -1) {//見つからなければ-1
                player.sendMessage("§4The game doesn't exist");
                SQL.close();
                return;
            }
            DBOperationUserGetSub(player, gameID, gameName2);
            SQL.close();
        });
    }
    public void DBOperationUserGet(Player player,int page){
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> {
            MySQLManager SQL = new MySQLManager(this.plugin, "Man10Score");
            try (ResultSet date = SQL.query("SELECT * FROM game_index;")) {
                int num = 0;
                while (date.next()){
                    num++;
                }
                int pageNum = num / 5;
                pageNum++;
                if (pageNum < page){
                    player.sendMessage("§4The page doesn't exist, Exists up to "+ pageNum +" pages");
                    SQL.close();
                    return;
                }
                date.beforeFirst();
                for (int i = 1;i < pageNum + 1;i++){//何ページ目か
                    if (i != page){//指定のページ以外なら
                        for (int l = 0;l < 5;l++){
                            date.next();
                        }
                        continue;
                    }
                    player.sendMessage("§e§lミニゲームスコア一覧");
                    for (int l = 0;l < 5;l++){
                        if (date.next()){
                            DBOperationUserGetSub(player,date.getInt("id"),date.getString("name"));
                        }else {
                            break;
                        }
                    }
                    player.sendMessage("§e§lP"+ page +"/"+ pageNum);
                    break;
                }
            } catch (SQLException e) {
                player.sendMessage("§4An error has occurred");
                SQL.close();
                return;
            }
            SQL.close();
        });
    }
    //効率化のためサブメソッド
    private void DBOperationUserGetSub(Player player,int gameID,String gameName){
        MySQLManager SQL = new MySQLManager(this.plugin, "Man10Score");
        try (ResultSet date = SQL.query("SELECT * FROM user_score WHERE uuid = '"+ player.getUniqueId().toString() +"' AND game_id = "+ gameID +";")){
            if (!date.next()){
                player.sendMessage("§e"+ gameName +":データがありません");
                SQL.close();
                return;
            }
            player.sendMessage("§e"+ gameName +":"+ date.getInt("score"));
        }catch (SQLException e){
            player.sendMessage("§4An error has occurred");
        }
        SQL.close();
    }

    public void DBOperationGameSet(CommandSender sender,String gameName, String key){
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> {
            if (gameName.contains(":") || gameName.contains("'")){
                sender.sendMessage("§4The game name is invalid, : and ' cannot be used");
                return;
            }
            boolean isSetOK = true;
            List<game_index> games = Man10MiniGameDates.games;
            for (game_index game : games) {
                if (game.name.equals(gameName) || game.key.equals(key)) {
                    isSetOK = false;
                    break;
                }
            }
            if (!isSetOK){
                sender.sendMessage("§4The name of the minigame or key is already registered");
                return;
            }
            MySQLManager SQL = new MySQLManager(this.plugin,"Man10Score");
            SQL.execute("INSERT INTO game_index (`name`, `key`) VALUES ('"+ gameName +"', '"+ key +"');");
            sender.sendMessage("§eRegistered a mini game");
            SQL.close();
            reloadGame_Index();
        });
    }

    public void DBOperationExchangeSet(Player player,String itemName,String gameKey,int point){
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin,() -> {
            if (itemName.contains(":") || itemName.contains("'")){
                player.sendMessage("§4The item name is invalid, : and ' cannot be used");
                return;
            }
            List<game_index> games = Man10MiniGameDates.games;
            Iterator<game_index> iteratorGame = games.iterator();
            boolean isGameOK = false;
            while (iteratorGame.hasNext()){
                game_index game = iteratorGame.next();
                if (game.key.equals(gameKey)){
                    isGameOK = true;
                    break;
                }
            }
            if (!isGameOK){
                player.sendMessage("§4The game doesn't exist");
                return;
            }
            List<exchange_items> items = Man10MiniGameDates.items;
            Iterator<exchange_items> iteratorItems = items.iterator();
            boolean isItemOK = true;
            while (iteratorItems.hasNext()){
                exchange_items item = iteratorItems.next();
                if (item.name.equals(itemName)){
                    if (item.game_key.equals(gameKey)) {
                        isItemOK = false;
                        break;
                    }
                }
            }
            if (!isItemOK){
                player.sendMessage("§4The item name already exists");
                return;
            }
            MySQLManager SQL = new MySQLManager(this.plugin,"Man10Score");
            ItemStack itemStack = player.getInventory().getItem(player.getInventory().getHeldItemSlot());
            if (itemStack == null){
                player.sendMessage("§4You have no item in hand");
                return;
            }
            ItemStack[] itemStacks = new ItemStack[1];
            itemStacks[0] = itemStack;
            if (SQL.execute("INSERT INTO exchange_items (`item`, `name`, `point`, `game_key`) VALUES" +
                    " ('"+ itemStackArrayToBase64(itemStacks) +"', '"+ itemName +"', '"+ point +"', '"+ gameKey +"');")){
                player.sendMessage("§eItem registration completed");
            }else {
                player.sendMessage("§4There was a registration error");
            }
            SQL.close();
            reloadExchange_Items();
        });
    }
    //リロード
    public void reloadGame_Index(){
        MySQLManager SQL = new MySQLManager(this.plugin,"Man10Score");
        Man10MiniGameDates.games.clear();
        try (ResultSet date = SQL.query("SELECT * FROM game_index;")){
            while (date.next()){
                game_index game = new game_index();
                game.id = date.getInt("id");
                game.key = date.getString("key");
                game.name = date.getString("name");
                Man10MiniGameDates.games.add(game);
            }
        }catch (SQLException e){
            Bukkit.getLogger().info("ゲーム一覧のデータ読み込みに失敗しました");
        }
        SQL.close();
    }
    public void reloadExchange_Items(){
        MySQLManager SQL = new MySQLManager(this.plugin,"Man10Score");
        Man10MiniGameDates.items.clear();
        try (ResultSet date = SQL.query("SELECT * FROM exchange_items;")){
            while (date.next()){
                exchange_items exchange = new exchange_items();
                exchange.id = date.getInt("id");
                exchange.item = date.getString("item");
                exchange.name = date.getString("name");
                exchange.point = date.getInt("point");
                exchange.game_key = date.getString("game_key");
                Man10MiniGameDates.items.add(exchange);
            }
        }catch (SQLException e){
            Bukkit.getLogger().info("アイテム一覧のデータ読み込みに失敗しました");
        }
        SQL.close();
    }
    //以下たかさんのコピペ
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(items.length);

            // Save every element in the list
            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
}
