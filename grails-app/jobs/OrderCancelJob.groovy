import grails.util.Holders
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.exception.ExpressionEvaluationException

/**
 * Created by alexm on 8/9/14.
 */
class OrderCancelJob {
    static triggers = {
        cron name: "cancel-orders", startDelay: 2000, cronExpression: '20 */5 * * * ?'
    }

    def description = "Cancel Orders Job"
    def concurrent = false
    def sessionRequired = true

    def orderService
    def pairService
    def accountService

    @Value('${crys.job.ordercancel.enable}')
    def enable

    def execute() {
        log.debug description

        def orders = orderService.getOpenOrders()
        orders?.each { order ->
            try {
                if (orderService.isForCancel(order)) {
                    if (pairService.fullUpdate(order.pair, 2) | accountService.fullUpdate(order.account, 2)) {
                        if (!orderService.isForCancel(order)) {
                            log.debug "order cancelling rejected after update: ${order.pair}"
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
