import grails.util.BuildSettings
import grails.util.Environment

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}

root(ERROR, ['STDOUT'])

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger{20} - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}
logger("tech.oleks", DEBUG, ['STDOUT'], false)
logger("grails.app", DEBUG, ['STDOUT'], false)
logger("grails.app.services.tech.oleks.crys.service.util.RestClientService", INFO, ['STDOUT'], false)
logger("grails.app.services.tech.oleks.crys.service.exchange.CryptsyExchangeService", INFO, ['STDOUT'], false)
logger("grails.app.services.tech.oleks.crys.service.exchange.BterExchangeService", INFO, ['STDOUT'], false)
logger("grails.app.services.grails.plugin", INFO, ['STDOUT'], false)
