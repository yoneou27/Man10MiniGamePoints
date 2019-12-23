package yoneyone.yone.yo.man10minigamepoints;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * Created by takatronix on 2017/03/05.
 */


public class MySQLManager {

    public  Boolean debugMode = false;
    private JavaPlugin plugin;
    private String HOST = null;
    private String DB = null;
    private String USER = null;
    private String PASS = null;
    private String PORT = null;
    boolean connected = false;
    private Statement st = null;
    private Connection con = null;
    private String conName;
    private MySQLFunc MySQL;

    ////////////////////////////////
    //      コンストラクタ
    ////////////////////////////////
    public MySQLManager(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.conName = name;
        this.connected = false;
        loadConfig();
        this.connected = Connect(HOST, DB, USER, PASS,PORT);
        //ここにテーブル作成など
        execute("CREATE TABLE if not exists `user_score` (\n" +
                "\t`id` INT(11) NOT NULL AUTO_INCREMENT,\n" +
                "\t`uuid` VARCHAR(40) NOT NULL DEFAULT '',\n" +
                "\t`game_id` INT(11) NOT NULL DEFAULT '0',\n" +
                "\t`player` VARCHAR(32) NOT NULL DEFAULT '',\n" +
                "\t`score` INT(11) NULL DEFAULT '0',\n" +
                "\tPRIMARY KEY (`id`),\n" +
                "\tINDEX `name` (`player`),\n" +
                "\tINDEX `uuid` (`uuid`),\n" +
                "\tINDEX `game_id` (`game_id`)\n" +
                ")\n" +
                "COLLATE='utf8mb4_0900_ai_ci'\n" +
                "ENGINE=InnoDB\n" +
                "AUTO_INCREMENT=1\n" +
                ";\n");
        execute("CREATE TABLE if not exists `game_index` (\n" +
                "\t`id` INT(11) NOT NULL AUTO_INCREMENT COMMENT 'ユニークキー',\n" +
                "\t`name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT 'ゲームネーム',\n" +
                "\t`key` VARCHAR(50) NULL DEFAULT NULL,\n" +
                "\tPRIMARY KEY (`id`),\n" +
                "\tUNIQUE INDEX `name` (`name`),\n" +
                "\tUNIQUE INDEX `key` (`key`)\n" +
                ")\n" +
                "COLLATE='utf8mb4_0900_ai_ci'\n" +
                "ENGINE=InnoDB\n" +
                "AUTO_INCREMENT=1\n" +
                ";\n");
        execute("CREATE TABLE　if not exists `exchange_items` (\n" +
                "\t`id` INT(11) NOT NULL AUTO_INCREMENT,\n" +
                "\t`item` TEXT NOT NULL,\n" +
                "\t`name` VARCHAR(50) NOT NULL,\n" +
                "\t`point` INT(11) NOT NULL DEFAULT '0',\n" +
                "\t`game_key` VARCHAR(50) NOT NULL,\n" +
                "\tPRIMARY KEY (`name`),\n" +
                "\tINDEX `id` (`id`)\n" +
                ")\n" +
                "COLLATE='utf8mb4_0900_ai_ci'\n" +
                "ENGINE=InnoDB\n" +
                "AUTO_INCREMENT=1\n" +
                ";\n");
        if(!this.connected) {
            plugin.getLogger().info("Unable to establish a MySQL connection.");
        }
    }

    /////////////////////////////////
    //       設定ファイル読み込み
    /////////////////////////////////
    public void loadConfig(){
        //   plugin.getLogger().info("MYSQL Config loading");
        plugin.reloadConfig();
        HOST = plugin.getConfig().getString("mysql.host");
        USER = plugin.getConfig().getString("mysql.user");
        PASS = plugin.getConfig().getString("mysql.pass");
        PORT = plugin.getConfig().getString("mysql.port");
        DB = plugin.getConfig().getString("mysql.db");
        //  plugin.getLogger().info("Config loaded");

    }

    public void commit(){
        try{
            this.con.commit();
        }catch (Exception e){

        }
    }

