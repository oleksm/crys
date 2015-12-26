package tech.oleks.crys.model.domain

class Audit {
    Date created
    Map<String, String> attributes
    EventType eventType
    Entity entity

    static constraints = {
        created(blank: false, nullable: false)
        eventType(blank:false, nullable: false)
        entity(blank:false, nullable: false)
    }

    static mapping = {
        table "CRYS_AUDIT"
    }

    public enum EventType {
        Sync
    }

    public enum Entity {
        Pair, Trade, Exchange, Evaluation, Bid, Ask, Order
    }
}
