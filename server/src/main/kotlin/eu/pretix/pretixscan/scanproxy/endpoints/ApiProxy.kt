package eu.pretix.pretixscan.scanproxy.endpoints

import eu.pretix.libpretixsync.db.*
import eu.pretix.pretixscan.scanproxy.Server
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.NotFoundResponse
import io.requery.Persistable

object EventEndpoint : Handler {
    override fun handle(ctx: Context) {
        val event = Server.syncData.select(Event::class.java)
            .where(Event.SLUG.eq(ctx.pathParam("event")))
            .get().firstOrNull() ?: throw NotFoundResponse("Event not found")
        ctx.json(event.json)
    }
}

abstract class ResourceEndpoint : Handler {
    abstract fun query(ctx: Context): List<RemoteObject>

    override fun handle(ctx: Context) {
        val res = query(ctx)
        ctx.json(mapOf(
            "count" to res.size,
            "next" to null,
            "previous" to null,
            "results" to res.map { it.json }
        ))
    }
}

abstract class CachedResourceEndpoint : ResourceEndpoint() {
    abstract val resourceName: String

    override fun handle(ctx: Context) {

        val rlm = Server.syncData.select(ResourceLastModified::class.java)
            .where(ResourceLastModified.RESOURCE.eq(resourceName))
            .and(ResourceLastModified.EVENT_SLUG.eq(ctx.pathParam("event")))
            .limit(1)
            .get().firstOrNull()
        if (rlm != null) {
            ctx.header("Last-Modified", rlm.getLast_modified())
            if (ctx.header("If-Modified-Since") == rlm.getLast_modified()){
                ctx.status(304)
                return
            }
        }

        val res = query(ctx)
        ctx.json(mapOf(
            "count" to res.size,
            "next" to null,
            "previous" to null,
            "results" to res.map { it.json }
        ))
    }
}


object CategoryEndpoint : CachedResourceEndpoint() {
    override val resourceName = "categories"
    override fun query(ctx: Context): List<RemoteObject> {
        return Server.syncData.select(ItemCategory::class.java)
            .where(ItemCategory.EVENT_SLUG.eq(ctx.pathParam("event")))
            .get().toList()
    }
}


object ItemEndpoint : CachedResourceEndpoint() {
    override val resourceName = "items"
    override fun query(ctx: Context): List<RemoteObject> {
        return Server.syncData.select(Item::class.java)
            .where(Item.EVENT_SLUG.eq(ctx.pathParam("event")))
            .get().toList()
    }
}


object QuestionEndpoint : CachedResourceEndpoint() {
    override val resourceName = "questions"
    override fun query(ctx: Context): List<RemoteObject> {
        return Server.syncData.select(Question::class.java)
            .where(Question.EVENT_SLUG.eq(ctx.pathParam("event")))
            .get().toList()
    }
}


object BadgeLayoutEndpoint : ResourceEndpoint() {
    override fun query(ctx: Context): List<RemoteObject> {
        return Server.syncData.select(BadgeLayout::class.java)
            .where(BadgeLayout.EVENT_SLUG.eq(ctx.pathParam("event")))
            .get().toList()
    }
}


object CheckInListEndpoint : CachedResourceEndpoint() {
    override val resourceName = "checkinlists"
    override fun query(ctx: Context): List<RemoteObject> {
        return Server.syncData.select(CheckInList::class.java)
            .where(CheckInList.EVENT_SLUG.eq(ctx.pathParam("event")))
            .get().toList()
    }
}


object BadgeItemEndpoint : ResourceEndpoint() {
    override fun query(ctx: Context): List<RemoteObject> {
        return Server.syncData.select(BadgeLayoutItem::class.java)
            .join(BadgeLayout::class.java).on(BadgeLayoutItem.LAYOUT_ID.eq(BadgeLayout.ID))
            .where(BadgeLayout.EVENT_SLUG.eq(ctx.pathParam("event")))
            .get().toList()
    }
}

object EmptyResourceEndpoint : Handler {
    override fun handle(ctx: Context) {
        ctx.json(
            mapOf(
                "count" to 0,
                "next" to null,
                "previous" to null,
                "results" to emptyList<Any>()
            )
        )
    }
}