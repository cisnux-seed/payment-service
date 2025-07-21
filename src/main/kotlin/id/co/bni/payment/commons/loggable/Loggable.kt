package id.co.bni.payment.commons.loggable

import org.slf4j.LoggerFactory
import org.slf4j.Logger
import kotlin.jvm.java

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger(this::class.java)
}