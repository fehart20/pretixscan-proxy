package eu.pretix.pretixscan.scanproxy.endpoints

import eu.pretix.pretixscan.scanproxy.PretixScanConfig
import eu.pretix.pretixscan.scanproxy.Server
import eu.pretix.pretixscan.scanproxy.db.DownstreamDeviceEntity
import io.javalin.http.*
import io.requery.kotlin.eq

object DeviceAuth : Handler {
    override fun handle(ctx: Context) {
        val configStore = PretixScanConfig(Server.dataDir, "", null)
        if (!configStore.isConfigured) {
            throw ServiceUnavailableResponse("Not configured")
        }

        if (configStore.organizerSlug != ctx.pathParam("organizer")) {
            throw NotFoundResponse("Unknown organizer")
        }

        if (ctx.header("Authorization") == null) {
            throw UnauthorizedResponse("Authorization required")
        }
        val auth = ctx.header("Authorization")!!.split(" ")
        if (auth[0] != "Device") {
            throw UnauthorizedResponse("Device auth required")
        }

        val result = (Server.proxyData
                select (DownstreamDeviceEntity::class) where (DownstreamDeviceEntity::api_token eq auth[1])).get()
        val device = result.firstOrNull() ?: throw UnauthorizedResponse("Unknown device token")
    }
}

