import grails.persistence.support.PersistenceContextInterceptor
import grails.util.Holders
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.exception.ApparentBugException
import tech.oleks.crys.model.domain.Exchange
import tech.oleks.crys.model.domain.Pair

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by alexm on 8/8/14.
 */
class SyncPairsJob {

    static triggers = {
        cron name: "evaluation", startDelay: 2000, cronExpression: '50 */10 * * * ?'
    }
    def description = "Exchange Pairs Evaluation"
    def concurrent = false
    def sessionRequired = true

    @Value('${crys.job.syncpairs.cacheMin}')
    int updatePairsMin
    @Value('${crys.job.syncpairs.threads}')
    int jobThreads
    @Value('${crys.job.syncpairs.waitThreadsSec}')
    int waitJobThreadsSec
    @Value('${crys.job.syncpairs.exchange.threads}')
    int exchangeThreads
    @Value('${crys.job.syncpairs.exchange.batchSize}')
    int exchangeBatchSize
    @Value('${crys.job.syncpairs.exchange.waitThreadsPerPairSec}')
    int waitThreadsPerPairSec
    @Value('${crys.sync.pair.cacheMin}')
    int cachePairMin

    def pairService

    def execute() {
        ExecutorService exec
        def exs = Exchange.findAllByActive(true)
        exs?.each { ex ->
            def pairs = pairService.pairsToEvaluate(ex, updatePairsMin)
            if (pairs) {
                if (!exec) {
                    exec = Executors.newFixedThreadPool(jobThreads)
                }
                exec.execute(new SyncPairsWorker(pairs: pairs))
            }
        }
        if (exec) {
            exec.shutdown()
            if (!exec.awaitTermination(waitJobThreadsSec, TimeUnit.SECONDS)) {
                throw new ApparentBugException("Job Pool Didn't terminate for given time ${waitJobThreadsSec} sec")
            }
        }
    }

    class SyncPairsWorker implements Runnable {
        List pairs
        @Override
        void run() {
            ExecutorService exec = Executors.newFixedThreadPool(exchangeThreads)
            def batches = pairs.collate(exchangeBatchSize)
            log.debug "batches: ${batches}"
            batches.each { batch ->
                exec.execute(new SyncPairWorker(pairs: batch))
            }
            exec.shutdown()
            int ttl = waitThreadsPerPairSec * pairs.size() / exchangeThreads
            if (!exec.awaitTermination(ttl, TimeUnit.SECONDS)) {
                throw new ApparentBugException("Pairs Pool Didn't terminate for given time ${ttl} sec")
            }
        }
    }

    class SyncPairWorker implements Runnable {
        List pairs
//        PersistenceContextInterceptor persistenceInterceptor
        @Override
        void run() {
            log.debug "running batch: ${pairs}"
            PersistenceContextInterceptor persistenceInterceptor = Holders.applicationContext.getBean("persistenceInterceptor")
            persistenceInterceptor.init()
            try {
                pairs.each { pid ->
                    Pair p = Pair.get(pid)
                    pairService.fullUpdate(p, cachePairMin)
                }
            }
            finally {
                persistenceInterceptor.flush()
                persistenceInterceptor.destroy()
            }
        }
    }
}
