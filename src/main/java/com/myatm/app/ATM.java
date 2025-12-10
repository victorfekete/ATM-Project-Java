package com.myatm.app;

import com.myatm.model.User;
import com.myatm.service.ATMService;
import com.myatm.service.Bank;
import com.myatm.service.Database;
import com.myatm.service.InvalidAmountException;
import com.myatm.utils.Input;

public class ATM {
    public static void main(String[] args) {

        Database.testConnection();


        Bank bank = new Bank();

        String username = Input.readString("Username: ");
        String pin = Input.readString("Pin: ");

        User user = bank.authenticate(username, pin);

        if(user == null){
            System.out.println("Username sau PIN gresit !");
            return;
        }

        boolean running = true;

        ATMService atm = new ATMService(user);

        while (running) {
            System.out.println("""
                        ===== MINI ATM =====
                        1. Afișează sold
                        2. Depune bani
                        3. Retrage bani
                        4. Istoric
                        5. Ieșire
                    """);

            int optiune = Input.readInt("Alege optiunea: ");

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
                    System.out.println("La revedere!");
                    running = false;
                }

                

                default -> System.out.println("Optiune invalida!");
            }

            System.out.println();
        }
    }
}
