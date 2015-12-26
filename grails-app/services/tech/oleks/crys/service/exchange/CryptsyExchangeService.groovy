package tech.oleks.crys.service.exchange

import grails.transaction.Transactional
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.exception.ApparentBugException
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Ask
import tech.oleks.crys.model.domain.Bid
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.model.domain.Order
import tech.oleks.crys.model.domain.Pair
import tech.oleks.crys.model.domain.Trade
import tech.oleks.crys.service.ExchangeService

import java.text.DateFormat
import java.text.SimpleDateFormat

@Transactional
class CryptsyExchangeService extends ExchangeService {

    @Value('${crys.exchange.cryptsy.tradeFee}')
    double tradeFee
    @Value('${crys.exchange.cryptsy.minPriceMovement}')
    Integer minPriceMovement
    @Value('${crys.exchange.cryptsy.minTradeAmount}')
    double minTradeAmount
    @Value('${crys.exchange.cryptsy.secureUrl}')
    String secureUrl
    @Value('${crys.exchange.cryptsy.dateFormat}')
    String dateFormat
    @Value('${crys.exchange.cryptsy.timeZone}')
    String timeZone

    def restClientService

    @Override
    Exchange getExchange() {
        def ex = Exchange.findByExchangeId("cryptsy.com");
        if (!ex) {
            ex = new Exchange(exchangeId: "cryptsy.com", name: "Cryptsy", active: true).save(failOnError: true)
        }
        return ex
    }

    @Override
    protected List<Pair> getPairs() {
        Date synced = new Date()
        def resp = restClientService.get("http://pubapi.cryptsy.com/api.php?method=marketdatav2")
        return parseJsonResult(resp, false, synced)
    }

    List<Pair> parseJsonResult(def resp, def addTrades = false, Date synced) {
        def result = resp.json
        if (result && result.success == 1) {
            def markets = result.return.markets
            if (markets) {
                log.debug "Recieved market data"
                def pairs = new ArrayList<Pair>()
                def it = markets.keys()
                while (it.hasNext()) {
                    def market = markets.get(it.next())
                    if (market && market.lasttradeprice) {
                        Pair pair = new Pair(
                                name: "${market.primarycode}_${market.secondarycode}".toLowerCase(),
                                refId: market.marketid,
                                tradeFee: tradeFee,
                                minTradeAmount: minTradeAmount,//new BigDecimal(minTradeAmount / Double.valueOf(market.lasttradeprice) * 2).setScale(8, BigDecimal.ROUND_HALF_UP).toDouble(),
                                minPriceMovement: minPriceMovement,
                                synced: synced
                        )
                        if (addTrades) {
                            if (market.recenttrades) {
                                DateFormat df = getDateFormat()
                                pair.trades = []
                                // {"id":"62802099","time":"2014-08-16 16:45:19","type":"Buy","price":"528.06889999","quantity":"0.09530362","total":"50.32687778"}
                                market.recenttrades?.each { t ->
                                    try {
                                        def trade = new Trade(timeStamp: df.parse(t.time),
                                                price: t.price,
                                                volume: t.quantity,
                                                tid: t.id,
                                                type: Trade.Type.valueOf(StringUtils.lowerCase(t.type)))
                                        pair.trades.add(trade)
                                        if (trade.timeStamp > pair.synced) {
                                            log.debug "adjusting pair synced time from: ${pair.synced} to ${trade.timeStamp}"
                                            pair.synced = trade.timeStamp
                                        }
                                    }
                                    catch (def e) {
                                        log.error "couldn't parse trade ${t}"
                                        throw e
                                    }
                                }
                            }
                            if (market.sellorders) {
                                pair.asks = []
                                log.debug "Recieved ${market.sellorders.size()} asks"
                                market.sellorders.each { a ->
                                    def ask = new Ask(volume: a.quantity, price: a.price)
                                    pair.asks.add(ask)
//                                    log.debug ask
                                }
                            }
                            if (market.buyorders) {
                                pair.bids = []
                                log.debug "Recieved ${market.buyorders.size()} bids"
                                market.buyorders.each { b ->
                                    def bid = new Bid(volume: b.quantity, price: b.price)
                                    pair.bids.add(bid)
//                                    log.debug bid
                                }
                            }
                        }
                        pairs.add(pair)
                    }
                }
                return pairs
            }
        }
        log.warn "No Markets Found: ${resp.responseEntity.body}"
    }

    @Override
    Pair getPairData(Pair pair) {
        Date synced = new Date()
        def resp = restClientService.get("http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid=${pair.refId}")
        def result = parseJsonResult(resp, true, synced)
        if (result?.size() > 0) {
            return result.get(0)
        }
    }

    @Override
    protected Account getBalance(Account a) {
        def resp = restClientService.securePost(secureUrl, a.apiKey, a.secretKey, a.algorithm,
                [method: "getinfo"])
        def result = resp.json
        if (result && result.success == "1" && result.return) {
            def ret = result.return
            def acc = new Account()
            acc.funds = new HashMap(ret.balances_available?:[:])
            acc.lockedFunds = new HashMap(ret.balances_hold?:[:])
//            log.debug ret
            return acc
        } else {
            log.warn "No funds info available: ${resp.responseEntity.body}"
        }
        return null
    }

