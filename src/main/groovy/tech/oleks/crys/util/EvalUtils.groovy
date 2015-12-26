package tech.oleks.crys.util

/**
 * Created by alexm on 8/14/14.
 */
class EvalUtils {
    def static dbl(Double v) {
        return v ?: 0.0d
    }

    def static amt(vol, price, fee) {
        def v = vol ?: 0.0d
        return v * price * (1.0d - fee)
    }

    def static gamt(vol, price, fee) {
        def v = vol ?: 0.0d
        return v * price / (1.0d - fee)
    }

    def static mergeFunds(Map f, Map lf) {
        def r = new HashMap(f)
        lf.entries.each { kv ->
            def v = r[kv.key] ?: 0.0d
            r[kv.key] = v + kv.value
        }
        return r
    }

    def static diffFunds(Map f, Map lf) {
        def r = new HashMap()
        f.entrySet().each { kv ->
            def lfv = lf[kv.key] ?: 0.0d
            double diff = kv.value - lfv
            if (diff != 0.0d) {
                r[kv.key] = diff
            }
        }
        lf.entrySet().each { kv->
            if (!f[kv.key] && kv.value != 0.0d) {
                r[kv.key] = -kv.value
            }
        }
        return r
    }
}