package tech.oleks.crys.service.ui

import grails.transaction.Transactional
import org.joda.time.DateTime
import tech.oleks.crys.model.PairReport
import tech.oleks.crys.model.domain.Order

@Transactional
class ReportService {
    def collectPairReport(int hr) {
        Date d = new DateTime().minusHours(hr).toDate()
        def orders = Order.createCriteria().list {
            or {
                ge "created", d
                ge "acquired", d
            }
        }
        def res = [:]
        orders?.each { o ->
            def pair = o.pair
            PairReport r = res[pair.name]
            if (!r) {
                res[pair.name] = r = new PairReport(
                        pair: pair.name
                )
            }
            ++r.places
            def amount = o.orderedVolume * o.price
            r.fees += amount * pair.tradeFee

            if (o.status == Order.Status.open) {
                r.locked += o.volume
                r.pending += o.volume * o.price
                ++r.open
            }

            if (o.reason == Order.CloseReason.cancel) {
                ++r.cancels
            }
            switch (o.type) {
                case Order.Type.sell: ++r.sells
                    if (o.status == Order.Status.closed) {
                        if (o.reason != Order.CloseReason.cancel) {
                            r.sold += o.orderedVolume
                            r.gain += amount
                        }
                    }
                    else {
                        r.sold += (o.orderedVolume - o.volume)
                        r.gain += o.price * (o.orderedVolume - o.volume)
                    }
                    break
                case Order.Type.buy: ++r.buys
                    if (o.status == Order.Status.closed) {
                        if (o.reason != Order.CloseReason.cancel) {
                            r.purchased += o.orderedVolume
                            r.spent += amount
                        }
                    }
                    else {
                        r.purchased += (o.orderedVolume - o.volume)
                        r.purchased += o.price * (o.orderedVolume - o.volume)
                    }
                    break
            }
        }
        def total = new PairReport()
        // final touch
        res.values().each { PairReport r ->
            r.profit = r.gain - r.spent - r.fees
            total.pair = "TOTAL"
            total.buys += r.buys
            total.cancels += r.cancels
            total.fees += r.fees
            total.gain += r.gain
            total.locked += r.locked
            total.open += r.open
            total.pending += r.pending
            total.places += r.places
            total.profit += r.profit
            total.sells += r.sells
            total.purchased += r.purchased
            total.sold += r.sold
            total.spent += r.spent
        }
        res.put(total.pair, total)
        return res.values()
    }
}
