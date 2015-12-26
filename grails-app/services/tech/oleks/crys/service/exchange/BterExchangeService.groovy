package tech.oleks.crys.service.exchange

import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import tech.oleks.crys.exception.ApparentBugException
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Ask
import tech.oleks.crys.model.domain.Bid
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.model.domain.Order
import tech.oleks.crys.model.domain.Pair
import tech.oleks.crys.model.domain.Trade
import tech.oleks.crys.service.ExchangeService

class BterExchangeService extends ExchangeService {

    def restClientService

    Exchange getExchange() {
        def ex = Exchange.findByExchangeId("bter.com");
        if (!ex) {
            ex = new Exchange(exchangeId: "bter.com", name: "BTER", active: true).save(failOnError: true)
        }
        return ex
    }

    List<Pair> getPairs() {
        def resp = restClientService.get("http://data.bter.com/api/1/marketinfo")
        def result = resp.json
        if (result && result.result == "true" && result.pairs) {
            log.debug "Recieved ${result.pairs.size()} markets"
            def pairs = new ArrayList<Pair>()
            result.pairs.each {
                String name = it.keys().next()
                def p = it[name]
                Pair pair = new Pair(
                        name: name,
                        refId: name,
                        tradeFee: p.fee / 100.0d,
                        minTradeAmount: p.min_amount,
                        minPriceMovement: p.decimal_places
                )
                pairs.add(pair)
//                log.debug "created pair: ${pair}"
            }
            return pairs
        } else {
            log.warn "No Markets Found: ${resp.responseEntity.body}"
        }
    }

    @Override
    Pair getPairData(Pair pair) {
        def p = new Pair(
                name: pair.name,
                refId: pair.refId
        )
        getMarketDepth(p)
        p.synced = new Date()
        getMarketHistory(p)
        return p
    }

    def getMarketDepth(Pair pair) {
        def resp = restClientService.get("http://data.bter.com/api/1/depth/${pair.refId}")
        def result = resp.json
        if (result && result.result == "true") {
            def asks = result.asks
            if (asks) {
                pair.asks = []
                log.debug "${pair.name}: Found ${asks.size()} asks"
                asks.each { a ->
                    def ask = new Ask(volume: a[1], price: a[0])
                    pair.asks.add(ask)
//                    log.debug ask
                }
            }
            def bids = result.bids
            if (bids) {
                pair.bids = []
                log.debug "${pair.name}: Found ${bids.size()} bids"
                bids.each { b ->
                    def bid = new Bid(volume: b[1], price: b[0])
                    pair.bids.add(bid)
//                    log.debug bid
                }
            }
        } else {
            log.warn "No market depth for ${pair.name}: ${resp.responseEntity.body} "
        }
    }

    def getMarketHistory(Pair pair) {
        def resp = restClientService.get("http://data.bter.com/api/1/trade/${pair.refId}")
        def result = resp.json
        if (result && Boolean.valueOf(result.result) && result.data) {
            pair.trades = []
            result.data.each { d ->
                try {
                    if (Double.valueOf(d.amount) > 0.0d) {
                        def trade = new Trade(timeStamp: new Date(Long.valueOf(d.date) * 1000L),
                                price: d.price,
                                volume: d.amount,
                                tid: d.tid,
                                type: Trade.Type.valueOf(d.type))
                        pair.trades.add(trade)
//                    log.debug trade
                        if (trade.timeStamp > pair.synced) {
                            log.debug "adjusting pair synced time from: ${pair.synced} to ${trade.timeStamp}"
                            pair.synced = trade.timeStamp
                        }
                    }
                    else {
                        log.warn "recieved trade with zero volume: ${d}"
                    }
                }
                catch (Exception e) {
                    log.error("Can't create trade from json: ${d}", e)
                    throw e
                }
            }
        } else {
            log.warn "No market history for ${pair.name}: ${resp.responseEntity.body} "
        }
    }

    @Override
    protected Account getBalance(Account a) {
        def resp = restClientService.securePost("https://bter.com/api/1/private/getfunds", a.apiKey, a.secretKey,
                a.algorithm)
        def result = resp.json
        if (result.result == "true" && (result.available_funds || result.locked_funds)) {
            def acc = new Account()
            acc.funds = new HashMap(result.available_funds?:[:])
            acc.lockedFunds = new HashMap(result.locked_funds?:[:])
            log.debug result
            return acc
        } else {
            log.warn "No funds info available: ${resp.responseEntity.body}"
        }
    }


    @Override
    protected void doCancelOrder(Order order) {
        def a = order.account
        def resp = restClientService.securePost("https://bter.com/api/1/private/cancelorder", a.apiKey, a.secretKey,
                a.algorithm, [order_id: order.refId])
        def result = resp.json

        if (result && Boolean.valueOf(result.result) && result.msg == "Success") {
            log.debug "Cancel Order ${order.refId} operation success"
        }
        else {
            log.debug "Could not Cancel Order ${order.refId}: ${resp.responseEntity.body}"
            throw new ApparentBugException("Could not Cancel Order ${order.refId}")
        }
    }

