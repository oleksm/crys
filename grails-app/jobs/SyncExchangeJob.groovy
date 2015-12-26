import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.service.ExchangeService

//    cronExpression: "s m h D M W Y"
//    | | | | | | `- Year [optional]
//    | | | | | `- Day of Week, 1-7 or SUN-SAT, ?
//    | | | | `- Month, 1-12 or JAN-DEC
//    | | | `- Day of Month, 1-31, ?
//    | | `- Hour, 0-23
//    | `- Minute, 0-59
//    `- Second, 0-59

class SyncExchangeJob {
    static triggers = {
        cron name: "sync-exchange", cronExpression: '0 0 */8 * * ?'
    }

    @Value('${crys.job.syncexchange.enable}')
    def enable


    def group = "sync"
    def description = "Exchange Pairs Synchronization"
    def concurrent = false
    def sessionRequired = true

    def execute() {
        def exchanges = Exchange.findAllByActive(true)
        exchanges?.each { ex ->
            ExchangeService.getExchangeService(ex).updatePairs()
        }
    }
}
