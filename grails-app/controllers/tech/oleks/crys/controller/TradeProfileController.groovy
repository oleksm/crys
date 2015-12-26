package tech.oleks.crys.controller

import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.TradeProfile

class TradeProfileController {

    def scaffold = TradeProfile

    def setupA() {
        if (!params.aid) {
            render "expected param aid"
            return
        }
        def a = Account.get(Long.valueOf(params.aid))
        if (!a) {
            render "No account found for '${params.aid}'"
            return
        }
        def p = a.profile
        p.bidExpressions = [
                "n.amtPurchased / n.amtSold > 0.75",
                "n.bidAmt / n.askAmt > 0.5",
                "n.lastSell.timeStamp > new DateTime(n.timeStamp).minusMinutes(15).toDate()",
                "n.lastBuy.timeStamp > new DateTime(n.timeStamp).minusMinutes(15).toDate()",
                "n.avgPrice / p.avgPrice > 0.99",
                "n.avgSellPrice / p.avgSellPrice > 0.99",
                "n.bidAmt / p.bidAmt > 0.9",
                "n.bidPrice / p.bidPrice > 0.99",
                "n.bidPrice / h4.avgSellPrice > 0.95"
        ]
        p.askExpressions = [
                "n.amtPurchased / n.amtSold < 1.0",
                "n.lastBuy.timeStamp > new DateTime(n.timeStamp).minusMinutes(15).toDate()",
                "n.avgPrice / p.avgPrice < 1.05",
                "n.avgBuyPrice / p.avgBuyPrice < 1.05",
                "n.amtPurchased / p.amtPurchased < 1.2",
                "n.amtSold / p.amtSold > 1.0",
                "n.bidPrice / p.bidPrice <= 1.01"
        ]
        p.watchExpressions = [
                "n.timeStamp",
                "p.timeStamp",
                "n.amtPurchased / n.amtSold",
                "n.bidAmt / n.askAmt",
                "n.lastSell.timeStamp",
                "n.lastBuy.timeStamp",
                "n.avgPrice / p.avgPrice",
                "n.avgBuyPrice / p.avgBuyPrice",
                "n.avgSellPrice / p.avgSellPrice",
                "n.bidAmt / p.bidAmt",
                "n.bidPrice / p.bidPrice",
                "n.amtPurchased / p.amtPurchased",
                "n.amtSold / p.amtSold",
                "n.bidPrice / h4.avgSellPrice",
                "n.bidPrice",
                "n.avgSellPrice",
                "h4.avgPrice"
        ]
        p.sellExpressions = [
                "n.bidAmt / n.askAmt <= 1.0",
                "n.avgPrice / p.avgPrice <= 1.0",
                "n.avgSellPrice / p.avgSellPrice <= 1.0",
                "n.avgBuyPrice / p.avgBuyPrice <= 1.0",
                "n.amtPurchased / p.amtPurchased < 1.1",
                "n.bidAmt / p.bidAmt < 1.0",
                "n.bidPrice / p.bidPrice < 1.0"
        ]
        p.quoteExpressions = ["n.amtTotal * 0.01 < 0.01 ? n.amtTotal * 0.01 : 0.01"]

        p.save(failOnError: true)
        render "OK"
    }

    def setupB() {
        if (!params.aid) {
            render "expected param aid"
            return
        }
        def a = Account.get(Long.valueOf(params.aid))
        if (!a) {
            render "No account found for '${params.aid}'"
            return
        }
        def p = a.profile
        p.bidExpressions = [
                "n.lastSell.timeStamp > new DateTime(n.timeStamp).minusMinutes(15).toDate()",
                "n.lastBuy.timeStamp > new DateTime(n.timeStamp).minusMinutes(10).toDate()",
                "n.avgSellPrice / p.avgSellPrice > 0.99",
                "n.amtPurchased / p.amtPurchased > 0.99",
                "n.bidPrice / p.bidPrice > 0.99"
        ]
        p.askExpressions = [
                "n.avgBuyPrice / p.avgBuyPrice < 1.01",
                "n.amtPurchased / p.amtPurchased < 1.01",
                "n.amtSold / p.amtSold > 1.0",
                "n.askPrice / p.askPrice < 1.01",
                "n.bidPrice / p.bidPrice < 1.01",
                "n.lastBuy.timeStamp > new DateTime(n.timeStamp).minusMinutes(15).toDate()"
        ]

        p.watchExpressions = [
                "n.timeStamp",
                "p.timeStamp",
                "n.amtPurchased / n.amtSold",
                "n.bidAmt / n.askAmt",
                "n.lastSell.timeStamp",
                "n.lastBuy.timeStamp",
                "n.avgPrice / p.avgPrice",
                "n.avgBuyPrice / p.avgBuyPrice",
                "n.avgSellPrice / p.avgSellPrice",
                "n.bidAmt / p.bidAmt",
                "n.bidPrice / p.bidPrice",
                "n.amtPurchased / p.amtPurchased",
                "n.amtSold / p.amtSold",
                "n.askAmt / p.askAmt",
                "n.bidPrice / h4.avgSellPrice",
                "n.bidPrice",
                "n.avgSellPrice",
                "h4.avgPrice"
        ]
        p.sellExpressions = [
                "n.avgBuyPrice / p.avgBuyPrice <= 1.0",
                "n.amtPurchased / p.amtPurchased <= 1.0",
                "n.amtSold / p.amtSold > 1.0",
                "n.askAmt / p.askAmt < 1.0",
                "n.bidAmt / p.bidAmt < 1.0",
                "n.askPrice / p.askPrice < 1.0",
                "n.bidPrice / p.bidPrice < 1.0"
        ]
        p.quoteExpressions = ["n.amtTotal * 0.01 < 0.01 ? n.amtTotal * 0.01 : 0.01"]

        p.save(failOnError: true)
        render "OK"
    }

}
