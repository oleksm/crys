package tech.oleks.crys.service

import grails.transaction.Transactional
import org.joda.time.DateTime
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.model.domain.Order

@Transactional
class AccountService {

    def pairService

    List<Account> getActiveAccounts() {
        return Account.createCriteria().list {
            createAlias("exchange", "ex")
            and {
                eq("active", true)
                eq("ex.active", true)
            }
        }
    }

    List<Account> getActiveAccounts(Exchange ex) {
        return Account.findAllByActiveAndExchange(true, ex)
    }

    boolean fullUpdate(Account account, int min) {
        if (min > 0 && account.synced > new DateTime().minusMinutes(min).toDate()) {
            return
        }
        def es = ExchangeService.getExchangeService(account.exchange)
        es.updateOrders(account)
        es.updateBalance(account)
        account.synced = new Date()
        return true
    }

    def lockBalance(Order o, d = 1.0d) {
        def account = o.account
        def pair = o.pair
        def merch = pairService.merchCurrency(pair.name)
        def base = pairService.baseCurrency(pair.name)
        switch (o.type) {
            case Order.Type.sell:
                def f = account.funds[merch] ?: 0.0d
                account.funds[merch] = f - o.volume * d
                def lf = account.lockedFunds[merch] ?: 0.0d
                account.lockedFunds[merch] = lf + o.volume * d
                break
            case Order.Type.buy:
                def gamt = EvalUtils.gamt(o.volume, o.price, pair.tradeFee) * d
                def f = account.funds[base] ?: 0.0d
                account.funds[base] = f - gamt
                def lf = account.lockedFunds[base] ?: 0.0d
                account.lockedFunds[base] = lf + gamt
        }
    }

    def unlockBalance(Order o) {
        lockBalance(o, -1.0d)
    }

    def syncMyTrades(Account account) {
        def es = ExchangeService.getExchangeService(account.exchange)
        es.updateMyRecentTrades(account)
    }

    def updateOrders(Account account) {
        ExchangeService.getExchangeService(account.exchange).updateOrders(account)
    }

    def isFundsAvailableForByuing(Account a) {
        return a.funds['btc'] >= a.profile.minQuote
    }

    def getAllPairsForSell(Account acc) {
        def names = acc.funds?.collect {it.value > 0.0d && it.key != 'btc' ? "${it.key}_btc" : null}
        if (names) {
            return Pair.findAllByExchangeAndSlowAndNameInList(acc.exchange, false, names)
        }
    }

}
