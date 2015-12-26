package tech.oleks.crys.model.domain

class MarketStats {

    Date timeStamp
    Date cutoff
    Double maxPrice
    Double minPrice
    Double avgPrice
    Double avgSellPrice
    Double avgBuyPrice
    Integer buys
    Integer sells
    Double volPurchased
    Double volSold
    Double volTotal
    Double amtPurchased
    Double amtSold
    Double amtTotal
    Double askVol
    Double bidVol
    Double askAmt
    Double bidAmt
    Double askPrice
    Double bidPrice
    Trade lastSell
    Trade lastBuy
    String tag

    static belongsTo = [pair: Pair]

    static constraints = {
        maxPrice (nullable: true, min: 1.0E-9d)
        minPrice (nullable: true, min: 1.0E-9d)
        avgPrice (nullable: true, min: 1.0E-9d)
        avgSellPrice (nullable: true, min: 1.0E-9d)
        avgBuyPrice (nullable: true, min: 1.0E-9d)
        buys (nullable: false, min: 0)
        sells (nullable: false, min: 0)
        volPurchased (nullable: false, min: 0.0d)
        volSold (nullable: false, min: 0.0d)
        volTotal (nullable: false, min: 0.0d)
        amtPurchased (nullable: false, min: 0.0d)
        amtSold (nullable: false, min: 0.0d)
        amtTotal (nullable: false, min: 0.0d)
        askPrice (nullable: true, min: 1.0E-9d)
        bidPrice (nullable: true, min: 1.0E-9d)
        bidVol (nullable: false, min: 0.0d)
        askVol (nullable: false, min: 0.0d)
        askAmt (nullable: false, min: 0.0d)
        bidAmt (nullable: false, min: 0.0d)
        lastSell(nullable: true)
        lastBuy(nullable: true)
        timeStamp (nullable: false)
    }

    static mapping = {
        table "CRYS_MARKET_STATS"
    }
}