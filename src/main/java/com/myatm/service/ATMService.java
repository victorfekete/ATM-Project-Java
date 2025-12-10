package com.myatm.service;

import com.myatm.model.User;

import java.sql.*;


public class ATMService {

    private User user;

    public ATMService(User user) {
        this.user = user;
    }

    //  Citire sold direct din DB (nu din memorie)
    public void afiseazaSold() {
        String sql = "SELECT balance FROM users WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, user.getId());
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                user.setBalance(balance); // sincronizam memoria cu DB
                System.out.println("Sold curent: " + balance + " RON");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la citirea soldului: " + e.getMessage());
        }
    }

    //  DEPUNERE – UPDATE + INSERT în DB
    public void depune(double suma) {
        if (suma <= 0) {
            throw new InvalidAmountException("Suma trebuie să fie mai mare decât zero!");
        }

        String updateSql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        String insertSql = "INSERT INTO transactions (user_id, type, amount, created_at) VALUES (?, 'DEPOSIT', ?, NOW())";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            // UPDATE balance
            try (PreparedStatement st = conn.prepareStatement(updateSql)) {
                st.setDouble(1, suma);
                st.setInt(2, user.getId());
                st.executeUpdate();
            }

            // INSERT transaction
            try (PreparedStatement st = conn.prepareStatement(insertSql)) {
                st.setInt(1, user.getId());
                st.setDouble(2, suma);
                st.executeUpdate();
            }

            conn.commit();
            user.setBalance(user.getBalance() + suma);

            System.out.println("Depunere reușită: " + suma + " RON");

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la depunere: " + e.getMessage());
        }
    }

    //  RETRAGERE – UPDATE + INSERT în DB
    public void retrage(double suma) {
        if (suma <= 0) {
            throw new InvalidAmountException("Suma invalida!");
        }

        afiseazaSold(); // actualizează soldul real din DB

        if (suma > user.getBalance()) {
            throw new InvalidAmountException("Fonduri insuficiente!");
        }

        String updateSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
        String insertSql = "INSERT INTO transactions (user_id, type, amount, created_at) VALUES (?, 'WITHDRAW', ?, NOW())";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            // UPDATE balance
            try (PreparedStatement st = conn.prepareStatement(updateSql)) {
                st.setDouble(1, suma);
                st.setInt(2, user.getId());
                st.executeUpdate();
            }

            // INSERT transaction
            try (PreparedStatement st = conn.prepareStatement(insertSql)) {
                st.setInt(1, user.getId());
                st.setDouble(2, suma);
                st.executeUpdate();
            }

            conn.commit();
            user.setBalance(user.getBalance() - suma);

            System.out.println("Ai retras: " + suma + " RON");

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la retragere: " + e.getMessage());
        }
    }

    //  ISTORIC – SELECT din DB
    public void afiseazaIstoric() {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, user.getId());
            ResultSet rs = st.executeQuery();

            System.out.println("===== ISTORIC TRANZACȚII =====");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(
                    rs.getTimestamp("created_at") + " | " +
                    rs.getString("type") + " | " +
                    rs.getDouble("amount") + " RON"
                );
            }

            if (!found) {
                System.out.println("Nu există tranzacții încă.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la citirea istoricului: " + e.getMessage());
        }
    }
}
