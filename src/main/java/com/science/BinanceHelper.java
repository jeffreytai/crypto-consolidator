package com.science;

import com.google.gson.JsonObject;
import com.science.exception.InvalidCoinException;
import com.science.slack.SlackWebhook;
import com.science.binance.api.BinanceApi;
import com.science.binance.api.BinanceApiException;
import com.science.binance.datatype.BinanceExchangeInfo;
import com.science.binance.datatype.BinanceExchangeSymbol;
import com.science.binance.datatype.BinanceOrder;
import com.science.binance.datatype.BinanceOrderPlacement;
import com.science.binance.datatype.BinanceOrderSide;
import com.science.binance.datatype.BinanceOrderType;
import com.science.binance.datatype.BinanceSymbol;
import com.science.binance.datatype.BinanceWalletAsset;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;


public class BinanceHelper {

    private final String BASE_CURRENCY = "BTC";
    private final BigDecimal MINIMUM_BTC_VALUE = BigDecimal.valueOf(0.001);

    private Set<String> coinExclusions;
    private BinanceApi binance;

    public BinanceHelper() {
        try {
            Properties props = new Properties();
            InputStream stream = BinanceHelper.class.getClassLoader().getResourceAsStream("exchange.properties");
            props.load(stream);
            stream.close();

            binance = new BinanceApi(props.getProperty("binance-key"), props.getProperty("binance-secret"));
        } catch (IOException | BinanceApiException ex) {
            ex.printStackTrace();
        }

        this.coinExclusions = new HashSet<>();
        this.coinExclusions.add(this.BASE_CURRENCY);
    }

    public void consolidateCoins(String[] exclusions) throws InvalidCoinException {
         Map<String, BinanceWalletAsset> balanceMap = new HashMap<>();
         Map<String, BigDecimal> priceMap = new HashMap<>();
         List<BinanceExchangeSymbol> exchangeSymbols = new ArrayList<>();

         // Retrieve wallet balance and price of all pairings
         try {
             balanceMap = binance.balancesMap();
             priceMap = binance.pricesMap();

             BinanceExchangeInfo binanceExchangeInfo = binance.exchangeInfo();
             exchangeSymbols = binanceExchangeInfo.getSymbols();

             // Used just for debugging - orders the list of Exchange Symbols
//             exchangeSymbols = exchangeSymbols.stream().sorted(Comparator.comparing((BinanceExchangeSymbol b) -> b.getBaseAsset())).collect(Collectors.toList());
         } catch (BinanceApiException ex) {
             ex.printStackTrace();
         }

        for (String exclusion : exclusions) {
             boolean anyMatch = priceMap.keySet().stream().anyMatch(e -> e.indexOf(exclusion) == 0);
             if (!anyMatch)  {
                 throw new InvalidCoinException(exclusion + " is not a valid coin");
             }

             coinExclusions.add(exclusion);
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

             BinanceExchangeSymbol exchangeSymbol = exchangeSymbols.stream().filter(s -> s.getSymbol().getSymbol().equals(tradingPair)).findFirst().get();
             Double minNotional = exchangeSymbol.getMinNotional().get("minNotional").getAsDouble();
             Double stepSize = exchangeSymbol.getLotSize().get("stepSize").getAsDouble();

             BigDecimal remainder = wallet.getFree().remainder(BigDecimal.valueOf(stepSize));
             BigDecimal amountToSell = wallet.getFree().subtract(remainder);

             BigDecimal amountInBitcoin = amountToSell.multiply(priceMap.get(tradingPair));

             if (amountInBitcoin.compareTo(BigDecimal.valueOf(minNotional)) < 0) {
                 System.out.println(asset + " doesn't meet minimum lot size");
                 continue;
             }

             System.out.println(asset + " will be sold at the rate of " + priceMap.get(tradingPair) + " " + this.BASE_CURRENCY +
                                " for a total of " + amountInBitcoin + " " + this.BASE_CURRENCY);

             totalBitcoinAmount = totalBitcoinAmount.add(amountInBitcoin);

             try {
                 BinanceSymbol symbol = new BinanceSymbol(tradingPair);
                 BinanceOrderPlacement placement = new BinanceOrderPlacement(symbol, BinanceOrderSide.SELL);

                 // Safeguard against selling for 0
                 BigDecimal sellPrice = priceMap.get(tradingPair);
                 if (sellPrice.equals(BigDecimal.ZERO)) {
                     continue;
                 }

                 placement.setType(BinanceOrderType.MARKET);
                 placement.setPrice(sellPrice);
                 placement.setQuantity(amountToSell);
                 System.out.println("Order placement: " + placement.toString());

                 // Used for debugging, creates a test order
//                 JsonObject createOrder = binance.testOrder(placement);

                 JsonObject createOrder = binance.createOrder(placement);
                 Long orderId = createOrder.get("orderId").getAsLong();
                 BinanceOrder order = binance.getOrderById(symbol, orderId);

                 System.out.println("Order: " + order.toString());
             } catch (BinanceApiException ex) {
                 System.out.println("Error with coin " + asset);
                 ex.printStackTrace();
                 continue;
             }
         }

         System.out.println("Total bitcoin amount: " + totalBitcoinAmount);
         sendSlackAlertMessage(totalBitcoinAmount);
    }

    private void sendSlackAlertMessage(BigDecimal totalBitcoinAmount) {
        SlackWebhook slack = new SlackWebhook("binance-consolidation");
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        // Formulate alert text
        String message = String.format("Consolidated %s worth of BTC on %s",
                totalBitcoinAmount.setScale(4, RoundingMode.FLOOR).toString(), dateFormat.format(new Date()).toString());

        // Send message to Slack channel
        slack.sendMessage(message);
    }
}
