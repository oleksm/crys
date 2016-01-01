import org.springframework.beans.factory.annotation.Value

/**
 * Created by alexm on 8/31/14.
 */
class PairStatsDailyJob_ {
    static triggers = {
        cron name: "pair-calcStats-daily", startDelay: 2000, cronExpression: '8 3 01 * * ?'
    }

    def description = "Collect Pair Stats Daily Job"
    def concurrent = false
    def sessionRequired = false

    def pairService
    def evaluationService
    int mins = 60 * 24 + 5

    def execute() {
        def pairs = pairService.activePairs()
        pairs?.each { p ->
            evaluationService.addStats(p, mins, "daily")
        }
        log.info "collected daily calcStats for ${pairs?.size()} pairs"
    }
}
