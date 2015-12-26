package tech.oleks.crys.controller

import tech.oleks.crys.evaluation.Expressions
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Pair


class AccountController {

    def scaffold = Account
    def reportService

    def stats() {
        int hr = params.hr ? Integer.valueOf(params.hr): 24
        [reportItems: reportService.collectPairReport(hr), hr: hr]
    }

    def eval() {
        if (!params.aid) {
            return [accounts: Account.list()]
        }
        Account acc = Account.get(params.aid)
        if (acc && !params.pid) {
            return [acc: acc, pairs: Pair.findAllByExchangeAndSlow(acc.exchange, false, [sord: "name", order: "asc"])]
        }
        def pair = Pair.get(params.pid)
        def debug = params.debug != null ? Boolean.valueOf(params.debug) : true
        def exprs = new Expressions(account: acc, pair: pair, debug: debug)
        [acc: acc,
         pair: pair,
         bid: exprs.run('bidExpressions'),
         ask: exprs.run('askExpressions'),
         sell: exprs.run('sellExpressions'),
         watch: exprs.run('watchExpressions')
        ]
    }
}
