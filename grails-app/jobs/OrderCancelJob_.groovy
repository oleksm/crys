import grails.util.Holders
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.exception.ExpressionEvaluationException

/**
 * Created by alexm on 8/9/14.
 */
class OrderCancelJob_ {
    static triggers = {
        cron name: "cancel-orders", startDelay: 2000, cronExpression: '20 */5 * * * ?'
    }

    def description = "Cancel Orders Job"
    def concurrent = false
    def sessionRequired = true

    def orderService
    def pairService
    def accountService

    def activ = false


    def execute() {
        log.debug description

        def orders = orderService.getOpenOrders()
        orders?.each { order ->
            try {
                if (orderService.isForCancel(order)) {
                    if (pairService.fullUpdate(order.pair, 2) | accountService.fullUpdate(order.account, 2)) {
                        if (!orderService.isForCancel(order)) {
                            log.info "order cancelling rejected after update: ${order.pair}"
                            return
                        }
                    }
                    orderService.cancelOrder(order)
                }
            }
            catch (ExpressionEvaluationException ex) {
                log.error ex
            }
        }
    }
}
