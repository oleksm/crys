package tech.oleks.crys.service

import grails.transaction.Transactional
import grails.util.Holders
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.exception.ApparentBugException
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Ask
import tech.oleks.crys.model.domain.Audit
import tech.oleks.crys.model.domain.Bid
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.model.domain.Order
import tech.oleks.crys.model.domain.Pair
import tech.oleks.crys.model.domain.Trade
import tech.oleks.crys.service.exchange.BterExchangeService
import tech.oleks.crys.service.exchange.CryptsyExchangeService

@Transactional
abstract class ExchangeService  {

    def auditService
    def consistencyValidationService
    def accountService

    @Value('${crys.exchange.accept.pairs.regex}')
    def acceptPairsRegex

    @Value('${crys.pair.sync.slowTreshold24}')
    Double slowPairTreshold24

    def updatePairs() {
        def ex = getExchange()
        def pairs = getPairs()
        auditService.syncEvent(new Audit(entity: Audit.Entity.Pair, attributes: [exchange: ex.name]))
        def localPairs = ex.pairs
        pairs.each { p ->
            if (!p.name.matches(acceptPairsRegex)) {
                log.debug "Filtered out as not a match: ${p.name}"
                return
            }
            def lp = localPairs.find { it.name == p.name }
            if (lp) {
                log.debug " Found local pair: ${lp}"
                //TODO: verify pair change
                if (lp.name != p.name
                    || lp.refId != p.refId
                    || lp.tradeFee != p.tradeFee
                    || lp.minTradeAmount != p.minTradeAmount
                    || lp.minPriceMovement != p.minPriceMovement) {
                    log.warn "Local Pair and remote do not match! \n${lp}\n${p}"
                }
                lp.name = p.name
                lp.refId = p.refId
                lp.tradeFee = p.tradeFee
                lp.minTradeAmount = p.minTradeAmount
                lp.minPriceMovement = p.minPriceMovement
            }
            else {
                log.info "New pair: ${p}"
                // TODO: submit add event
                p.trade = true
                p.synced = new Date()
                ex.addToPairs(p)
            }
        }
        // verify deleted pairs
        localPairs.each { lp ->
            def p = pairs.find{ it.name == lp.name }
            if (!p) {
                // TODO: submit delete event
                log.warn "Remove Pair: ${lp}"
            }
        }
        ex.save(failOnError: true)
        return ex
    }

    def updatePair(Pair lp) {
        Pair p = getPairData(lp)
        if (p) {
            lp.refresh()
            updateMarketHistory(p, lp)
            updateMarketDepth(p, lp)
            lp.synced = p.synced
            lp.save(failOnError: true)
        }
        else {
            log.warn "No pair ${lp.name} data fetched from ${lp.exchange.name}"
        }
    }

    private updateMarketDepth(Pair p, Pair pair) {
        List<Ask> asks = new ArrayList(pair.asks)
        asks?.each { ask ->
            pair.removeFromAsks(ask)
            ask.delete(failOnError: true)
        }
        if (pair.asks) {
            throw new ApparentBugException("Still some asks for pair not removed")
        }
        pair.askPrice = p.asks.min {it.price}.price
        p.asks.each { pair.addToAsks(it) }
        auditService.syncEvent(new Audit(entity: Audit.Entity.Ask, attributes: [exchange: pair.exchange.name, pair: pair.name]))

        List<Bid> bids = new ArrayList(pair.bids)
        bids?.each { bid ->
            pair.removeFromBids(bid)
            bid.delete(failOnError: true)
        }
        if (pair.bids) {
            throw new ApparentBugException("Still some bids for pair not removed")
        }
        if (p.bids) {
            pair.bidPrice = p.bids.max {it.price}.price
            p.bids.each { pair.addToBids(it)}
        }
        auditService.syncEvent(new Audit(entity: Audit.Entity.Bid, attributes: [exchange: pair.exchange.name, pair: pair.name]))
    }


