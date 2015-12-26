import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.exception.ExpressionEvaluationException

/**
 * Created by alexm on 8/12/14.
 */
class OrderSellJob {
    static triggers = {
        cron name: "place-sell-orders", startDelay: 2000, cronExpression: '40 */5 * * * ?'
    }

    def description = "Place Sell Orders Job"
    def concurrent = false
    def sessionRequired = true

    def orderService
    def pairService
    def accountService

    @Value('${crys.job.ordersell.enable}')
    def enable


    def execute() {
        def accounts = accountService.getActiveAccounts()
        def allPairs = [:]
        accounts?.each { a ->
            def pairs = accountService.getAllPairsForSell(a)
            pairs?.each { p ->
                try {
                    def eval = orderService.evaluateForSell(p, a)
                    if (eval) {
                        // re-evaluate to ensure latest trades included
                        if (pairService.fullUpdate(p, 2) | accountService.fullUpdate(a, 2)) {
                            eval = orderService.evaluateForSell(p, a)
                            if (!eval) {
                                log.debug "order placement rejected after update: ${p}"
                                return
                            }
                            def order = orderService.prepareSellOrder(a, p, eval)
                            orderService.placeOrder(order)
                        }
                    }
                }
                catch (ExpressionEvaluationException ex) {
                    log.error ex
                }
            }
        }
    }
}
