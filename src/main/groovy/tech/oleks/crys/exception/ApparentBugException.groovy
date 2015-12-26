package tech.oleks.crys.exception

/**
 * Created by alexm on 8/9/14.
 */
class ApparentBugException extends RuntimeException {
    ApparentBugException(Throwable throwable) {
        super(throwable)
    }

    ApparentBugException(String s) {
        super(s)
    }

    ApparentBugException(String s, Throwable throwable) {
        super(s, throwable)
    }
}
