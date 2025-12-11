package com.myatm.service;

import com.myatm.model.Transaction;
import com.myatm.model.User;

import java.io.FileWriter;
import java.sql.*;

public class ATMService {

     public static final long TIMEOUT = 60_000;

    private User user;

    public ATMService(User user) {
        this.user = user;
    }

    // Citire sold direct din DB (nu din memorie)
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

    // DEPUNERE – UPDATE + INSERT în DB
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

    // RETRAGERE – UPDATE + INSERT în DB
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

    // ISTORIC – SELECT din DB
    public void afiseazaIstoric() {
        Bank bank = new Bank();
        var tranzactii = bank.getUserTransactions(user.getId());

        if (tranzactii.isEmpty()) {
            System.out.println("Nu există tranzacții încă.");
            return;
        }

        System.out.println("===== ISTORIC TRANZACȚII =====");

        for (Transaction t : tranzactii) {
            System.out.println(t.getDate() + " | " + t.getType() + " | " + t.getAmount() + " RON");
        }
    }

    public void transfer(String destUsername, double suma) {
        if (suma <= 0) {
            throw new InvalidAmountException("Suma trebuie să fie mai mare decât zero!");
        }

        if (destUsername.equals(user.getUsername())) {
            throw new InvalidAmountException("Nu poți transfera bani către tine însuți!");
        }

        Bank bank = new Bank();
        User destUser = bank.findByUsername(destUsername);

        if (destUser == null) {
            throw new InvalidAmountException("Destinatar inexistent!");
        }

        // Actualizăm soldul sursei înainte de verificare
        afiseazaSold();
        if (suma > user.getBalance()) {
            throw new InvalidAmountException("Fonduri insuficiente pentru transfer!");
        }

        String updateSrcSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
        String updateDstSql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        String insertSrcSql = "INSERT INTO transactions (user_id, type, amount, peer_user_id) " +
                "VALUES (?, 'TRANSFER_OUT', ?, ?)";
        String insertDstSql = "INSERT INTO transactions (user_id, type, amount, peer_user_id) " +
                "VALUES (?, 'TRANSFER_IN', ?, ?)";

        try (Connection conn = Database.getConnection()) {

            conn.setAutoCommit(false);

            // 1) scădem de la sursă
            try (PreparedStatement st = conn.prepareStatement(updateSrcSql)) {
                st.setDouble(1, suma);
                st.setInt(2, user.getId());
                st.executeUpdate();
            }

            // 2) adăugăm la destinatar
            try (PreparedStatement st = conn.prepareStatement(updateDstSql)) {
                st.setDouble(1, suma);
                st.setInt(2, destUser.getId());
                st.executeUpdate();
            }

            // 3) tranzacție pentru sursă
            try (PreparedStatement st = conn.prepareStatement(insertSrcSql)) {
                st.setInt(1, user.getId());
                st.setDouble(2, suma);
                st.setInt(3, destUser.getId());
                st.executeUpdate();
            }

            // 4) tranzacție pentru destinatar
            try (PreparedStatement st = conn.prepareStatement(insertDstSql)) {
                st.setInt(1, destUser.getId());
                st.setDouble(2, suma);
                st.setInt(3, user.getId());
                st.executeUpdate();
            }

            conn.commit();

            // updatăm și în memorie
            user.setBalance(user.getBalance() - suma);
            System.out.println("Transfer reușit: " + suma + " RON către " + destUsername);

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la transfer: " + e.getMessage());
        }
    }

    public void exportCSV() {
    String fileName = "history_" + user.getUsername() + ".csv";

    String sql = "SELECT created_at, type, amount FROM transactions WHERE user_id = ? ORDER BY created_at DESC";

    try (Connection conn = Database.getConnection();
         PreparedStatement st = conn.prepareStatement(sql);
         FileWriter writer = new FileWriter(fileName)) {

        st.setInt(1, user.getId());
        ResultSet rs = st.executeQuery();

        // Scriem headerul CSV
        writer.write("date,type,amount\n");

        boolean found = false;

        while (rs.next()) {
            found = true;

            String date = rs.getTimestamp("created_at").toString();
            String type = rs.getString("type");
            double amount = rs.getDouble("amount");

            writer.write(date + "," + type + "," + amount + "\n");
        }

        if (!found) {
            System.out.println("Nu există tranzacții pentru export.");
            return;
        }

        System.out.println("Fișier generat: " + fileName);

    } catch (Exception e) {
        throw new RuntimeException("Eroare la export CSV: " + e.getMessage());
    }
}



}
