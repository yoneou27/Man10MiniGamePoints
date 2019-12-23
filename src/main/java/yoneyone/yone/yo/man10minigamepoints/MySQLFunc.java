package yoneyone.yone.yo.man10minigamepoints; /**
 * Created by takatronix on 2017/03/05.
 */

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class MySQLFunc {
    String HOST = null;
    String DB = null;
    String USER = null;
    String PASS = null;
    String PORT = null;
    private Connection con = null;

    public MySQLFunc(String host, String db, String user, String pass,String port) {
        this.HOST = host;
        this.DB = db;
        this.USER = user;
        this.PASS = pass;
        this.PORT = port;
    }

    public Connection open() {
        try {
            //Bukkit.getLogger().info("データベースの準備を始めています…");
            Class.forName("com.mysql.jdbc.Driver");
            //Bukkit.getLogger().info("ロード完了！");
            this.con = DriverManager.getConnection("jdbc:mysql://" + this.HOST + ":" + this.PORT +"/" + this.DB + "?useSSL=false", this.USER, this.PASS );
            //Bukkit.getLogger().info("データベース接続完了！");
            return this.con;
        } catch (SQLException var2) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not connect to MySQL server, error code: " + var2.getErrorCode());
            Bukkit.getLogger().log(Level.SEVERE, "§4§l[Man10MiniGamePoints]データベースに接続できませんでした！！！");
        } catch (ClassNotFoundException var3) {
            Bukkit.getLogger().log(Level.SEVERE, "JDBC driver was not found in this machine.");
        }
        return this.con;
    }

    public boolean checkConnection() {
        return this.con != null;
    }

    public void close(Connection c) {
        c = null;
    }

    public Connection getCon() {
        return this.con;
    }

    public void setCon(Connection con) {
        this.con = con;
    }
}