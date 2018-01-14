# crypto-consolidator

Given a Binance API key and secret, consolidate all coins by converting into Bitcoin. Designed as an executable jar for usage in the command line.<br/>

Command format:
```
java -jar crypto-consolidator.jar [COINS TO EXCLUDE]
```

Example usage:
Converts all non-zero balance coins to Bitcoin:
```
java -jar crypto-consolidator.jar
```

Converts all non-zero balance coins except Monero (XMR) and 0x (ZRX) into BTC:<br/>
```
java -jar crypto-consolidator.jar XMR ZRX
```
