package tech.oleks.crys.util

import org.joda.time.DateTime

import java.math.RoundingMode

/**
 * Created by alexm on 8/30/14.
 */
class GoogleChartUtils {

    def static date(Date date) {
        DateTime t = new DateTime(date).minusMonths(1)
        return t.toDate().format("yyyy, MM, dd, HH, mm, ss")
//        return "${d.year}, ${d.monthOfYear}, ${d.dayOfMonth}, ${d.hourOfDay()}, ${d.minuteOfHour}, ${d.secondOfMinute}"
    }

    def static round(Double v) {
        return new BigDecimal(v).setScale(5, RoundingMode.HALF_UP).toDouble()
    }
}
