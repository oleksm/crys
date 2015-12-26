package tech.oleks.crys.service

import grails.transaction.Transactional
import org.hibernate.criterion.CriteriaSpecification
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.model.domain.Pair

@Transactional
class PairService {

    @Value('${crys.sync.pair.stats.tag}')
    String syncStatsTag

    def evaluationService

    def pairsToEvaluate(int min) {
        def cutoff = new DateTime().minusMinutes(min).toDate()
        return  Pair.createCriteria().list {
            createAlias("lastEvaluation", "eval", CriteriaSpecification.LEFT_JOIN)
            createAlias("exchange", "ex")
            and {
                eq("trade", true)
                eq("ex.active", true)
                eq("slow", false)
                or {
                    isNull("lastEvaluation")
                    lt("eval.created", cutoff)
                }
            }
        }
    }

    def activePairs() {
        return Pair.createCriteria().list {
            createAlias("exchange", "ex")
            and {
                eq("ex.active", true)
                eq("slow", false)
                isNotNull("synced")
            }
        }
    }

    def getPairsToTrade(def ex) {
        return Pair.createCriteria().list {
            eq "exchange", ex
            eq "slow", false
            eq "trade", true
        }
    }

    def findByName(def name) {
        return Pair.createCriteria().list {
            createAlias("exchange", "ex")
            and {
                eq("ex.active", true)
                eq("slow", false)
                eq("name", name)
            }
        }
    }

    List<Long> pairsToEvaluate(def ex, int min) {
        def cutoff = new DateTime().minusMinutes(min).toDate()
        return  Pair.createCriteria().list {
            projections {
                 property "id"
            }
            and {
                eq("trade", true)
                eq("exchange", ex)
                eq("slow", false)
                or {
                    isNull("synced")
                    lt("synced", cutoff)
                }
            }
        }
    }

    def List<Pair> getSlowPairs() {
        return  Pair.createCriteria().list {
            createAlias("exchange", "ex")
            and {
                eq("ex.active", true)
                eq("slow", true)
                ge("synced", new DateTime().minusHours(2).toDate())
            }
        }
    }

    def List<Pair> pairsToSell() {
        return Pair.createCriteria().list {
            createAlias("lastEvaluation", "eval")
            createAlias("exchange", "ex")
            and {
                eq("trade", true)
                eq("eval.sell", true)
                eq("ex.active", true)
            }
        }
    }

    def boolean fullUpdate(Pair pair, int min = 0) {
        log.debug "fullUpdate ${pair.id}"
        def es = ExchangeService.getExchangeService(pair.exchange)
        Date lastSynced = pair.synced
        es.updatePair(pair)
        evaluationService.addStats(pair, lastSynced, syncStatsTag)
        return true
    }

    def baseCurrency(String pair) {
        return pair.split('_')[1]
    }

    def merchCurrency(String pair) {
        return pair.split('_')[0]
    }

    def clearAllTrades(def ex) {
        def pairs = Pair.findAllByExchange(ex)
        pairs?.each { p ->
            p.trades?.collect{it}.each {t ->
                p.removeFromTrades(t)
                t.delete(failOnError: true)
            }
        }
    }
}
