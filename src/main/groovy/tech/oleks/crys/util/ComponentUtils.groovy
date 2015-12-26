package tech.oleks.crys.util

import grails.util.Holders
import tech.oleks.crys.service.EvaluationService

/**
 * Created by alexm on 9/14/14.
 */
class ComponentUtils {

    static EvaluationService evaluationService

    /**
     *
     * @return
     */
    public static EvaluationService getEvaluationService() {
        if (evaluationService == null) {
            evaluationService = Holders.applicationContext.getBean(EvaluationService.class)
        }
        return evaluationService
    }
}
