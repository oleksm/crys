package tech.oleks.crys.controller

import grails.util.Holders
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.service.ExchangeService

class ExchangeController {

    @Value('${crys.sync.pair.stats.tag}')
    String syncStatsTag
    @Value('${crys.exchange.chart.minAmount}')
    Double minAmount
    @Value('${crys.sync.pair.stats.step}')
    Double syncStepMin

    def scaffold = Exchange
    def evaluationService

    def sync() {
        def eid = params.eid
        if (eid) {
            def ex = Exchange.findByExchangeId(eid)
            if (ex) {
                def es = ExchangeService.getExchangeService(ex).updatePairs()
            }
            else {
                render "no exchange ${eid}"
            }
        }
        else {
            render "Requires param eid"
        }
    }

    def init() {
        Holders.applicationContext.getBeansOfType(ExchangeService.class).each {
            it.value.exchange
        }
    }

    def chart() {
        def stats = evaluationService.getStatsByTagMinAmt(syncStatsTag, 0.0d)
        int hr = syncStepMin / 60

        [stats: stats, hr: hr, pair_names: new HashSet(stats.collect {it.pair.name})]
    }

    def csv() {
        def b = new StringBuilder("timestamp,pair,exchange,amtpurchased,amtsold,buyprice,sellprice,bidamt,askamt,bidprice,askprice,buys,sells\n")
            .append("datetime,string,string,float,float,float,float,float,float,float,float,int,int\n")
            .append("T,C,C,,,,,,,,,,\n")
        def stats = evaluationService.getAllStatsByTagMinAmt(syncStatsTag, minAmount)
        stats?.each { s ->
            b.append("${s.timeStamp.format("yyyy-MM-dd HH:mm:ss")},${s.pair.name},${s.pair.exchange.name}," +
                    "${fmd(s.amtPurchased)},${fmd(s.amtSold)},${fmd(s.avgBuyPrice)},${fmd(s.avgSellPrice)}," +
                    "${fmd(s.bidAmt)},${fmd(s.askAmt)},${fmd(s.bidPrice)},${fmd(s.askPrice)},${s.buys},${s.sells}\n")
        }
        int step = syncStepMin / 60
        header "Content-Disposition", "inline;filename=s${stats.min{it.timeStamp}.timeStamp.format("yyMMddHHmmss")}_${step}"
        render (
                contentType: "text/csv",
                text: b.toString()
        )
    }

    def private static fmd(def d) {
        return d ? String.format("%.9f", d) : "0.0"
    }
}
