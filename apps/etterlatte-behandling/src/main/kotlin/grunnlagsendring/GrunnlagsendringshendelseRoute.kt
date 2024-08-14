package no.nav.etterlatte.grunnlagsendring

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdHendelseBeriket
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Folkeregisteridentifikatorhendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId

internal fun Route.grunnlagsendringshendelseRoute(grunnlagsendringshendelseService: GrunnlagsendringshendelseService) {
    val logger = routeLogger

    route("/grunnlagsendringshendelse") {
        post("/doedshendelse") {
            kunSystembruker {
                val doedshendelse = call.receive<DoedshendelsePdl>()
                logger.info("Mottar en doedshendelse fra PDL for ${doedshendelse.fnr.maskerFnr()}")
                grunnlagsendringshendelseService.opprettDoedshendelse(doedshendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/utflyttingshendelse") {
            kunSystembruker {
                val utflyttingsHendelse = call.receive<UtflyttingsHendelse>()
                logger.info("Mottar en utflyttingshendelse fra PDL for ${utflyttingsHendelse.fnr.maskerFnr()}")
                grunnlagsendringshendelseService.opprettUtflyttingshendelse(utflyttingsHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/forelderbarnrelasjonhendelse") {
            kunSystembruker {
                val forelderBarnRelasjonHendelse = call.receive<ForelderBarnRelasjonHendelse>()
                logger.info("Mottar en forelder-barn-relasjon-hendelse fra PDL for ${forelderBarnRelasjonHendelse.fnr.maskerFnr()}")
                grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/adressebeskyttelse") {
            kunSystembruker {
                val adressebeskyttelse = call.receive<Adressebeskyttelse>()
                logger.info("Mottar en adressebeskyttelse-hendelse fra PDL")
                grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/bostedsadresse") {
            kunSystembruker {
                val bostedsadresse = call.receive<Bostedsadresse>()
                logger.info("Mottar en adresse-hendelse fra PDL for ${bostedsadresse.fnr.maskerFnr()}")
                grunnlagsendringshendelseService.oppdaterAdresseHendelse(bostedsadresse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/vergemaalellerfremtidsfullmakt") {
            kunSystembruker {
                val vergeMaalEllerFremtidsfullmakt = call.receive<VergeMaalEllerFremtidsfullmakt>()
                logger.info(
                    "Mottar en vergeMaalEllerFremtidsfullmakt-hendelse fra PDL for ${vergeMaalEllerFremtidsfullmakt.fnr.maskerFnr()}",
                )
                grunnlagsendringshendelseService.opprettVergemaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/sivilstandhendelse") {
            kunSystembruker {
                val sivilstandHendelse = call.receive<SivilstandHendelse>()
                logger.info("Mottar en sivilstand-hendelse fra PDL for ${sivilstandHendelse.fnr.maskerFnr()}")
                grunnlagsendringshendelseService.opprettSivilstandHendelse(sivilstandHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/folkeregisteridentifikatorhendelse") {
            kunSystembruker {
                val hendelse = call.receive<Folkeregisteridentifikatorhendelse>()
                logger.info("Mottar en folkeregisteridentifikator-hendelse fra PDL for ${hendelse.fnr.maskerFnr()}")
                grunnlagsendringshendelseService.opprettFolkeregisteridentifikatorhendelse(hendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/institusjonsopphold") {
            kunSystembruker {
                val oppholdsHendelse = call.receive<InstitusjonsoppholdHendelseBeriket>()
                logger.info("Mottar en institusjons-hendelse fra inst2")
                grunnlagsendringshendelseService.opprettInstitusjonsOppholdhendelse(oppholdsHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/{$SAKID_CALL_PARAMETER}") {
            get {
                call.respond(GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sakId)))
            }

            get("/gyldigehendelser") {
                call.respond(inTransaction { grunnlagsendringshendelseService.hentGyldigeHendelserForSak(sakId) })
            }
        }
    }
    route("/api/grunnlagsendringshendelse/{$SAKID_CALL_PARAMETER}/institusjon") {
        get {
            call.respond(
                grunnlagsendringshendelseService
                    .hentAlleHendelserForSakAvType(sakId, GrunnlagsendringsType.INSTITUSJONSOPPHOLD),
            )
        }
    }
}

data class GrunnlagsendringsListe(
    val hendelser: List<Grunnlagsendringshendelse>,
)
