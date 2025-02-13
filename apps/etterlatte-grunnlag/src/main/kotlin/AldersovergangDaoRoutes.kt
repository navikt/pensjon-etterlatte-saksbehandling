package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.sak.tilSakId
import java.time.YearMonth

fun Route.aldersovergangDaoRoutes(aldersovergangDao: AldersovergangDao) {
    route("/dao") {
        get("/hentSoekereFoedtIEnGittMaaned") {
            val maaned = YearMonth.parse(call.request.queryParameters["maaned"]!!)

            val soekere = aldersovergangDao.hentSoekereFoedtIEnGittMaaned(maaned)

            call.respond(soekere)
        }

        get("/hentSakerHvorDoedsfallForekomIGittMaaned") {
            val maaned = YearMonth.parse(call.request.queryParameters["maaned"]!!)

            val saker = aldersovergangDao.hentSakerHvorDoedsfallForekomIGittMaaned(maaned)

            call.respond(saker)
        }

        get("/hentFoedselsdato") {
            val sakId = call.request.queryParameters["sakId"]!!.tilSakId()
            val opplysningType = Opplysningstype.valueOf(call.request.queryParameters["opplysningType"]!!)

            val foedselsdato = aldersovergangDao.hentFoedselsdato(sakId, opplysningType)

            call.respond(foedselsdato ?: HttpStatusCode.OK)
        }
    }
}
