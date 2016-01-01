import grails.util.Holders
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.service.ExchangeService


class SyncExchangeJob {
    static triggers = {
        cron name: "sync-exchange", cronExpression: Holders.grailsApplication.config.crys.job.syncexchange.schedule
    }

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