    private updateMarketHistory(Pair p, Pair pair) {
        def hr24 = new DateTime().minusHours(24).toDate()
        def trades = p.trades.findAll {it.timeStamp > hr24}
        auditService.syncEvent(new Audit(entity: Audit.Entity.Trade, attributes: [exchange: pair.exchange.name, pair: pair.name]))
        if (trades
                && trades.find {it.type == Trade.Type.buy}
                && trades.find {it.type == Trade.Type.sell}
                // todo scale to minimum date recieved trades within 1 day
                && trades.sum {it.price * it.volume} >= slowPairTreshold24) {
            pair.slow = false
            Date date = trades.min{it.timeStamp}.timeStamp
            def localTrades = Trade.findAllByPairAndTimeStampGreaterThanEquals(pair, date)
            trades.each { t ->
                def lt = localTrades.find { it.tid == t.tid }
                if (lt) {
//                    log.debug "Found local trade: ${lt}"
                    if (lt.price != t.price || lt.type != t.type || lt.volume != t.volume || lt.timeStamp != t.timeStamp) {
                        log.warn "Local and remode trade not equal! ${lt} <> ${t}"
                    }
                }
                else {
                    log.debug "New trade history record for pair ${p.name}: ${t}"
                    pair.addToTrades(t)
                }
            }
        }
        else {
            pair.slow = true
            log.info "${pair.exchange.name}:${pair.name} is slow market"
        }
    }

    def updateOrders(Account account) {
        def orders = getOpenOrders(account)
        def ex = getExchange()
        Date d =  new Date()
        auditService.syncEvent(new Audit(entity: Audit.Entity.Order, attributes: [exchange: ex.name]))
        if (orders) {
            def ids = orders.collect {it.refId}
            log.debug "updating remote orders: ${ids}"
            def localOrders = Order.findAllByRefIdInList(ids)
            orders.each { o ->
                def lo = localOrders.find {it.refId == o.refId}
                if (lo) {
                    if (o.volume != lo.volume) {
                        log.info "Order ${o.refId} volume changed: ${lo.volume} -> ${o.volume}"
                    }
                    lo.status = o.status
                    lo.volume = o.volume
                    lo.synced = d
                    lo.save(failOnError: true)
                }
                else {
                    log.info "Acquired new remote order for ${account.name} - ${o.pair.name}: ${o.orderId}}"
                    // Add Order
                    o.acquired = d
                    o.synced = d
                    account.addToOrders(o)
                }
            }
            // update closed orders
            def closed = Order.createCriteria().list {
                createAlias("pair", "p")
                eq "status", Order.Status.open
                eq "account", account
                eq "p.exchange", ex
                not {
                    'in' ('refId', ids)
                }
            }
            closed?.each { lo ->
                lo.status = Order.Status.closed
                lo.synced = d
                lo.save(failOnError: true)
                log.info "closing order ${lo.refId} for unknown yet reason"
            }
        }
    }

    def cancelOrder(Order order) {
        log.info "cancelling order: ${order.refId}"
        // check belongs to this exchange
        def ex = getExchange()
        if (order.pair.exchange != ex) {
            throw new ApparentBugException("order ${order} doesn'trade belong to ${ex.name}")
        }
        Date d = new Date()
        doCancelOrder(order)
        order.closed = d
        order.synced = d
        order.status = Order.Status.closed
        order.reason = Order.CloseReason.cancel
        order.save(failOnError: true)
        accountService.unlockBalance(order)
    }

    def updateBalance(Account account) {
        // check belongs to this exchange
        def ex = getExchange()
        if (account.exchange != ex) {
            throw new ApparentBugException("order ${order} doesn'trade belong to ${ex.name}")
        }
        def acc = getBalance(account)
        // lowercase currency names
        acc.funds = acc.funds.findAll { Double.valueOf(it.value) > 0 }.collectEntries {[(it.key.toLowerCase()): Double.valueOf(it.value)]}
        acc.lockedFunds = acc.lockedFunds.findAll { Double.valueOf(it.value) > 0 }.collectEntries {[(it.key.toLowerCase()): Double.valueOf(it.value)]}
        consistencyValidationService.updateBalance(acc, account)
        account.funds = acc.funds
        account.lockedFunds = acc.lockedFunds
        account.save(failOnError: true)
    }

