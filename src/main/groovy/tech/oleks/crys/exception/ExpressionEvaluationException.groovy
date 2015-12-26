package tech.oleks.crys.exception

/**
 * Created by alexm on 9/20/14.
 */
class ExpressionEvaluationException extends Exception {

    ExpressionEvaluationException() {
    }

    ExpressionEvaluationException(String var1) {
        super(var1)
    }

    ExpressionEvaluationException(String var1, Throwable var2) {
        super(var1, var2)
    }

    ExpressionEvaluationException(Throwable var1) {
        super(var1)
    }
}
