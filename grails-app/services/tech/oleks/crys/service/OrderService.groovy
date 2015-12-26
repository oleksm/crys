package tech.oleks.crys.service

import grails.transaction.Transactional
import grails.util.Holders
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.evaluation.Expressions
import tech.oleks.crys.evaluation.OrderEvaluation
import tech.oleks.crys.exception.ApparentBugException
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Ask
import tech.oleks.crys.model.domain.Order
import tech.oleks.crys.model.domain.Pair
import tech.oleks.crys.util.EvalUtils

@Transactional
class OrderService {

    def conf = Holders.grailsApplication.config.crys.ai.campaign.evaluation

    @Value('${crys.handle.manual.orders}')
    boolean handleManualOrders
    @Value('${crys.order.minPriceStep}')
    int minPriceStep

    def pairService
    def accountService
    def evaluationService

    List<Order> getOpenOrders() {
        return handleManualOrders ?
                Order.findAllByStatus(Order.Status.open, [sort: "price", order: "desc"]) :
                Order.findAllByStatusAndCreatedIsNotNull(Order.Status.open, [sort: "price", order: "desc"])
    }

    boolean isForCancel(Order o) {
        // give it a chance of 4hr min
        def orderDate = o.created ?: o.acquired
        // slow market
        if (o.pair.slow) {
            return true
        }
        def profile = o.account.profile
        // No orders longer than 24hr
        if (orderDate < new DateTime().minusHours(profile.maxOpenOrderHr).toDate()) {
            log.debug "Cancel order ${o.refId}: sitting more than 24hr"
            return true
        }
        switch (o.type) {
            case Order.Type.buy:
                // if this is best candidate among my other same pair orders
                if (o.price == maxMyOrderPrice(o)) {
                    // overwhelming buy depth above
                    def bda = Bid.createCriteria().get {
                        projections {
                            sum "volume"
                        }
                        and {
                            eq("pair", o.pair)
                            gt("price", o.price)
                        }
                    }
                    def vSold = evaluationService.getCurrentSampleByPairAndAccount(o.account, o.pair).volSold
                    if (bda > 0 && bda > vSold) {
                        log.debug "Cancel order ${o.refId}: Overwhelming buy depth above ${bda} > ${vSold}"
                        return true
                    }
                    def exprs = new Expressions(account: o.account, pair: o.pair)
                    if (!exprs.run('bidExpressions')) {
                        log.debug "Cancel order ${o.refId} coz don't want to buy ${o.pair.name} anymore"
                    }
                }
                break;
            case Order.Type.sell:
                // if this is best candidate among my other same pair orders
                if (o.price == minMyOrderPrice(o)) {
                    // overwhelming sell depth above
                    def sda = Ask.createCriteria().get {
                        projections {
                            sum "volume"
                        }
                        and {
                            eq("pair", o.pair)
                            lt("price", o.price)
                        }
                    }
                    def vPur = evaluationService.getCurrentSampleByPairAndAccount(o.account, o.pair).volPurchased
                    if (sda > 0 && sda > vPur) {
                        log.debug "Cancel order ${o.refId}: Overwhelming sell depth above ${sda} > ${vPur}"
                        return true
                    }
                }
                break;
        }
        log.debug "No need to Cancel order ${o.refId}"
    }

    def OrderEvaluation evaluateForBuy(Pair p, Account a) {
        def base = pairService.baseCurrency(p.name)
        def merch = pairService.merchCurrency(p.name)

        if (a.funds[base] < a.profile.minQuote) {
            log.debug "No funds available to buy ${a.name} - ${p.name}"
            return
        }

        // doesn't exceed pair quote?
        def q = a.funds[base] < a.profile.maxQuote ? a.funds[base] : a.profile.maxQuote
        def invested = (EvalUtils.dbl(a.funds[merch]) + EvalUtils.dbl(a.lockedFunds[merch])) * p.bidPrice
        if (invested + q > a.profile.maxPairQuote) {
            log.debug "merch ${merch} investment ${invested} exceeds limit ${a.profile.maxPairQuote} for the pair ${p.name}"
            return
        }
        // is last order placed more than interval
        def orders = Order.createCriteria().list {
            and {
                eq("account", a)
                eq("pair", p)
                eq("type", Order.Type.buy)
                eq("status", Order.Status.open)
            }
            order("created", "desc")
            order("acquired", "desc")
        }
        if (orders) {
            if (orders.size() >= a.profile.maxOpenBuyOrders) {
                log.debug "Exceeded number of open orders per pair ${p.name} for account ${a.name}: ${a.profile.maxOpenBuyOrders}"
                return
            }
            def order = orders.get(0)
            Date d = order.created ?: order.acquired
            if (d > new DateTime().minusMinutes(a.profile.buyOrderDelay).toDate()) {
                log.debug "Order ${order.refId} placed at ${d} which less than ${a.profile.buyOrderDelay} min past"
                return
            }
        }
        // evaluate and create bid order
        if (evaluationService.bid(a, p)) {
            def eval = new OrderEvaluation(
                    quote: q,
                    price: p.bidPrice + Double.valueOf("${minPriceStep}.0E-${p.minPriceMovement}")
            )
            if (eval.price >= p.askPrice) {
                eval.price = p.bidPrice
            }
            return eval
        }
    }

