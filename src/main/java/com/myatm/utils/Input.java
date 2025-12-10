package com.myatm.utils;

import java.util.Scanner;

public class Input {
    private static final Scanner scanner = new Scanner(System.in);

    public static String readString(String msg){
        System.out.print(msg);
        return scanner.nextLine();
    }

    public static int readInt(String msg){
        System.out.print(msg);
        return Integer.parseInt(scanner.nextLine());
    }

    public static double readDouble(String msg){
        System.out.print(msg);
        return Double.parseDouble(scanner.nextLine());
    }
}
