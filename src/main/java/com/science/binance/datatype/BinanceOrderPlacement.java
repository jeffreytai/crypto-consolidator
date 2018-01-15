package com.science.binance.datatype;

/* ============================================================
 * java-binance-api
 * https://github.com/webcerebrium/java-binance-api
 * ============================================================
 * Copyright 2017-, Viktor Lopata, Web Cerebrium OÜ
 * Released under the MIT License
 * ============================================================ */

import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.science.binance.api.BinanceApiException;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BinanceOrderPlacement {
    public BinanceSymbol symbol = null;
    public BinanceOrderSide side = null;
    public BinanceOrderType type = BinanceOrderType.LIMIT;
    public BinanceTimeInForce timeInForce = BinanceTimeInForce.GOOD_TILL_CANCELLED;
    public BigDecimal quantity;
    public BigDecimal price;
    public String newClientOrderId = "";
    public BigDecimal stopPrice = null;
    public BigDecimal icebergQty = null;

    public BinanceOrderPlacement() {
    }

    public BinanceOrderPlacement(BinanceSymbol symbol, BinanceOrderSide side) {
        this.symbol = symbol;
        this.side = side;
    }

    public String getAsQuery() throws BinanceApiException {
        StringBuffer sb = new StringBuffer();
        Escaper esc = UrlEscapers.urlFormParameterEscaper();
        if (symbol == null) {
            throw new BinanceApiException("Order Symbol is not set");
        }
        sb.append("&symbol=").append(symbol.toString());
        if (side == null) {
            throw new BinanceApiException("Order side is not set");
        }
        sb.append("&side=").append(side.toString());
        if (type == null) {
            throw new BinanceApiException("Order type is not set");
        }
        sb.append("&type=").append(type.toString());
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BinanceApiException("Order quantity should be bigger than zero");
        }
        sb.append("&quantity=").append(quantity.toString());

        if (type == BinanceOrderType.MARKET) {
            // price should be skipped for a market order, we are accepting market price
            // so should timeInForce
        } else {
            if (timeInForce == null) {
                throw new BinanceApiException("Order timeInForce is not set");
            }
            sb.append("&timeInForce=").append(timeInForce.toString());
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BinanceApiException("Order price should be bigger than zero");
            }
            sb.append("&price=").append(price.toString());
        }

        if (!Strings.isNullOrEmpty(newClientOrderId)) {
            sb.append("&newClientOrderId=").append(esc.escape(newClientOrderId));
        }
        if (stopPrice != null) {
            sb.append("&stopPrice=").append(stopPrice.toString());
        }
        if (icebergQty != null) {
            sb.append("&icebergQty=").append(icebergQty.toString());
        }
        return sb.toString().substring(1); // skipping the first &
    }

    public BinanceSymbol getSymbol() {
        return symbol;
    }

    public void setSymbol(BinanceSymbol symbol) {
        this.symbol = symbol;
    }

    public BinanceOrderSide getSide() {
        return side;
    }

    public void setSide(BinanceOrderSide side) {
        this.side = side;
    }

    public BinanceOrderType getType() {
        return type;
    }

    public void setType(BinanceOrderType type) {
        this.type = type;
    }

    public BinanceTimeInForce getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(BinanceTimeInForce timeInForce) {
        this.timeInForce = timeInForce;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getNewClientOrderId() {
        return newClientOrderId;
    }

    public void setNewClientOrderId(String newClientOrderId) {
        this.newClientOrderId = newClientOrderId;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    public BigDecimal getIcebergQty() {
        return icebergQty;
    }

    public void setIcebergQty(BigDecimal icebergQty) {
        this.icebergQty = icebergQty;
    }
}
