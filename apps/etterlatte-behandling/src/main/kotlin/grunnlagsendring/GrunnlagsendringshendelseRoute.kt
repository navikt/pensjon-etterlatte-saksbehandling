package no.nav.etterlatte.grunnlagsendring

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdHendelseBeriket
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
import no.nav.etterlatte.libs.ktor.route.sakId
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal fun Route.grunnlagsendringshendelseRoute(grunnlagsendringshendelseService: GrunnlagsendringshendelseService) {
    val logger = LoggerFactory.getLogger("GrunnlagsendringhendelseRoute")

    route("/grunnlagsendringshendelse") {
        post("/doedshendelse") {
            kunSystembruker {
                val doedshendelse = call.receive<DoedshendelsePdl>()
                logger.info("Mottar en doedshendelse fra PDL for ${doedshendelse.fnr.maskerFnr()} hendelsesid: ${doedshendelse.hendelseId}")
                grunnlagsendringshendelseService.opprettDoedshendelse(doedshendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/utflyttingshendelse") {
            kunSystembruker {
                val utflyttingsHendelse = call.receive<UtflyttingsHendelse>()
                logger.info(
                    "Mottar en utflyttingshendelse fra PDL for ${utflyttingsHendelse.fnr.maskerFnr()} hendelsesid: ${utflyttingsHendelse.hendelseId}",
                )
                grunnlagsendringshendelseService.opprettUtflyttingshendelse(utflyttingsHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/forelderbarnrelasjonhendelse") {
            kunSystembruker {
                val forelderBarnRelasjonHendelse = call.receive<ForelderBarnRelasjonHendelse>()
                logger.info(
                    "Mottar en forelder-barn-relasjon-hendelse fra PDL for ${forelderBarnRelasjonHendelse.fnr.maskerFnr()} hendelsesid: ${forelderBarnRelasjonHendelse.hendelseId}",
                )
                grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        // Merk at denne endepunktet i seg selv gir ikke ny gradering, denne baserer seg på hva PDL sier om personen og 3-parter
        post("/adressebeskyttelse") {
            kunSystembruker {
                val adressebeskyttelse = call.receive<Adressebeskyttelse>()
                logger.info("Mottar en adressebeskyttelse-hendelse fra PDL hendelsesid: ${adressebeskyttelse.hendelseId}")
                grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/bostedsadresse") {
            kunSystembruker {
                val bostedsadresse = call.receive<Bostedsadresse>()
                logger.info(
                    "Mottar en adresse-hendelse fra PDL for ${bostedsadresse.fnr.maskerFnr()} hendelsesid: ${bostedsadresse.hendelseId}",
                )
                grunnlagsendringshendelseService.oppdaterAdresseHendelse(bostedsadresse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/vergemaalellerfremtidsfullmakt") {
            kunSystembruker {
                val vergeMaalEllerFremtidsfullmakt = call.receive<VergeMaalEllerFremtidsfullmakt>()
                logger.info(
                    "Mottar en vergeMaalEllerFremtidsfullmakt-hendelse fra PDL for ${vergeMaalEllerFremtidsfullmakt.fnr.maskerFnr()} hendelsesid: ${vergeMaalEllerFremtidsfullmakt.hendelseId}",
                )
                grunnlagsendringshendelseService.opprettVergemaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/sivilstandhendelse") {
            kunSystembruker {
                val sivilstandHendelse = call.receive<SivilstandHendelse>()
                logger.info(
                    "Mottar en sivilstand-hendelse fra PDL for ${sivilstandHendelse.fnr.maskerFnr()} hendelsesid: ${sivilstandHendelse.hendelseId}",
                )
                grunnlagsendringshendelseService.opprettSivilstandHendelse(sivilstandHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/folkeregisteridentifikatorhendelse") {
            kunSystembruker {
                val hendelse = call.receive<Folkeregisteridentifikatorhendelse>()
                logger.info(
                    "Mottar en folkeregisteridentifikator-hendelse fra PDL for ${hendelse.fnr.maskerFnr()} hendelsesid: ${hendelse.hendelseId}",
                )
                grunnlagsendringshendelseService.opprettFolkeregisteridentifikatorhendelse(hendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/institusjonsopphold") {
            kunSystembruker {
                val oppholdsHendelse = call.receive<InstitusjonsoppholdHendelseBeriket>()
                logger.info("Mottar institusjons-hendelse med ID ${oppholdsHendelse.hendelseId} fra inst2")
                grunnlagsendringshendelseService.opprettInstitusjonsOppholdhendelse(oppholdsHendelse)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/ufoeretrygd") {
            kunSystembruker {
                val hendelse = call.receive<UfoereHendelse>()
                logger.info("Mottar en hendelse fra uføre")
                grunnlagsendringshendelseService.opprettUfoerehendelse(hendelse)
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

data class UfoereHendelse(
    val personIdent: String,
    val fodselsdato: LocalDate,
    val virkningsdato: LocalDate,
    val vedtaksType: VedtaksType,
)

enum class VedtaksType {
    INNV,
    ENDR,
    OPPH,
}

internal enum class GrunnlagsendringshendelseFeatureToggle(
    private val key: String,
) : FeatureToggle {
    LOGG_MANGLENDE_EKTEFELLE_IDENT("logg-manglende-ektefelle-ident"),
    ;

    override fun key(): String = key
}
