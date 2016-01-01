import org.springframework.beans.factory.annotation.Value

/**
 * Created by alexm on 8/10/14.
 */
class OrderReconcileJob_ {
    static triggers = {
        cron name: "reconcile-closed-orders", startDelay: 2000, cronExpression: '25 07 */8 * * ?'
    }

    def description = "Reconcile Closed Orders Job"
    def concurrent = false
    def sessionRequired = true
    def active = false

    def orderService

    @Value('${crys.job.orderreconcile.enable}')
    def enable


    def execute() {
        orderService.reconcileClosedOrders()
    }
}
