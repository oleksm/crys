package tech.oleks.crys.service

import grails.transaction.Transactional
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.evaluation.Expressions
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.MarketStats
import tech.oleks.crys.model.domain.Pair

import java.math.RoundingMode

@Transactional
class EvaluationService {

    @Value('${crys.pair.stats.depth.lookup}')
    Double depthLookup
    @Value('${crys.sync.pair.stats.tag}')
    String syncStatsTag


    MarketStats calcStats(Pair pair, int mins) {
        def cutoff = new DateTime().minusMinutes(mins).toDate()
        return calcStats(pair, cutoff)
    }

    MarketStats calcStats(Pair pair, Date cutoff, MarketStats checkpoint = null) {
        // Aggregate Trades
        def trades = checkpoint ?
                Trade.findAllByPairAndTimeStampGreaterThanAndTimeStampLessThanEquals(pair, cutoff, checkpoint.timeStamp)
                : Trade.findAllByPairAndTimeStampGreaterThan(pair, cutoff)
        if (!trades) {
            log.debug "No trades for ${pair.name} after ${cutoff}"
            return
        }
        def sells = trades.findAll { it.type == Trade.Type.sell }
        def buys = trades.findAll { it.type == Trade.Type.buy }
        def s = new MarketStats(
                cutoff: cutoff,
                buys: buys?.size() ?: 0,
                sells: sells?.size() ?: 0,
                maxPrice: trades.max { it.price }.price,
                minPrice: trades.min { it.price }.price,
                volPurchased: buys.sum { it.volume } ?: 0.0d,
                volSold: sells.sum { it.volume } ?: 0.0d,
                amtPurchased: buys.sum { it.price * it.volume } ?: 0.0d,
                amtSold: sells.sum { it.price * it.volume } ?: 0.0d,
                timeStamp: checkpoint ? checkpoint.timeStamp : pair.synced,
                pair: pair
        )
        s.lastSell = sells ? sells.max {it.timeStamp}
                : Trade.findByTypeAndPairAndTimeStampLessThanEquals(Trade.Type.sell, pair, s.timeStamp,
                [sort: 'timeStamp', order: 'desc', max: 1])
        s.lastBuy = buys ? buys.max {it.timeStamp}
                : Trade.findByTypeAndPairAndTimeStampLessThanEquals(Trade.Type.buy, pair, s.timeStamp,
                [sort: 'timeStamp', order: 'desc', max: 1])
        s.volTotal = s.volPurchased + s.volSold
        s.amtTotal = s.amtPurchased + s.amtSold
        s.avgPrice = s.volTotal ? s.amtTotal / s.volTotal : null
        s.avgBuyPrice = s.volPurchased ? s.amtPurchased / s.volPurchased : null
        s.avgSellPrice = s.volSold ? s.amtSold / s.volSold : null
        if (!checkpoint) {
            // Aggregate Ask Depth
            def step = new BigDecimal(pair.askPrice * depthLookup).setScale(pair.minPriceMovement, RoundingMode.UP).toDouble()
            def asks = Ask.findAllByPairAndPriceLessThanEquals(pair, pair.askPrice + step)
            if (asks) {
                s.askVol = asks.sum { it.volume }
                s.askAmt = asks.sum { it.volume * it.price }
            }
            // Aggregate Bid Depth
            step = new BigDecimal(pair.bidPrice * depthLookup).setScale(pair.minPriceMovement, RoundingMode.UP).toDouble()
            def bids = Bid.findAllByPairAndPriceGreaterThanEquals(pair, pair.bidPrice - step)
            if (bids) {
                s.bidVol = bids.sum { it.volume }
                s.bidAmt = bids.sum { it.volume * it.price }
            }
            s.askPrice = pair.askPrice
            s.bidPrice = pair.bidPrice
        }
        else {
            s.askVol = checkpoint.askVol
            s.askAmt = checkpoint.askAmt
            s.askPrice = checkpoint.askPrice
            s.bidVol = checkpoint.bidVol
            s.bidAmt = checkpoint.bidAmt
            s.bidPrice = checkpoint.bidPrice
        }
        return s
    }

    def addStats(Pair pair, int mins, String tag) {
        def cutoff = new DateTime().minusMinutes(mins).minusMillis(1).toDate()
        addStats(pair, cutoff, tag)
    }

    def addStats(Pair pair, Date cutoff, String tag) {
        log.debug "Adding statistics for ${pair.exchange.name} ${pair.name} after ${cutoff} (${tag})"
        def stats = calcStats(pair, cutoff)
        if (stats) {
            stats.tag = tag
            stats.save(failOnError: true)
        } else {
            log.debug "No calcStats calculated for ${pair.exchange.name}:${pair.name}"
        }
    }

    def getStatsByTagMinAmt(def tag, def minAmt) {
        return MarketStats.createCriteria().list {
            createAlias "pair", "p"
            and {
                eq "tag", tag
                ge "amtTotal", minAmt
                eq "p.trade", true
                eq "p.slow", false
                eqProperty("p.synced", "timeStamp")
            }
            order "timeStamp", "desc"
        }
    }

    def getAllStatsByTagMinAmt(def tag, def minAmt) {
        return MarketStats.createCriteria().list {
            createAlias "pair", "p"
            createAlias "p.exchange", "ex"
            and {
                eq "tag", tag
                eq "p.trade", true
                eq "p.slow", false
                eq "ex.active", true
                ge "amtTotal", minAmt
                eq "ex.name", "Cryptsy"
                eq "p.name", "ltc_btc"
            }
            order "timeStamp", "asc"
        }
    }

    def getLastStats(def pair, def tag) {
        return MarketStats.findByTimeStampAndPairAndTag(pair.synced, pair, tag)
    }

    List<MarketStats> getLastStats(def pair, def tag, def max) {
        return MarketStats.findAllByPairAndTag(pair, tag, [sort: 'timeStamp', order: 'desc', max: max])
    }


    def getStats(def pair, int hr, String tag) {
        def cutoff = new DateTime().minusHours(hr).toDate()
        return MarketStats.findAllByPairAndTagAndTimeStampGreaterThanEquals(pair, tag, cutoff,
                [sort: "timeStamp", order: "desc"])
    }

    def boolean bid(Account a, Pair p) {
        return new Expressions(account: a, pair: p).run('bidExpressions').result
    }

    def boolean ask(Account a, Pair p) {
        return new Expressions(account: a, pair: p).run('askExpressions').result
    }

    def boolean buy(Account a, Pair p) {
        //TODO: no buys supported yet
        return false
    }

    def getCurrentSampleByPairAndAccount(Account a, Pair pair) {
        // get latest 2
        def stats = getLastStats(pair, syncStatsTag, 2)
        def n = stats[0]
        def p = stats[1]
        def cutoff = new DateTime(p.timeStamp).minusMinutes(a.profile.scaleStatsMins).toDate()

        //TODO: scale by latest time interval

        return calcStats(pair, cutoff, n)
    }

    def getPreviousSampleByPairAndAccount(Account a, Pair pair) {
        // get latest 2
        def stats = getLastStats(pair, syncStatsTag, 2)
        def p = stats[1]
        def cutoff = new DateTime(p.timeStamp).minusMinutes(a.profile.scaleStatsMins).toDate()

        //TODO: scale by latest time interval

        return calcStats(pair, cutoff, p)
    }
}