    def placeOrder(Order order) {
        // check belongs to this exchange
        def ex = getExchange()
        if (order.pair.exchange != ex) {
            throw new ApparentBugException("order ${order} doesn'trade belong to ${ex.name}")
        }

        order.refId = doPlaceOrder(order)
        if (!order.refId) {
            throw new ApparentBugException("No Reference ID assigned on place order for ${order.account.name} - ${order.pair.name}")
        }
        order.orderId = order.refId
        order.synced = order.created = new Date()
        order.account = order.account.refresh()
        order.save(failOnError: true)
        accountService.lockBalance(order)
    }

    static ExchangeService getExchangeService(Exchange ex) {
        switch (ex.exchangeId) {
            case "bter.com": return Holders.applicationContext.getBean(BterExchangeService.class)
            case "cryptsy.com": return Holders.applicationContext.getBean(CryptsyExchangeService.class)
        }
    }

    def updateOrder(Order order) {
        def o = getOrder(order)
        if (o) {
            if (order.refId != o.refId
                    || (order.status != o.status && order.status != Order.Status.unknown)
                    || order.pair != o.pair
                    || order.type != o.type
                    || order.orderId != o.orderId) {
                log.debug "order.refId: ${order.refId}, o.refId: ${o.refId}, order.status: ${order.status}, " +
                        "o.status: ${o.status}, order.pair: ${order.pair}, o.pair: ${o.pair}, order.type: ${order.type}," +
                        "o.type: ${o.type}, order.orderId: ${order.orderId}, o.orderId: ${o.orderId}"
                throw new ApparentBugException("Remote and Local Orders do not match: ${order.refId}")
            }
            // partially completed
            if (o.status == Order.Status.closed && o.volume > 0) {
                if (order.reason && order.reason != Order.CloseReason.cancel) {
                    throw new ApparentBugException("Remote and Local Orders close reason do not match: ${order.refId}")
                }
                order.reason = Order.CloseReason.cancel
            }
            order.synced = new Date()
            order.orderedVolume = o.orderedVolume
            order.volume = o.volume
            order.price = o.price
            if (!order.reason && order.volume == 0.0d) {
                order.reason = Order.CloseReason.complete
            }

            if ((!order.reason) || (o.reason && order.reason != o.reason)) {
                throw new ApparentBugException("Remote and Local Orders close reason do not match: ${order.refId}")
            }

            order.save(failOnError: true)
        }
        else {
            log.warn "No order exists remotely in ${order.pair.exchange}:${order.pair.name} ${order.orderId} (${order.refId})}"
        }
    }

    def updateMyRecentTrades(Account account) {
        Date now = new Date()
        List<Trade> trades = getMyRecentTrades(account)
        trades?.each { trade ->
            def order = account.orders?.find { o-> o.refId == trade.refId}
            if (order) {
                order.orderId = trade.orderId
                if (order.acquired < trade.timeStamp) {
                    order.acquired = trade.timeStamp
                }
                order.synced = now
                order.save(failOnError: true)

            }
            // create order from trade
            else {
                log.info "Creating order ${trade.refId} from my trade"
                def o = new Order(
                        orderId: trade.orderId,
                        refId: trade.refId,
                        type: Order.Type.valueOf(trade.type.toString()),
                        acquired: trade.timeStamp,
                        synced: now,
                        pair: trade.pair,
                        status: Order.Status.unknown,
                        price: trade.price,
                        orderedVolume: trade.volume,
                        volume: 0.0d)
                account.addToOrders(o)
                o.save(failOnError: true)
            }
        }
    }

    def abstract Exchange getExchange()
    abstract protected List<Pair> getPairs()
    abstract protected Pair getPairData(Pair pair)
    abstract protected Account getBalance(Account a)
    abstract protected List<Order> getOpenOrders(Account a);
    abstract protected void doCancelOrder(Order order)
    abstract protected Order getOrder(Order order)
    abstract protected List<Trade> getMyRecentTrades(Account a)
    abstract protected String doPlaceOrder(Order order)
}
