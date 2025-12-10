package com.myatm.service;

import com.myatm.model.User;
import java.sql.*;

public class Bank {

    public Bank() {
        // Nu mai încărcăm useri din fișier
    }

    public User authenticate(String username, String pin) {
        String sql = "SELECT * FROM users WHERE username = ? AND pin = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, username);
            st.setString(2, pin);

            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("pin"),
                        rs.getDouble("balance")
                );
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Eroare la autentificare: " + e.getMessage());
        }
    }
}
