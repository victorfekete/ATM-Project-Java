package com.myatm.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/myatm";
    private static final String USER = "root";
    private static final String PASSWORD = "Umangozaur23";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Nu mă pot conecta la baza de date!", e);
        }
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✔ Conexiune reușită la MySQL!");
        } catch (Exception e) {
            System.out.println("❌ Conexiune eșuată!");
            e.printStackTrace();
        }
    }
}
