package tech.oleks.crys.controller

import org.joda.time.DateTime
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.model.domain.Pair
import tech.oleks.crys.model.domain.Trade
import tech.oleks.crys.util.GoogleChartUtils

class PairController {

    def scaffold = Pair

    def pairService
    def evaluationService

    def sync() {
        if (!params.eid) {
            render "param 'eid'"
            return
        }
        def ex = Exchange.findByExchangeId(params.eid)
        if (!ex) {
            render "no exchange ${params.eid}"
        }
        if (!params.pair) {
            render "need pair param"
            return
        }
        def pair =  Pair.findAllByExchangeAndName(ex, params.pair)
        if (!pair) {
            render "no pair ${params.pair}"
            return
        }
        pairService.fullUpdate(pair)
        render "OK!"
    }

    def trade() {
        def name = params.pair
        if (!name) {
            render 'pair param is required'
            return
        }

        int hr = params.hr ? Integer.valueOf(params.hr): 8
        Date cutoff = new DateTime().minusHours(hr).toDate()
        def trades = Trade.createCriteria().list {
            createAlias("pair", "p")
            createAlias("p.exchange", "ex")
            and {
                eq "p.name", name
                eq "p.slow", false
                eq "ex.active", true
                ge "timeStamp", cutoff
            }
            order "timeStamp", "asc"
        }

        def pairs = trades.collect {it.pair}.unique().sort()

        def r = new LinkedHashMap()
        trades?.each { Trade t ->
            def time = GoogleChartUtils.date(t.timeStamp)
            def rec = r[time]
            if (!rec) {
                rec = []
                r[time] = rec
            }
            if (rec.size() < pairs.size() * 2) {
                // Fill all columns
                pairs.each { p ->
                    rec.addAll([null, null])
                }
                // Fill current column
            }
            def i = pairs.indexOf(pairs.find {it.id == t.pair.id}) * 2 + (t.type == Trade.Type.sell ? 0 : 1)
            def p = rec[i]
            if (!p || (t.type == Trade.Type.buy && t.price > p) || (t.type == Trade.Type.sell && t.price < p)) {
                rec[i] = t.price
            }
        }

        def header = ['Time']
        // Fill HEADER
        pairs.each { p ->
            header.add("${p.exchange.name} ${Trade.Type.sell}")
            header.add("${p.exchange.name} ${Trade.Type.buy}")
        }
        def allPairs = Pair.createCriteria().listDistinct {
            projections {
                distinct "name"
            }
            createAlias("exchange", "ex")
            and {
                eq "ex.active", true
                eq "slow", false
                isNotEmpty "trades"
            }
            order "name"
        }

        def stats = []

        //Show Stats
        pairs.each {p ->
            def s = evaluationService.calcStats(p, hr * 60)
            if (s) {
                stats.add(new TreeMap(s.properties))
            }
        }


        [allPairs: allPairs,
         pairs: pairs,
         hr: hr,
         result: r,
         header: header,
         pair: name,
         stats: stats]
    }

    def chart() {
        def name = params.pair
        if (!name) {
            render 'pair param is required'
            return
        }
        def pairs = Pair.findAllByName(name)
        if (!pairs) {
            render "No pairs ${name} found"
            return
        }
        def tag = params.tag ?: 'hourly'
        int hr = params.hr ? Integer.valueOf(params.hr): 24 * 7 //1 week
        pairs.each { p ->


        }
        def stats = evaluationService.getStats(pairs.get(0), hr, tag)
        [stats: stats, pair: name, hr: hr, pairs: pairs]
    }

    def status() {
        def pairs = Pair.findAll()

        [pairs: pairs]
    }

    def ratio() {


    }
}
