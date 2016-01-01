package tech.oleks.crys.service.util

import grails.plugins.rest.client.RestBuilder
import org.springframework.beans.factory.annotation.Value
import tech.oleks.crys.util.InvokeUtils

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.atomic.AtomicLong

class RestClientService implements  GroovyInterceptable {
    static transactional = false

    @Value('${crys.rest.client.useragent}')
    def userAgent
    @Value('${crys.rest.client.connectTimeout}')
    int connectTimeout
    @Value('${crys.rest.client.readTimeout}')
    int readTimeout

    AtomicLong nonceGen = new AtomicLong(System.currentTimeMillis())

    boolean active = true

    def get(def url) {
        log.debug "GET ${url} with useragent ${userAgent}"
        long start = System.currentTimeMillis()
        def resp = new RestBuilder(connectTimeout: connectTimeout, readTimeout: readTimeout).get(url) {
            header 'User-Agent', "${userAgent}"
        }
        log.debug "GET respond within ${System.currentTimeMillis() - start} ms"
        if (resp.status != 200) {
            log.error "rest call responded with ${resp.status}: '${url}'\n resp.responseEntity.body"
        }
        return resp
    }

    def securePost(def url, def key, def secret, def algorithm, def params = [:]) {
        // generate nonce
        def nonce = nonceGen.incrementAndGet()
        // final params list
        def form = [nonce: nonce]
        if (params) {
            form.putAll(params)
        }
        // to a query string
        def query = form.collect{k,v -> "$k=$v"}.join('&')
        // call resource
        log.debug "SECURE POST ${url} with useragent ${userAgent} and query: ${query}"
        long start = System.currentTimeMillis()
        def resp = new RestBuilder(connectTimeout: connectTimeout, readTimeout: readTimeout).post(url) {
            header 'User-Agent', "${userAgent}"
            header 'Content-Type', 'application/x-www-form-urlencoded'
            header 'KEY', key
            header 'SIGN', sign(query, secret, algorithm)
            body query
        }
        log.debug "POST respond within ${System.currentTimeMillis() - start} ms"
        if (resp.status != 200) {
            log.error "rest call responded with ${resp.status}: '${url}'\n resp.responseEntity.body"
        }
        return resp

    }

    def sign(String query, def secret, def algorithm) {
        def keySpec = new SecretKeySpec(secret.bytes, algorithm)
        def mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        def bytes = mac.doFinal(query.bytes)
        return bytes.encodeHex().toString()
    }

    def invokeMethod(String name, args) {
        if (active) {
            return InvokeUtils.invokeMethod(this, name, args)
        }
    }
}