    ////////////////////////////////
    //   connect
    ////////////////////////////////
    public Boolean Connect(String host, String db, String user, String pass,String port) {
        this.HOST = host;
        this.DB = db;
        this.USER = user;
        this.PASS = pass;
        this.MySQL = new MySQLFunc(host, db, user, pass,port);
        this.con = this.MySQL.open();
        if(this.con == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return false;
        }

        try {
            this.st = this.con.createStatement();
            this.connected = true;
            this.plugin.getLogger().info("[" + this.conName + "] Connected to the database.");
        } catch (SQLException var6) {
            this.connected = false;
            this.plugin.getLogger().info("[" + this.conName + "] Could not connect to the database.");
        }

        this.MySQL.close(this.con);
        return Boolean.valueOf(this.connected);
    }

    ////////////////////////////////
    //     counting rows
    ////////////////////////////////
    public int countRows(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT * FROM %s", new Object[]{table}));

        try {
            while(set.next()) {
                ++count;
            }
        } catch (SQLException var5) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
        }

        return count;
    }
    ////////////////////////////////
    //     counting recode
    ////////////////////////////////
    public int count(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT count(*) from %s", table));

        try {
            count = set.getInt("count(*)");

        } catch (SQLException var5) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
            return -1;
        }

        return count;
    }
    ////////////////////////////////
    //      execute
    ////////////////////////////////
    public boolean execute(String query) {
        this.MySQL = new MySQLFunc(this.HOST, this.DB, this.USER, this.PASS,this.PORT);
        this.con = this.MySQL.open();
        if(this.con == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return false;
        }
        boolean ret = true;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            this.st = this.con.createStatement();
            this.st.execute(query);
        } catch (SQLException var3) {
            this.plugin.getLogger().info("[" + this.conName + "] Error executing statement: " +var3.getErrorCode() +":"+ var3.getLocalizedMessage());
            this.plugin.getLogger().info(query);
            ret = false;

        }

        this.close();
        return ret;
    }

    ////////////////////////////////
    //      query
    ////////////////////////////////
    public ResultSet query(String query) {
        this.MySQL = new  MySQLFunc(this.HOST, this.DB, this.USER, this.PASS,this.PORT);
        this.con = this.MySQL.open();
        ResultSet rs = null;
        if(this.con == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return rs;
        }

        if (debugMode){
            plugin.getLogger().info("[DEBUG] query:" + query);
        }

        try {
            this.st = this.con.createStatement();
            rs = this.st.executeQuery(query);
        } catch (SQLException var4) {
            this.plugin.getLogger().info("[" + this.conName + "] Error executing query: " + var4.getErrorCode());
            this.plugin.getLogger().info(query);
        }

//        this.close();

        return rs;
    }
/*
    public String escape(String before){
        StringBuilder after = new StringBuilder();
        String esSymbol = "\\";
        char es1 = '0';
        char es2 = '\'':
        char es3 = '"';
        char es4 = 'b';
        char es5 = 'n';
        char es6 = 'r';
        char es7 = 't';
        char es8 = 'z';
        char es9 = '\\';
        char es10 = '%';
        char es11 = '_';
        for (int i = 0; i < before.length(); i++) {
            if (before.charAt(i) == es1 || before.charAt(i) == es2 || before.charAt(i) == es3
                    || before.charAt(i) == es4 || before.charAt(i) == es5 || before.charAt(i) == es6
                    || before.charAt(i) == es7 || before.charAt(i) == es8 || before.charAt(i) == es9
                    || before.charAt(i) == es10 || before.charAt(i) == es11) {
                after.append(esSymbol);
                after.append(before.charAt(i));
                continue;
            }
            after.append(before.charAt(i));
        }
        return after.toString();
    }
*/

    public void close(){

        try {
            this.st.close();
            this.con.close();
            this.MySQL.close(this.con);

        } catch (SQLException var4) {
        }catch (NullPointerException ignored){
        }

    }
}