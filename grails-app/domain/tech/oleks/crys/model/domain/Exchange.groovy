package tech.oleks.crys.model.domain

class Exchange {

    String exchangeId
    String name
    boolean active

    static hasMany = [pairs: Pair, accounts: Account]

    static constraints = {
        exchangeId(blank: false, nullable: false)
        name(blank: false, nullable: false)
    }

    static mapping = {
        table "CRYS_EXCHANGE"
    }

    String toString() {
        return "${getClass().simpleName}(" +
                "id: ${id}, " +
                "exchangeId: ${exchangeId}, " +
                "name: ${name}, " +
                "pairs: ${pairs?.size()})"
    }
}
