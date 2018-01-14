package com.science;

import com.science.exception.InvalidCoinException;

public class Application {

    public static void main(String args[]) {
        BinanceHelper binance = new BinanceHelper();

        try {
            binance.consolidateCoins(args);
        } catch (InvalidCoinException ex) {
            ex.printStackTrace();
        }
    }
}
