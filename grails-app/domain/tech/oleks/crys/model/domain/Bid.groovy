package tech.oleks.crys.model.domain

class Bid {

    Double volume
    Double price

    static belongsTo = [pair: Pair]

    static constraints = {
        volume(blank:false, nullable: false, min: 1.0E-8d)
        price(blank:false, nullable: false, min: 1.0E-8d)
        pair(nullable: false)
    }

    static mapping = {
        table "CRYS_BID"
    }

    @Override
    String toString() {
        return "${getClass().simpleName}(" +
                "id: ${id}, " +
                "volume: ${volume}, " +
                "price: ${price})" +
                "pair: ${pair?.name}"
    }
}
