package com.myatm.service;

import com.myatm.model.Transaction;
import com.myatm.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

public class Bank {

    public Bank() {
        // Nu mai încărcăm useri din fișier
    }

    public boolean createUser(String username, String pin) {

    // verificăm dacă există deja username în DB
    String checkSql = "SELECT id FROM users WHERE username = ?";

    try (Connection conn = Database.getConnection();
         PreparedStatement check = conn.prepareStatement(checkSql)) {

        check.setString(1, username);
        ResultSet rs = check.executeQuery();

        if (rs.next()) {
            return false; // username deja există
        }
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }

    // Hash-uim PIN-ul
    String hashedPin = BCrypt.hashpw(pin, BCrypt.gensalt());

    // Inserăm userul nou
    String insertSql = "INSERT INTO users (username, pin, balance) VALUES (?, ?, 0)";

    try (Connection conn = Database.getConnection();
         PreparedStatement st = conn.prepareStatement(insertSql)) {

        st.setString(1, username);
        st.setString(2, hashedPin);
        st.executeUpdate();

        return true;

    } catch (SQLException e) {
        throw new RuntimeException("Eroare la crearea utilizatorului: " + e.getMessage());
    }
}


    public User authenticate(String username, String pin) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, username);
            ResultSet rs = st.executeQuery();

            if (!rs.next()) {
                // user inexistent
                return null;
            }

            int id = rs.getInt("id");
            String dbUsername = rs.getString("username");
            String hashedPin = rs.getString("pin");
            double balance = rs.getDouble("balance");
            int failedAttempts = rs.getInt("failed_attempts");
            boolean locked = rs.getBoolean("locked");

            //  dacă e deja blocat → aruncăm excepție
            if (locked) {
                throw new AccountLockedException("Contul este blocat. Contactează banca.");
            }

            //  verificăm PIN-ul cu BCrypt
            boolean pinOk = BCrypt.checkpw(pin, hashedPin);

            if (!pinOk) {
                // creștem numărul de încercări greșite
                int newFailed = failedAttempts + 1;

                String updateSql = "UPDATE users SET failed_attempts = ?, locked = ? WHERE id = ?";

                try (PreparedStatement up = conn.prepareStatement(updateSql)) {
                    boolean shouldLock = newFailed >= 3;
                    up.setInt(1, newFailed);
                    up.setBoolean(2, shouldLock);
                    up.setInt(3, id);
                    up.executeUpdate();
                }

                if (newFailed >= 3) {
                    throw new AccountLockedException("Cont blocat după prea multe încercări de PIN!");
                }

                return null; // PIN greșit, dar contul nu e încă blocat
            }

            //  PIN corect → resetăm numărul de încercări greșite dacă e nevoie
            if (failedAttempts > 0) {
                String resetSql = "UPDATE users SET failed_attempts = 0 WHERE id = ?";
                try (PreparedStatement up = conn.prepareStatement(resetSql)) {
                    up.setInt(1, id);
                    up.executeUpdate();
                }
            }

            //  returnăm user-ul valid
            return new User(id, dbUsername, hashedPin, balance);

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la autentificare: " + e.getMessage());
        }
    }

    // Încărcăm soldul și tranzacțiile userului
    public List<Transaction> getUserTransactions(int userId) {
        List<Transaction> list = new ArrayList<>();

        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = Database.getConnection();
                PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, userId);
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                list.add(new Transaction(
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("type"),
                        rs.getDouble("amount")));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la încărcarea tranzacțiilor: " + e.getMessage());
        }

        return list;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
                PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, username);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("pin"),
                        rs.getDouble("balance"));
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la căutarea utilizatorului: " + e.getMessage());
        }
    }

}
