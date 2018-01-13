package com.science;

import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceOrder;
import com.webcerebrium.binance.datatype.BinanceOrderPlacement;
import com.webcerebrium.binance.datatype.BinanceOrderSide;
import com.webcerebrium.binance.datatype.BinanceOrderType;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import com.webcerebrium.binance.datatype.BinanceWalletAsset;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public class Consolidator {

    private final String BASE_CURRENCY = "BTC";

    private Set<String> coinExclusions;
    private BinanceApi binance;

    public Consolidator() {
        try {
            Properties props = new Properties();
            InputStream stream = Consolidator.class.getClassLoader().getResourceAsStream("application.properties");
            props.load(stream);
            stream.close();

            binance = new BinanceApi(props.getProperty("binance-key"), props.getProperty("binance-secret"));
        } catch (IOException | BinanceApiException ex) {
            ex.printStackTrace();
        }

        this.coinExclusions = new HashSet<>();
        this.coinExclusions.add(this.BASE_CURRENCY);
    }

    public void getBalances() {
         Map<String, BinanceWalletAsset> balanceMap = new HashMap<>();
         Map<String, BigDecimal> priceMap = new HashMap<>();

         // Retrieve wallet balance and price of all pairings
         try {
             balanceMap = binance.balancesMap();
             priceMap = binance.pricesMap();
         } catch (BinanceApiException ex) {
             ex.printStackTrace();
         }

         Map<String, BinanceWalletAsset> orderedBalances = new TreeMap<>();
         orderedBalances.putAll(balanceMap/*.entrySet().stream().limit(10).collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()))*/);

         BigDecimal totalBitcoinAmount = BigDecimal.ZERO;

         for (Map.Entry<String, BinanceWalletAsset> balance : orderedBalances.entrySet()) {
             String asset = balance.getKey();
             BinanceWalletAsset wallet = balance.getValue();

             String tradingPair = asset + this.BASE_CURRENCY;

             // Exclude necessary coins
             if (this.coinExclusions.contains(asset)) {
                 System.out.println("Excluding " + asset + " from sell order");
                 continue;
             }

             // Ignore pairings that are invalid
             if (!priceMap.containsKey(tradingPair)) {
                 System.out.println("Trading pair " + tradingPair + " doesn't exist");
                 continue;
             }

             BigDecimal amountInBitcoin = wallet.getFree().multiply(priceMap.get(tradingPair));
             System.out.println(asset + " will be sold at the rate of " + priceMap.get(tradingPair) + " " + this.BASE_CURRENCY +
                                " for a total of " + amountInBitcoin + " " + this.BASE_CURRENCY);

             totalBitcoinAmount = totalBitcoinAmount.add(amountInBitcoin);

             try {
                 BinanceSymbol symbol = new BinanceSymbol(tradingPair);
                 BinanceOrderPlacement placement = new BinanceOrderPlacement(symbol, BinanceOrderSide.SELL);

                 placement.setType(BinanceOrderType.MARKET);
                 placement.setPrice(priceMap.get(tradingPair));
                 placement.setQuantity(wallet.getFree());

//                 Long orderId = binance.createOrder(placement).get("OrderId").getAsLong();
//                 BinanceOrder order = binance.getOrderById(symbol, orderId);
//
//                 System.out.println(order.toString());
             } catch (BinanceApiException ex) {
                 ex.printStackTrace();
                 continue;
             }
         }

         System.out.println("Total bitcoin amount: " + totalBitcoinAmount);
    }
}