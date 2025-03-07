package eu.pretix.pretixscan.scanproxy.endpoints

import com.fasterxml.jackson.databind.JsonMappingException
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.json.JavalinJson
import org.slf4j.LoggerFactory

abstract class JsonBodyHandler<T>(private val bodyClass: Class<T>) : Handler {
    private val LOG = LoggerFactory.getLogger(JsonBodyHandler::class.java)

    abstract fun handle(ctx: Context, body: T)

    override fun handle(ctx: Context) {
        var body: Any?
        try {
            body = JavalinJson.fromJson(ctx.body(), bodyClass)
        } catch (e: JsonMappingException) {
            LOG.info(e.message)
            throw BadRequestResponse("Invalid JSON body")
        }
        handle(ctx, body)
    }

}