    @Override
    protected List<Order> getOpenOrders(Account a) {
        def resp = restClientService.securePost(secureUrl, a.apiKey, a.secretKey,
                a.algorithm, [method: "allmyorders"])
        def result = resp.json
        log.debug "result: ${result}"
        if (result && result.success == "1" && result.return) {
            def ex = getExchange()
            def orders = []
            def df = getDateFormat()
            result.return.each { o ->
                // check for pair existance
                def pair = Pair.findByExchangeAndRefId(ex, o.marketid)
                if (!pair) {
                    throw new ApparentBugException("Nop pair found in db: ${o.pair}")
                }
                def order = new Order(
                        refId: o.orderid,
                        orderId: o.orderid,
                        pair: pair,
                        type: Order.Type.valueOf(StringUtils.lowerCase(o.ordertype)),
                        status: Order.Status.open,
                        price: new BigDecimal(o.price).setScale(pair.minPriceMovement, BigDecimal.ROUND_HALF_UP).doubleValue(),
                        volume: o.quantity,
                        orderedVolume: o.orig_quantity,
                        created: df.parse(o.created)
                )
                orders.add(order)
                log.debug o
            }
            return orders
        } else {
            log.warn "No open orders info available: ${resp.responseEntity.body}"
        }
    }

    @Override
    protected void doCancelOrder(Order order) {
        def a = order.account
        def resp = restClientService.securePost(secureUrl, a.apiKey, a.secretKey,
                a.algorithm, [method: "cancelorder", orderid: order.refId])
        def result = resp.json

        if (result && result.success == "1") {
            log.debug "Cancel Order ${order.refId} operation success"
        }
        else {
            log.debug "Could not Cancel Order ${order.refId}: ${resp.responseEntity.body}"
            throw new ApparentBugException("Could not Cancel Order ${order.refId}")
        }
    }

    @Override
    protected Order getOrder(Order order) {
        def a = order.account
        def resp = restClientService.securePost(secureUrl, a.apiKey, a.secretKey,
                a.algorithm, [method: "getorderstatus", orderid: order.orderId])
        def result = resp.json
        log.debug "Recieved: ${result}"
        if (result && result.success == "1" && result.return) {
            def o = result.return
            if (o) {
                def pair = order.pair
                def status = (NumberUtils.isNumber(String.valueOf(o.orderinfo.active)) ? Integer.valueOf(o.orderinfo.active) > 0 : Boolean.valueOf(o.orderinfo.active))  ? Order.Status.open : Order.Status.closed
                def remainQty = Double.valueOf(o.orderinfo.remainqty)
                return new Order(
                        refId: order.refId,
                        pair: pair,
                        orderId: order.orderId,
                        type: order.type,
                        status: status,
                        price: order.price,
                        volume: o.orderinfo.remainqty,
                        orderedVolume: remainQty > 0 || (o.tradeinfo && o.tradeinfo.size() > 0) ?
                                (remainQty + (o.tradeinfo && o.tradeinfo.size() > 0 ? o.tradeinfo.sum {Double.valueOf(it.quantity)} : 0.0d)) : order.orderedVolume
                )
            }
        }
        else {
            log.warn "No order ${order.orderId} info available: ${resp.responseEntity.body}"
        }
    }

    @Override
    protected List<Trade> getMyRecentTrades(Account a) {
        def resp = restClientService.securePost(secureUrl, a.apiKey, a.secretKey,
                a.algorithm, [method: "allmytrades", startdate: new DateTime().minusHours(24).toString("yyyy-MM-dd")])
        def result = resp.json
        log.debug "recieved response: ${result}"
        if (result && result.success == "1" && result.return) {
            def trades = []
            def df = getDateFormat()
            result.return.each { t ->
                def trade = new Trade(
                        orderId: t.order_id,
                        refId: t.order_id,
                        tid: t.tradeid,
                        timeStamp: df.parse(t.datetime),
                        pair: Pair.findByExchangeAndRefId(a.exchange, t.marketid),
                        volume: t.quantity,
                        price: t.tradeprice,
                        type: Trade.Type.valueOf(StringUtils.lowerCase(t.tradetype)),
                )
                trades.add(trade)
            }
            return trades
        }
        else {
            log.warn "No addTrades info available: ${resp.responseEntity.body}"
        }
    }

    private DateFormat getDateFormat() {
        DateFormat df = new SimpleDateFormat(dateFormat)
        df.setTimeZone(TimeZone.getTimeZone(timeZone))
        return df
    }

    @Override
    protected String doPlaceOrder(Order o) {
        def a = o.account
        def resp = restClientService.securePost(secureUrl, a.apiKey, a.secretKey,
                a.algorithm, [method   : "createorder", marketid: o.pair.refId,
                              ordertype: StringUtils.capitalize(o.type.toString()), quantity: o.orderedVolume,
                              price    : o.price])
        def result = resp.json
        log.debug "Place Order respond: ${result}"
        if (result && result.success == "1" && result.orderid) {
            log.debug "Place Order ${result.orderid} operation success: ${result}"
            return result.orderid
        }
        else {
            log.debug "Could not Place Order: ${resp.responseEntity.body}"
            throw new ApparentBugException("Could not Place Order")
        }
    }
}