    @Override
    protected String doPlaceOrder(Order order) {
//        pair	currency pair	ltc_btc
//        type	trading type	SELL or BUY
//        rate	The rate to buy or sell	0.023
//        amount	The amount to buy or sell	100
        def a = order.account
        def resp = restClientService.securePost("https://bter.com/api/1/private/placeorder", a.apiKey, a.secretKey,
                a.algorithm, [pair: order.pair.name, type: order.type.toString().toUpperCase(), rate: order.price,
                              amount: order.orderedVolume])
        def result = resp.json

        if (result && Boolean.valueOf(result.result) && result.msg == "Success" && result.order_id) {
            log.debug "Place Order ${result.order_id} operation success: ${result}"
            return result.order_id
        }
        else {
            log.debug "Could not Place Order: ${resp.responseEntity.body}"
            throw new ApparentBugException("Could not Place Order")
        }
    }

    List<Order> getOpenOrders(Account a) {
        def resp = restClientService.securePost("https://bter.com/api/1/private/orderlist", a.apiKey, a.secretKey,
                a.algorithm)
        def result = resp.json
        if (Boolean.valueOf(result.result) && result.msg == "Success" && result.orders) {
            def ex = getExchange()
            def orders = []
            result.orders.each { o ->
                // check consistency
                if (!isConsistentOrder(o)) {
                    //throw new ApparentBugException("Inconsistent order json: ${o}")
                }
                // check for pair existance
                def pair = Pair.findByExchangeAndName(ex, o.pair)
                if (!pair) {
                    throw new ApparentBugException("Nop pair found in db: ${o.pair}")
                }

                def order = new Order(
                    refId: o.oid,
                    orderId: o.id,
                    pair: pair,
                    type: Order.Type.valueOf(o.type),
                    status: Order.Status.valueOf(o.status),
                    price: new BigDecimal(o.rate).setScale(pair.minPriceMovement, BigDecimal.ROUND_HALF_UP).doubleValue(),
                    volume: o.get(o.type + "_amount"),
                    orderedVolume: o.initial_amount
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
    Order getOrder(Order order) {
        def a = order.account
        def resp = restClientService.securePost("https://bter.com/api/1/private/getorder", a.apiKey, a.secretKey,
                a.algorithm, [order_id: order.orderId])
        def result = resp.json
        if (Boolean.valueOf(result.result) && result.msg == "Success" && result.order) {
            def o = result.order
            if (o) {
                def pair = Pair.findByExchangeAndName(order.account.exchange, o.pair)
                def reason
                def status
                if (o.status == 'cancelled') {
                    reason == Order.CloseReason.cancel
                    status = Order.Status.closed
                } else {
                    status = Order.Status.valueOf(o.status)
                }
                return new Order(
                        refId: order.refId,
                        pair: pair,
                        orderId: o.id,
                        type: Order.Type.valueOf(o.type),
                        status: status,
                        reason: reason,
                        price: new BigDecimal(Double.valueOf(o.initial_rate)).setScale(pair.minPriceMovement, BigDecimal.ROUND_HALF_UP).doubleValue(),
                        volume: o.amount,
                        orderedVolume: o.initial_amount
                )
            }
        }
        else {
            log.warn "No order ${order.orderId} info available: ${resp.responseEntity.body}"
        }
    }

    @Override
    protected List<Trade> getMyRecentTrades(Account a) {
        // get trades during last 24 hours
        Date cutoff = new DateTime().minusHours(24).toDate()
        def pairs = Order.createCriteria().listDistinct {
            projections {
                distinct "pair"
            }
            and {
                eq("reconciled", false)
                eq("account", a)
                or {
                    ge("created", cutoff)
                    ge("acquired", cutoff)
                    ge("synced", cutoff)
                    ge("closed", cutoff)
                }
            }
        }
        def trades = []
        pairs.each { p ->
            def tr = getMyRecentTrades(a, p)
            if (tr) {
                trades.addAll(tr)
            }
        }
        return trades
    }

    List<Trade> getMyRecentTrades(Account a, Pair pair) {
        def resp = restClientService.securePost("https://bter.com/api/1/private/mytrades", a.apiKey, a.secretKey,
                a.algorithm, [pair: pair.name])
        def result = resp.json
        if (Boolean.valueOf(result.result) && result.msg == "Success" && result.trades) {
            def trades = []
            result.trades.each { t ->
                def trade = new Trade(
                        orderId: t.orderid,
                        refId: t.oid,
                        tid: t.id,
                        timeStamp: new Date(Long.valueOf(t.time_unix) * 1000L),
                        pair: Pair.findByExchangeAndName(a.exchange, t.pair),
                        volume: t.amount,
                        price: t.rate,
                        type: Trade.Type.valueOf(t.type)
                )
                trades.add(trade)
            }
            return trades
        }
        else {
            log.warn "No trades info available: ${resp.responseEntity.body}"
        }
    }


    boolean isConsistentOrder(def o) {
        def r = o.rate == o.initial_rate
        if (!r) return
        switch (o.type) {
            case "sell":
                return (StringUtils.equals(o.amount, o.get(o.type + "_amount"))
                && StringUtils.equalsIgnoreCase(o.sell_type, ((String)o.pair).split("_")[0])
                && StringUtils.equalsIgnoreCase(o.buy_type, ((String)o.pair).split("_")[1]))
            case "buy":
                return (StringUtils.equals(o.amount, o.get("sell_amount"))
                && StringUtils.equalsIgnoreCase(o.sell_type, ((String)o.pair).split("_")[1])
                && StringUtils.equalsIgnoreCase(o.buy_type, ((String)o.pair).split("_")[0]))
        }
    }
}