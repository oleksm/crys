package tech.oleks.crys.util

/**
 * Created by alexm on 8/13/14.
 */
class InvokeUtils {
    def static invokeMethod(o, name, args) {
        def metaMethod = o.metaClass.getMetaMethod(name, args)
        return metaMethod.invoke(o, args)
    }

    def static invokeJob(o, name, args, log) {
        if (name == "execute") {
            log.debug "Started " + o.class.simpleName
            long start = System.currentTimeMillis()
            def r = invokeMethod(this, name, args)
            long exec = System.currentTimeMillis() - start
            log.debug "Finished " + this.class.simpleName + " in ${exec}ms"
            return r
        }
        return invokeMethod(this, name, args)
    }
}
