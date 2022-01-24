package com.runetopic.xlitekt

import com.runetopic.xlitekt.network.awaitOnPort
import com.runetopic.xlitekt.plugin.ktor.installKoin
import io.ktor.application.Application
import io.ktor.server.engine.commandLineEnvironment
import java.util.TimeZone

fun main(args: Array<String>) = commandLineEnvironment(args).start()

fun Application.main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    installKoin()
    awaitOnPort(environment.config.property("ktor.deployment.port").getString().toInt())
}
