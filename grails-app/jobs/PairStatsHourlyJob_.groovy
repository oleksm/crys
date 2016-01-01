import org.springframework.beans.factory.annotation.Value

/**
 * Created by alexm on 8/31/14.
 */
class PairStatsHourlyJob_ {
    static triggers = {
        cron name: "pair-calcStats-hourly", startDelay: 2000, cronExpression: '0 3 * * * ?'
    }

    def description = "Collect Pair Stats Hourly Job"
    def concurrent = false
    def sessionRequired = false

    def pairService
    def evaluationService
    int mins = 60 + 5

    def execute() {
        def pairs = pairService.activePairs()
        pairs?.each { p ->
            evaluationService.addStats(p, mins, "hourly")
        }
        log.debug "collected calcStats for ${pairs?.size()} pairs"
    }
}
