package health

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.healthApi() {
    route("health") {
        get("isalive") {
            call.respond(HttpStatusCode.OK)
        }

        get("isready") {
            call.respond(HttpStatusCode.OK)
        }
    }
}
