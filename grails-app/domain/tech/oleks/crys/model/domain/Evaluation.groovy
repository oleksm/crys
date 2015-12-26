package tech.oleks.crys.model.domain
/**
 * Created by alexm on 8/3/14.
 */
class Evaluation {
    Date created
    Pair pair

    boolean allowBuy
    boolean allowSell
    Double qoute

    static belongsTo = [pair: Pair]

    static constraints = {
        created (nullable: false)
    }

    static mapping = {
        table "CRYS_EVALUATION"
    }

    String toString() {
        return "${getClass().simpleName}(" +
                "id: ${id}, " +
                "exchange: ${pair?.exchange?.exchangeId}, " +
                "pair: ${pair?.name}, " +
                "created: ${created})"
    }
}
