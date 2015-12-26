package tech.oleks.crys.controller

import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.model.domain.quartz.JobEvent
import tech.oleks.crys.util.GoogleChartUtils

class QuartzController {

    @Value('${crys.quartz.chart.hr}')
    int defaultHr

    def chart() {
        int hr = params.hr ? Integer.valueOf(params.hr) : 4//defaultHr
        def cutoff = new DateTime().minusHours(hr).toDate()
        def events = JobEvent.findAllByStartDateGreaterThanEquals(cutoff)
        def result = events?.collect { e ->
            DateTime start = new DateTime(e.startDate)
            DateTime end = start.plus(e.duration)
            def name = e.eventName ?: StringUtils.replace(e.errorMessage, "\n", " ") ?: ''

            [job: e.jobName, event: name, error: StringUtils.isNotBlank(e.errorMessage),
             start: GoogleChartUtils.date(start.toDate()),
             end: GoogleChartUtils.date(end.toDate())]
        }
        [events: result, hr: hr]
    }
}
