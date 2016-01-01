package tech.oleks.crys.evaluation

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.joda.time.DateTime
import tech.oleks.crys.exception.ExpressionEvaluationException
import tech.oleks.crys.model.domain.Account
import tech.oleks.crys.model.domain.Pair
import tech.oleks.crys.model.domain.TradeProfile
import tech.oleks.crys.util.ComponentUtils

/**
 * Created by alexm on 9/14/14.
 */
public class Expressions {
    Account account
    Pair pair
    boolean debug
    GroovyShell shell

    def log = LogFactory.getLog(this.class)

    def run(def prop) {
        def pv = account.profile.getProperty(prop)
        Collection exprs = pv instanceof Collection ? pv : [pv]
        if (!exprs) {
            log.info "no expressions ${prop} to evaluate ${account.name}::${pair.name}"
            return
        }

        def start = System.currentTimeMillis()
        if (!shell) {
            Binding binding = new DynaBinding()
            binding.setVariable("a", account)
            binding.setVariable("ap", account.profile)
            binding.setVariable("pair", pair)
            def importCustomizer = new ImportCustomizer()
            importCustomizer.addImport 'org.joda.time.DateTime'
            def conf = new CompilerConfiguration()
            conf.addCompilationCustomizers(importCustomizer)
            shell = new GroovyShell(this.class.classLoader, binding, conf)
        }

        def report = ["Evaluation report for ${account.name}::${pair.name} prop:${prop}"]
        def result
        if (debug) {
            for (def expr: exprs) {
                try {
                    log.debug "evaluating expression for ${account.name} :: ${pair.name} :: ${expr}"
                    result = shell.evaluate(expr)
                }
                catch (Exception ex) {
                    throw new ExpressionEvaluationException("expression: ${expr}", ex)
                }
                report.add("${expr} : [${result}]")
                if (!result) {
                    break
                }
            }
        }
        else {
            def expr = exprs.collect {"(${it})"}.join(" && ")
            try {
                result = shell.evaluate(expr)
            }
            catch (Exception ex) {
                throw new ExpressionEvaluationException("account: ${account.name}(${account.id}), " + "pair: " +
                        "${pair.exchange.name}:${pair.name}(${pair.id}), expression: ${expr}", ex)
            }
            report.add("${expr} : [${result}]")
        }
        long execMills = System.currentTimeMillis() - start
        report.add("Executed within ${execMills} ms.")
        log.info(report.toString())
        return [result: result, report: report]
    }

    class DynaBinding extends Binding {
        @Override
        Object getVariable(String name) {
            if (!hasVariable(name)) {
                def v
                // test v
                if (name == 'k') {
                    v = 2
                }
                // Now sample
                if (name == 'n') {
                    v = ComponentUtils.evaluationService.getCurrentSampleByPairAndAccount(account, pair)
                }
                // Previous sample
                if (name == 'p') {
                    v = ComponentUtils.evaluationService.getPreviousSampleByPairAndAccount(account, pair)
                }
                // get stats for xx hr
                if (name.matches(/h\d+/)) {
                    def hr = name.find(/\d+/).toInteger()
                    v = ComponentUtils.evaluationService.calcStats(pair, new DateTime().minusHours(hr).toDate())
                }
                if (v) {
                    setVariable(name, v)
                }
            }
            return super.getVariable(name)
        }
    }

    def static main(def args) {
        def p = new Pair(
                name: "aaa_bbb"
        )
        def a = new Account(
                name: "test1",
                profile: new TradeProfile(bidExpressions: ["k >= 2", "3 > 2", "3 > 4"], quoteExpressions: "k * 2.4")
        )
        new Expressions(pair: p, account: a).run("bidExpressions")
        new Expressions(pair: p, account: a).run("bidExpressions")
        new Expressions(pair: p, account: a).run("quoteExpressions")
        def kk =  new Expressions(pair: p, account: a).run("quoteExpressions")
        System.out.println ("finally: ${kk * 2.1}")
    }
}