    OrderEvaluation evaluateForSell(Pair p, Account a) {
        def merch = pairService.merchCurrency(p.name)
        if (!a.funds[merch] || EvalUtils.gamt(a.funds[merch], p.askPrice, p.tradeFee) < p.minTradeAmount) {
            log.debug "Not enough ${merch} funds ${a.funds[merch]} left to sell"
            return
        }
        // is last order placed more than interval
        def orders = Order.createCriteria().list {
            and {
                eq("account", a)
                eq("pair", p)
                eq("type", Order.Type.sell)
                eq("status", Order.Status.open)
            }
            order("created", "desc")
            order("acquired", "desc")
        }
        if (orders) {
            if (orders.size() >= a.profile.maxOpenSellOrders) {
                log.debug "Exceeded number of open orders per pair ${p.name}: ${a.profile.maxOpenSellOrders}"
                return
            }
            def order = orders.get(0)
            Date d = order.created ?: order.acquired
            if (d > new DateTime().minusMinutes(a.profile.sellOrderDelay).toDate()) {
                log.debug "Order ${order.refId} placed at ${d} which less than ${a.profile.sellOrderDelay} min past"
                return
            }
        }
        // evaluate and create ask order
        def exprs = new Expressions(account: a, pair: p)
        if (exprs.run('askExpressions')?.result) {
            def eval = new OrderEvaluation(
                    price: p.askPrice - Double.valueOf("${minPriceStep}.0E-${p.minPriceMovement}")
            )
            if (eval.price <= p.bidPrice) {
                eval.price = p.askPrice
            }
            eval.quote = a.funds[merch] * eval.price
            if (eval.quote > a.profile.maxQuote) {
                eval.quote = a.profile.maxQuote
            }
            return eval
        }
        if (exprs.run('sellExpressions')?.result) {
            def eval = new OrderEvaluation(
                    price: p.bidPrice
            )
            eval.quote = a.funds[merch] * eval.price
            if (eval.quote > a.profile.maxQuote) {
                eval.quote = a.profile.maxQuote
            }
            return eval
        }
    }

    Order prepareBuyOrder(Account account, Pair pair, OrderEvaluation eval) {
        //refresh exchange data and re-evaluate
        if (!eval || eval.quote <= 0.0d) {
            throw new ApparentBugException("Evaluated quote is not valid: ${eval.quote} for ${account.name} - ${pair.name}")
        }
        def volume = eval.quote / eval.price * (1.0d - pair.tradeFee)
        def created = new Date()
        return new Order(
            created: created,
            acquired: created,
            type: Order.Type.buy,
            status: Order.Status.open,
            price: eval.price,
            orderedVolume: volume,
            volume: volume,
            account: account,
            pair: pair
        )
    }

    Order prepareSellOrder(Account account, Pair pair, OrderEvaluation eval) {
        //refresh exchange data and re-evaluate
        def volume = eval.quote / eval.price * account.profile.sellQuoteMult
        // cash sweep
        def sweep = account.funds[pairService.merchCurrency(pair.name)]
        if (sweep < volume * account.profile.sellQuoteSweep) {
            volume = sweep
        }
        def created = new Date()
        return new Order(
                created: created,
                acquired: created,
                type: Order.Type.sell,
                status: Order.Status.open,
                price: eval.price,
                orderedVolume: volume,
                volume: volume,
                account: account,
                pair: pair
        )
    }

    def minMyOrderPrice(Order o) {
        return Order.createCriteria().get {
            projections {
                min("price")
            }
            and {
                eq("account", o.account)
                eq("pair", o.pair)
                eq("type", o.type)
                eq("status", o.status)
            }
        }
    }

    def maxMyOrderPrice(Order o) {
        return Order.createCriteria().get {
            projections {
                max("price")
            }
            and {
                eq("account", o.account)
                eq("pair", o.pair)
                eq("type", o.type)
                eq("status", o.status)
            }
        }
    }

    def reconcileClosedOrders() {

        // update my trades
        def accounts = accountService.getActiveAccounts()
        accounts?.each { acc ->
            accountService.syncMyTrades(acc)
            accountService.updateOrders(acc)
        }
        def orders = Order.findAllByReconciledAndStatusNotEqual(false, Order.Status.open)
        orders?.each { o ->
            if (o.orderId) {
                ExchangeService.getExchangeService(o.pair.exchange).updateOrder(o)
            }
            else {
                o.status = Order.Status.closed
                o.reason = o.volume > 0.0d ? Order.CloseReason.cancel : Order.CloseReason.complete
            }
            o.reconciled = true
            o.save(failOnError: true)
        }
    }

    def placeOrder(Order o) {
        ExchangeService.getExchangeService(o.pair.exchange).placeOrder(o)
    }

    def cancelOrder(Order o) {
        ExchangeService.getExchangeService(o.pair.exchange).cancelOrder(o)
    }
}
