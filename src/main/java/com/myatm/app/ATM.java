package com.myatm.app;

import com.myatm.model.User;
import com.myatm.service.ATMService;
import com.myatm.service.AccountLockedException;
import com.myatm.service.Bank;
import com.myatm.service.Database;
import com.myatm.service.InvalidAmountException;
import com.myatm.utils.Input;

public class ATM {

    public static void main(String[] args) {

        Database.testConnection();
        Bank bank = new Bank();

        boolean running = true;

        while (running) {

            System.out.println("""
                    ===== MENIU PRINCIPAL =====
                    1. Login
                    2. Creează cont nou
                    3. Ieșire
                    """);

            int opt = Input.readInt("Alege opțiunea: ");

            switch (opt) {

                case 1 -> startATM(bank); // LOGIN + MENIU ATM

                case 2 -> createAccount(bank); // CREARE CONT

                case 3 -> {
                    System.out.println("La revedere!");
                    running = false;
                }

                default -> System.out.println("Opțiune invalidă!");
            }
        }
    }

    // ---------------- LOGIN + MENIU ATM ----------------

    private static void startATM(Bank bank) {

        String username = Input.readString("Username: ");
        String pin = Input.readString("PIN: ");

        User user = bank.authenticate(username, pin);

        try {
            user = bank.authenticate(username, pin);
        } catch (AccountLockedException e) {
            System.out.println("Eroare: " + e.getMessage());
            return; // nu mai intrăm în ATM
        }

        if (user == null) {
            System.out.println("Username sau PIN greșit!");
            return;
        }

        ATMService atm = new ATMService(user);
        boolean logged = true;

        long lastActivity = System.currentTimeMillis();

        while (logged) {

            long now = System.currentTimeMillis();
            if (now - lastActivity > ATMService.TIMEOUT) {
                System.out.println(" Sesiune închisă automat din cauza inactivității.");
                return;
            }
            
            System.out.println("""
                        ===== MINI ATM =====
                        1. Afișează sold
                        2. Depune bani
                        3. Retrage bani
                        4. Istoric
                        5. Transfer
                        6. Delogare
                        7. Export tranzactii in CSV
                    """);

            int optiune = Input.readInt("Alege opțiunea: ");

            lastActivity = System.currentTimeMillis();

            switch (optiune) {
                case 1 -> atm.afiseazaSold();

                case 2 -> {
                    try {
                        atm.depune(Input.readDouble("Introdu suma: "));
                    } catch (InvalidAmountException e) {
                        System.out.println("Eroare: " + e.getMessage());
                    }
                }

                case 3 -> {
                    try {
                        atm.retrage(Input.readDouble("Introdu suma: "));
                    } catch (InvalidAmountException e) {
                        System.out.println("Eroare: " + e.getMessage());
                    }
                }

                case 4 -> atm.afiseazaIstoric();

                case 5 -> {
                    String dest = Input.readString("Către cine transferi? (username): ");
                    double suma = Input.readDouble("Suma: ");
                    try {
                        atm.transfer(dest, suma);
                    } catch (InvalidAmountException e) {
                        System.out.println("Eroare: " + e.getMessage());
                    }
                }

                case 6 -> logged = false;

                case 7 -> atm.exportCSV();

                default -> System.out.println("Opțiune invalidă!");
            }
            lastActivity = System.currentTimeMillis();
            System.out.println();
        }
    }

    // ---------------- CREARE CONT NOU ----------------

    private static void createAccount(Bank bank) {

        System.out.println("=== CREARE CONT NOU ===");

        String username = Input.readString("Alege username: ");
        String pin = Input.readString("Alege PIN (minim 3 cifre): ");

        boolean ok = bank.createUser(username, pin);

        if (ok) {
            System.out.println("Cont creat cu succes!");
        } else {
            System.out.println("Username deja există!");
        }
    }
}
