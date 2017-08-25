/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package normalformlearning;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/**
 *
 * @author Denis
 */
public final class SQLiteConnection{
    private Connection conn = null;
    public SQLiteConnection(){
        connect();
    }
    
    public void connect(){
        try{
            conn = DriverManager.getConnection("jdbc:sqlite:testdb.db");
            System.out.println("Connected");
        }catch(SQLException e){
            System.err.println(e.getMessage());
        }
    }
    
    public Connection getConnection(){
        return conn;
    }
}
