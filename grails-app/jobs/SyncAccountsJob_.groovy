import grails.util.Holders
import org.springframework.beans.factory.annotation.Value

/**
 * Created by alexm on 8/9/14.
 */
class SyncAccountsJob_ {
    static triggers = {
        cron name: "sync-accounts", startDelay: 2000, cronExpression: '15 */5 * * * ?'
    }

    def description = "Sync Accounts Job"
    def concurrent = false
    def sessionRequired = true

    def accountService

    @Value('${crys.job.syncaccounts.enable}')
    def enable

    def execute() {
        def accounts = accountService.getActiveAccounts()
        accounts?.each { account ->
            accountService.fullUpdate(account, 2)
        }
    }
}
