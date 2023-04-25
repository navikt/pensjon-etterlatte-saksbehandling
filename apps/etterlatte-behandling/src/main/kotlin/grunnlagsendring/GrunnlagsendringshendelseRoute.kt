package no.nav.etterlatte.grunnlagsendring

import institusjonsopphold.KafkaOppholdHendelse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.sakId

internal fun Route.grunnlagsendringshendelseRoute(
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService
) {
    val logger = application.log

    route("/grunnlagsendringshendelse") {
        post("/doedshendelse") {
            val doedshendelse = call.receive<Doedshendelse>()
            logger.info("Mottar en doedshendelse fra PDL")
            grunnlagsendringshendelseService.opprettDoedshendelse(doedshendelse)
            call.respond(HttpStatusCode.OK)
        }

        post("/utflyttingshendelse") {
            val utflyttingsHendelse = call.receive<UtflyttingsHendelse>()
            logger.info("Mottar en utflyttingshendelse fra PDL")
            grunnlagsendringshendelseService.opprettUtflyttingshendelse(utflyttingsHendelse)
            call.respond(HttpStatusCode.OK)
        }

        post("/forelderbarnrelasjonhendelse") {
            val forelderBarnRelasjonHendelse = call.receive<ForelderBarnRelasjonHendelse>()
            logger.info("Mottar en forelder-barn-relasjon-hendelse fra PDL")
            grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse)
            call.respond(HttpStatusCode.OK)
        }

        post("/adressebeskyttelse") {
            val adressebeskyttelse = call.receive<Adressebeskyttelse>()
            logger.info("Mottar en adressebeskyttelse-hendelse fra PDL")
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
            call.respond(HttpStatusCode.OK)
        }

        post("/vergemaalellerfremtidsfullmakt") {
            val vergeMaalEllerFremtidsfullmakt = call.receive<VergeMaalEllerFremtidsfullmakt>()
            logger.info("Mottar en vergeMaalEllerFremtidsfullmakt-hendelse fra PDL")
            grunnlagsendringshendelseService.opprettVergemaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt)
            call.respond(HttpStatusCode.OK)
        }

        post("/institusjonsopphold") {
            val oppholdsHendelse = call.receive<KafkaOppholdHendelse>()
            logger.info("Mottar en institusjons-hendelse fra inst2")
            grunnlagsendringshendelseService.opprettInstitusjonsOppholdhendelse(oppholdsHendelse)
            call.respond(HttpStatusCode.OK)
        }

        post("/reguleringfeilet") {
            val hendelse = call.receive<ReguleringFeiletHendelse>()
            logger.info("Motter hendelse om at regulering har feilet i sak ${hendelse.sakId}")
            grunnlagsendringshendelseService.opprettEndretGrunnbeloepHendelse(hendelse.sakId)
            call.respond(HttpStatusCode.OK)
        }

        route("/{$SAKID_CALL_PARAMETER}") {
            get {
                call.respond(GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sakId)))
            }

            get("/gyldigehendelser") {
                call.respond(grunnlagsendringshendelseService.hentGyldigeHendelserForSak(sakId))
            }
        }
    }
}

data class GrunnlagsendringsListe(val hendelser: List<Grunnlagsendringshendelse>)
data class ReguleringFeiletHendelse(val sakId: Long)