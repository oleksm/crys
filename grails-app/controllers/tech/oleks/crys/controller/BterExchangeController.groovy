package tech.oleks.crys.controller

import tech.oleks.crys.model.domain.Pair

class BterExchangeController {

    def pairService

    def index() {
        if (params.pair) {
            def pair = Pair.findAllByName(params.pair)
            if (pair) {
                pairService.fullUpdate(pair)
            }
            else {
                log.warn "no pair ${params.pair}"
            }
        }
        render "OK!"
    }
}
