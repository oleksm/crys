package tech.oleks.crys.model.domain

class Order {

    Date created
    Date closed
    Date acquired
    Date synced
    String refId
    String orderId
    Type type
    Status status
    CloseReason reason
    Double price
    Double orderedVolume
    Double volume
    Pair pair
    boolean reconciled

    static belongsTo = [account: Account]

    static constraints = {
        created(nullable: true)
        closed (nullable: true)
        reason (nullable: true)
        price(nullable: false, min: 1.0E-8d)
        volume(nullable: false, min: 0.0d)
        orderedVolume(nullable: false, min: 1.0E-8d)
        refId(nullable: false, matches: "[0-9]{5,15}", unique: true)
        orderId(nullable: true)
    }

    static mapping = {
        table "CRYS_ORDER"
    }

//    {
//        "id":"15088",
//        "sell_type":"BTC",
//        "buy_type":"LTC",
//        "sell_amount":"0.39901357",
//        "buy_amount":"12.0",
//        "pair":"ltc_btc",
//        "type":"buy",
//        "rate":0.033251,
//        "amount":"0.39901357",
//        "initial_rate":0.033251,
//        "initial_amount":"1"
//        "status":"open"
//    },


    enum Type {
        sell, buy
    }

    enum Status {
        open, closed, unknown
    }

    enum CloseReason {
        complete, cancel
    }
}
