package tech.oleks.crys.model.domain

class ConsistencyCheck {

    Date created
    Type type
    String source
    String bindTo
    Map<String, Object> attrs
    boolean passed

    static constraints = {
    }

    static mapping = {
        table "CRYS_CONSISTENCY_CHECK"
    }

    enum Type {
        LockFunds, UnlockFunds, CancelOrder, NewOrder
    }
}
