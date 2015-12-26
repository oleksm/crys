package tech.oleks.crys.model.domain

class Account {

//    String key
//    String secret

    Map<String, Double> funds
    Map<String, Double> lockedFunds
    Date synced
    boolean active
    String apiKey
    String secretKey
    String algorithm
    String name


    static hasMany = [orders: Order, funds: Double, lockedFunds: Double]
    static belongsTo = [exchange: Exchange, profile: TradeProfile]

    static mapping = {
        table "CRYS_ACCOUNT"
    }

    static constraints = {
        apiKey (nullable: true)
        secretKey (nullable: true)
        algorithm (nullable: true)
    }
}
