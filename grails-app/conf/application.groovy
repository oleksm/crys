/**
 * Created by alexm on 8/2/14.
 */
crys.rest.client.useragent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.153 Safari/537.36"
crys.rest.client.connectTimeout = 1000
crys.rest.client.readTimeout = 30000

crys.exchange {
    accept.pairs.regex = ".+_btc"
    cryptsy.tradeFee = 0.0025
    cryptsy.minPriceMovement = 8
    cryptsy.minTradeAmount = 0.00000011
    cryptsy.dateFormat = "yyyy-MM-dd HH:mm:ss" //2014-08-16 16:45:19
    cryptsy.timeZone = "GMT-4"
    cryptsy.secureUrl = "https://api.cryptsy.com/api"
    chart.minAmount=1.0d
}

//    cronExpression: "s m h D M W Y"
//    | | | | | | `- Year [optional]
//    | | | | | `- Day of Week, 1-7 or SUN-SAT, ?
//    | | | | `- Month, 1-12 or JAN-DEC
//    | | | `- Day of Month, 1-31, ?
//    | | `- Hour, 0-23
//    | `- Minute, 0-59
//    `- Second, 0-59

crys.job.orderbuy.enable=false
crys.job.ordercancel.enable=false
crys.job.orderreconcile.enable=false
crys.job.ordersell.enable=false
crys.job.syncaccounts.enable=false
crys.job.syncexchange.schedule = '0 0 */8 * * ?'
crys.job.synchotpairs.enable=false

crys.job.syncpairs {
    enable=false
    cacheMin=20
    threads=5
    waitThreadsSec=300
    exchange {
        threads = 3
        batchSize=6
        waitThreadsPerPairSec=15
    }
}

crys.pair.stats.depth.lookup=0.05
crys.pair.sync.slowTreshold24 = 1.0


crys.job.syncslowpairs.enable=false

crys.handle.manual.orders = false

crys.quartz.chart.hr = 8

crys.order.minPriceStep=2

crys.sync.pair.cacheMin=2
crys.sync.pair.stats.step=60*12
crys.sync.pair.stats.tag="sync-pairs"

crys {
    ai {
        campaign {
            policy {
                hangup.order.expiration = 24
            }
            evaluation {
                buy {
                    // buy sell volume ratio hr1
                    bsvr1.min = 1.01
                    // buy sell volume ratio hr2
                    bsvr2.min = 1.01
                    // weighed spread %
                    wsp.min = 0.012
                    // estimated profit %
                    epp.min = 0.01
                    // velocity quote ratio hr1
                    vqr1.min = 25.0
                    // velocity quote ratio hr2
                    vqr2.scale.min = 0.4
                    // or vqr2 more than this rate:
                    vqr2.indulge.min = 50.0

                }
                sell {
                    bsvr1.max = 3.0
                    bsvr1.min = 1.0
                    //bsvr2.max = 1.5
                    // tranding down ratio
                    tr1=0.88
                    // velocity quote ratio hr1
                    vqr1.min = 5.0
                    order.quote.multiplier = 2.0
                    order.quote.sweep.multiplier = 1.5
                    pair.order.interval.min = 15
                }
                cancel {
                    depthAbove.multiplier.max = 0.5
                }
                // short sample tense I, hr
                hr1 = 2
                // short sample tense II, hr
                hr2 = 6
                // Max Quote allowed for single order
                order.quote.max = 0.011
                // Max quote across all orders for the pair
                pair.quote.max = 0.07
                pair.open.orders.max = 3
                // Minimum interval between orders by the pair
                pair.order.interval.min = 20
            }
        }
    }
}