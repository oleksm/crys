package tech.oleks.crys.model.domain

class Pair {
    Date synced
    String name
    Double tradeFee
    Double minTradeAmount
    Integer minPriceMovement
    String refId
    Double askPrice
    Double bidPrice
    boolean slow
    boolean trade

    static hasMany = [asks: Ask, bids: Bid, trades: Trade]
    static belongsTo = [exchange:Exchange]

    static constraints = {
        name(blank: false, nullable: false, matches: "[a-z0-9]{2,6}_[a-z0-9]{2,6}")
        tradeFee(blank:false, nullable: false, min: 0.0d, max: 0.5d)
        minTradeAmount(blank:false, nullable: false, min: 1.0E-8d)
        minPriceMovement(blank:false, nullable: false, min: 1, max: 10)
        exchange(unique: 'name')
        askPrice (nullable: true)
        bidPrice (nullable: true)
    }

    static mapping = {
        table "CRYS_PAIR"
    }

    @Override
    String toString() {
        return "${getClass().simpleName}(" +
                "id: ${id}, " +
                "name: ${name}, " +
                "tradeFee: ${tradeFee}, " +
                "minTradeAmount: ${minTradeAmount}, " +
                "minPriceMovement: ${minPriceMovement}, " +
                "asks: ${asks?.size()}, " +
                "bids: ${bids?.size()}, " +
                "exchange: ${exchange?.exchangeId})"
    }
}
