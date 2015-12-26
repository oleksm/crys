import tech.oleks.crys.exception.ExpressionEvaluationException

/**
 * Created by alexm on 8/10/14.
 */
class OrderBuyJob {
    static triggers = {
        cron name: "place-buy-orders", startDelay: 2000, cronExpression: '30 */5 * * * ?'
    }

    def description = "Place Buy Orders Job"
    def concurrent = false
    def sessionRequired = true

    def orderService
    def pairService
    def accountService

    def execute() {
        def accounts = accountService.getActiveAccounts()
        def allPairs = [:]
        accounts?.each { a ->
            if (accountService.isFundsAvailableForByuing(a)) {
                def pairs = allPairs[a.exchange.exchangeId]
                if (!pairs) {
                    pairs = pairService.getPairsToTrade(a.exchange)
                    allPairs[a.exchange.exchangeId] = pairs
                }
                pairs?.each { p ->
                    try {
                        def eval = orderService.evaluateForBuy(p, a)
                        if (eval) {
                            // re-evaluate to ensure latest trades included
                            if (pairService.fullUpdate(p, 2) | accountService.fullUpdate(a, 2)) {
                                eval = orderService.evaluateForBuy(p, a)
                                if (!eval) {
                                    log.debug "order placement rejected after update: ${p}"
                                    return
                                }
                            }
                            def order = orderService.prepareBuyOrder(a, p, eval)
                            orderService.placeOrder(order)

                            if (!accountService.isFundsAvailableForByuing(a)) {
                                return
                            }
                        }
                    }
                    catch (ExpressionEvaluationException ex) {
                        log.error ex
                    }
                }
            }
            else {
                log.debug "no funds available ${a.id} for byuing"
            }
        }
    }
}
