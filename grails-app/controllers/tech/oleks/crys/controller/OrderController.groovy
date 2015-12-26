package tech.oleks.crys.controller

import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.model.domain.Order
import tech.oleks.crys.model.domain.Pair

class OrderController {

    def scaffold = Order
    def orderService
    def pairService

    def sell() {
        if (params.pair) {
            def ex = Exchange.findByExchangeId("bter.com")
            def pair = Pair.findByNameAndExchange(params.pair, ex)
            def account = Account.findAllByExchange(ex).get(0)
            if (pair) {
                pairService.fullUpdate(pair)
                def order = orderService.prepareSellOrder(account, pair)
                if (order) {
                    orderService.placeOrder(order)
                    render "OK"
                }
                else {
                    render "no order created for account ${account} and pair ${pair}"
                }
            }
            else {
                render "No pair found for ${params.pair}"
            }
        }
        else {
            render "Need 'pair' param"
        }
    }

    def buy() {
        if (params.pair) {
            def ex = Exchange.findByExchangeId("bter.com")
            def pair = Pair.findByNameAndExchange(params.pair, ex)
            def account = Account.findAll().get(0)
            if (pair) {
                pairService.fullUpdate(pair)
                def order = orderService.prepareBuyOrder(account, pair)
                if (order) {
                    orderService.placeOrder(order)
                    render "OK"
                }
                else {
                    render "no order created for account ${account} and pair ${pair}"
                }
            }
            else {
                render "No pair found for ${params.pair}"
            }
        }
        else {
            render "Need 'pair' param"
        }
    }
}
