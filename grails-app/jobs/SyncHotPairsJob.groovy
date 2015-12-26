import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Pair

/**
 * Created by alexm on 8/8/14.
 */
class SyncHotPairsJob {

    static triggers = {
        cron name: "valuate-hot-pairs", startDelay: 2000, cronExpression: '02 */5 * * * ?'
    }

    def description = "Exchange Pairs Evaluation"
    def concurrent = false
    def sessionRequired = true

    def pairService
    def accountService

    @Value('${crys.job.synchotpairs.enable}')
    def enable

    def execute() {
        def accounts = accountService.getActiveAccounts()
        accounts.each { Account a ->
            def names = new HashSet()
            names.addAll(a.funds.keySet())
            names.addAll(a.lockedFunds.keySet())
            def pairs = Pair.findAllByExchangeAndNameInList(a.exchange, names.collect {"${it}_btc"})
            pairs?.each { pair ->
                pairService.fullUpdate(pair, 1)
            }
        }
    }
}
