package cspro2sql;

import cspro2sql.bean.Dictionary;
import cspro2sql.reader.DictionaryReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class StatsEngine {

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        try (InputStream in = StatsEngine.class.getResourceAsStream("/database.properties")) {
            prop.load(in);
        } catch (IOException ex) {
            return;
        }
        try {
            Dictionary dictionary = DictionaryReader.read(
                    prop.getProperty("dictionary.filename"),
                    prop.getProperty("db.dest.table.prefix"));
            execute(dictionary, prop);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    static boolean execute(Dictionary dictionary, Properties prop) {
        String schema = prop.getProperty("db.dest.schema");
        Connection connDst = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            //Connect to the destination database
            connDst = DriverManager.getConnection(
                    prop.getProperty("db.dest.uri") + "/" + prop.getProperty("db.dest.schema") + "?autoReconnect=true&useSSL=false",
                    prop.getProperty("db.dest.username"),
                    prop.getProperty("db.dest.password"));
            connDst.setAutoCommit(false);

            Statement stmtDst = connDst.createStatement();

            for (String template : MonitorEngine.TEMPLATES) {
                stmtDst.executeUpdate("TRUNCATE " + schema + ".m" + template);
                stmtDst.executeQuery("SELECT @ID := 0");
                stmtDst.executeUpdate("INSERT INTO " + schema + ".m" + template + " SELECT @ID := @ID + 1 ID, " + template + ".* FROM " + schema + "." + template);
                connDst.commit();
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            try {
                if (connDst != null) {
                    connDst.rollback();
                }
            } catch (SQLException ex1) {
                System.out.println("Rollback failure");
                return false;
            }
            System.out.println("Database exception (" + ex.getMessage() + ")");
        } finally {
            try {
                if (connDst != null) {
                    connDst.close();
                }
            } catch (SQLException ex) {
                System.out.println("Impossible to close the db conenction");
                return false;
            }
        }
        return true;
    }

}
