package tech.oleks.crys.model.domain

class TradeProfile {

    Map itemizations
    Double minQuote
    Double maxQuote
    int maxOpenSellOrders
    int maxOpenBuyOrders
    Double maxPairQuote
    Double failBreakTreshold
    Integer maxOpenOrderHr
    Integer buyOrderDelay
    Integer scaleStatsMins
    Integer sellOrderDelay
    Double sellQuoteMult
    Double sellQuoteSweep
    List<String> buyExpressions
    List<String> bidExpressions
    List<String> sellExpressions
    List<String> askExpressions
    List<String> cancelAskExprs
    List<String> cancelBidExprs
    List<String> watchExpressions
    List<String> quoteExpressions

    static constraints = {
    }

    static hasMany = [bidExpressions: String, buyExpressions: String, sellExpressions: String, askExpressions: String,
                      cancelAskExprs: String, cancelBidExprs: String, quoteExpressions: String,
                      watchExpressions: String]

    static mapping = {
        table "C_TPROFILE"
    }
}