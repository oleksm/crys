import grails.util.Holders
import org.springframework.beans.factory.annotation.Value

/**
 * Created by alexm on 8/14/14.
 */
class SyncSlowPairsJob {

    static triggers = {
        cron name: "sync-slow-pairs", startDelay: 2000, cronExpression: '05 05 01 * * ?'
    }

    def description = "Refresh Slow Pairs"
    def concurrent = false
    def sessionRequired = true

    def pairService

    @Value('${crys.job.syncslowpairs.enable}')
    def enable


    def execute() {
        def pairs = pairService.getSlowPairs()
        pairs?.each { pair ->
            pairService.fullUpdate(pair, 2)
        }
    }
}
