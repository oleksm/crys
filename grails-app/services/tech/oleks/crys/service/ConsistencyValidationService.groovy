package tech.oleks.crys.service

import grails.transaction.Transactional
import tech.oleks.crys.exception.ApparentBugException
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.ConsistencyCheck
import tech.oleks.crys.model.domain.Order
import tech.oleks.crys.util.EvalUtils
import tech.oleks.crys.util.InvokeUtils

@Transactional
class ConsistencyValidationService implements GroovyInterceptable {

    def pairService
    def active = true

    def updateOrders(List<Order> orders) {
        def tests = ConsistencyCheck.findAllByBindToAndPassed("update-orders", false)
        tests?.each { test ->
            def refId = test.attrs.order
            switch (test.type) {
                case ConsistencyCheck.Type.CancelOrder:
                    // 1. There is no order living in exchange
                    if (orders.find { it.refId == refId }) {
                        throw new ApparentBugException("Consistency check not passed ${test.type}, order: ${refId}: Order exists remotely")
                    }
                    // 2. Local order is closed
                    def lo = Order.findByRefId(refId)
                    if (lo.status != Order.Status.closed || lo.reason != Order.CloseReason.cancel) {
                        throw new ApparentBugException("Consistency check not passed ${test.type}, order: ${refId}: Local order is not closed properly")
                    }
                    break
                case ConsistencyCheck.Type.NewOrder:
                    // 1. There must have order living in exchange
                    def o = orders.find { it.refId == refId }
                    if (!o) {
                        throw new ApparentBugException("Consistency check not passed ${test.type}, order: ${refId}: Order does not exists remotely")
                    }
                    // 2. Local order is open and consistent
                    def lo = Order.findByRefId(refId)
                    // check if found order belongs to account
                    if (o.orderedVolume != lo.orderedVolume
                            || o.price != lo.price
                            || o.type != lo.type
                            || o.status != lo.status
                            || o.pair != lo.pair
                            || o.volume > lo.volume
                            || lo.status != Order.Status.open) {
                        // inconsistent order, take a look!
                        throw new ApparentBugException("Consistency check not passed ${test.type}, order: ${refId}: remote data do not match local")
                    }
                    break
                default:
                    throw new ApparentBugException("No more test types expected for updateOrders: ${test.type}")
            }
            test.passed = true
        }
        // add check for any other unexpected activities around orders
    }

    def updateBalance2(Account a, Account la) {
        def tests = ConsistencyCheck.findAllByBindToAndPassed("update-balance", false)
        Map funds = new HashMap(la.funds)
        Map lockedFunds = new HashMap(la.lockedFunds)
        if (tests) {
            tests.each { test ->
                def currency = test.attrs.currency
                Double volume = Double.valueOf(test.attrs.volume)
                switch (test.type) {
                    case ConsistencyCheck.Type.LockFunds:
                        funds[currency] -= volume
                        if (funds[currency] == 0.0d) {
                            funds.remove(currency)
                        }
                        def v = lockedFunds[currency]
                        lockedFunds[currency] = v ? v += volume : volume
                        break
                    case ConsistencyCheck.Type.UnlockFunds:
                        def v = funds[currency]
                        funds[currency] = v ? v += volume : volume
                        lockedFunds[currency] -= volume
                        if (lockedFunds[currency] == 0.0d) {
                            lockedFunds.remove(currency)
                        }
                        break
                    default:
                        throw new ApparentBugException("No more test types expected for updateBalance: ${test.type}")

                }
            }
        }
        // reconcile total
        if (!a.funds.equals(funds) || !a.lockedFunds.equals(lockedFunds)) {
            throw new ApparentBugException("Consistency check not passed ${tests?.collect { it.type }}, can not reconcile remote funds ${a.funds} with local ${funds} and r lockedFunds ${a.lockedFunds} with ${lockedFunds} ")
        }
        tests?.each { it.passed = true }
    }

    def cancelOrder(Order order) {
        fileOrderTests(order, "cancelOrder-${order.refId}", [ConsistencyCheck.Type.UnlockFunds, ConsistencyCheck.Type.CancelOrder])
    }

    def placeOrder(Order order) {
        fileOrderTests(order, "placeOrder-${order.refId}", [ConsistencyCheck.Type.LockFunds, ConsistencyCheck.Type.NewOrder])
    }

    protected fileOrderTests(Order order, def source, List types) {
        def created = new Date()
        def pair = order.pair
        def currency
        def volume
        if (order.type == Order.Type.sell) {
            currency = pairService.merchCurrency(pair.name)
            volume = order.volume
        } else {
            currency = pairService.baseCurrency(pair.name)
            volume = order.volume * order.price
        }
        new ConsistencyCheck(
                created: created,
                type: types[0],
                source: source,
                bindTo: "update-balance",
                attrs: [
                        currency: currency,
                        volume: String.valueOf(volume)
                ]
        ).save(failOnError: true)
        new ConsistencyCheck(
                created: created,
                type: types[1],
                source: source,
                bindTo: "update-orders",
                attrs: [
                        "order": order.refId,
                ]
        ).save(failOnError: true)
    }

    def invokeMethod(String name, args) {
        if (active) {
            return InvokeUtils.invokeMethod(this, name, args)
        }
    }

    def updateBalance(Account a, Account la) {
        def af = EvalUtils.mergeFunds(a.funds, a.lockedFunds)
        def laf = EvalUtils.mergeFunds(la.funds, la.lockedFunds)
        if (!af.equals(laf)) {
            log.warn "af <> laf, diff: ${EvalUtils.diffFunds(af, laf)}"
        }
    }

}