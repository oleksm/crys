package tech.oleks.crys.model.domain

class Trade {
    Date timeStamp
    Double price
    Double volume
    String tid
    String refId
    String orderId
    Type type

    static belongsTo = [pair:Pair]

    static constraints = {
        timeStamp(blank: false, nullable: false)
        price(blank: false, nullable: false, min: 1.0E-8d)
        volume(blank: false, nullable: false, min: 1.0E-8d)
        tid(blank: false, nullable: false, matches: "[0-9]{5,15}")
        orderId(nullable: true)
        refId(nullable: true)
        type(blank: false, nullable: false)
        pair(nullable: false)

    }

    static mapping = {
        table "CRYS_TRADE"
    }

    @Override
    String toString() {
        return "${getClass().simpleName}(" +
                "id: ${id}, " +
                "timeStamp: ${timeStamp}, " +
                "price: ${price}, " +
                "volume: ${volume}, " +
                "tid: ${tid}, " +
                "type: ${type}, " +
                "pair: ${pair?.name}"
    }

    enum Type {
        sell, buy
    }
}
