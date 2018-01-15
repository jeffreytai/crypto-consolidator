package com.science.binance.websocket;
/* ============================================================
 * java-binance-api
 * https://github.com/webcerebrium/java-binance-api
 * ============================================================
 * Copyright 2017-, Viktor Lopata, Web Cerebrium OÜ
 * Released under the MIT License
 * ============================================================ */


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.science.binance.api.BinanceApiException;
import com.science.binance.datatype.BinanceEventAggTrade;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

@Slf4j
public abstract class BinanceWebSocketAdapterAggTrades extends WebSocketAdapter {
    @Override
    public void onWebSocketConnect(Session sess) {
//        log.debug("onWebSocketConnect: {}", sess);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
//        log.error("onWebSocketError: {}", cause);
    }

    @Override
    public void onWebSocketText(String message) {
//        log.debug("onWebSocketText message={}", message);
        JsonObject operation = (new Gson()).fromJson(message, JsonObject.class);
        try{
            onMessage(new BinanceEventAggTrade(operation));
        } catch ( BinanceApiException e ) {
//            log.error("Error in websocket message {}", e.getMessage());
        }
    }
    public abstract void onMessage(BinanceEventAggTrade event) throws BinanceApiException;
}
