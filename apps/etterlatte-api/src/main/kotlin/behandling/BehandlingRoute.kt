package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


// /api
fun Route.behandlingRoute(service: BehandlingService) {

    route("saker") {
        //hent alle saker

        get {
            try {
                val accessToken = getAccessToken(call)
                val list = service.hentSaker(accessToken)
                call.respond(list)
            } catch (e: Exception) {
                throw e
            }
        }

        route("{sakId}") {
            // hent spesifikk sak (alle behandlinger?)
            get {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    call.respond(service.hentBehandlingerForSak(sakId, getAccessToken(call)))
                }
            }

            // Opprett behandling på sak
            post("behandlinger") {
                val sakId = call.parameters["sakId"]?.toLong()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    val testBehandlingsopplysning = Grunnlagsopplysning(
                        UUID.randomUUID(), Grunnlagsopplysning.Privatperson(
                            "11057523044",
                            LocalDateTime.now().toInstant(ZoneOffset.UTC)

                        ), Opplysningstyper.SOEKNAD_MOTTATT_DATO, objectMapper.createObjectNode(), objectMapper.createObjectNode()
                    )
                    val behandlingsBehov = BehandlingsBehov(sakId, listOf(testBehandlingsopplysning))
                    call.respond(service.opprettBehandling(behandlingsBehov, getAccessToken(call)))
                }
            }

            // Slett alle behandlinger på en sak
            delete("behandlinger") {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler");
                }
                else {
                   if(service.slettBehandlinger(sakId, getAccessToken(call))){
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }

    route("behandling/{behandlingId}") {
        get {
            val behandlingId = call.parameters["behandlingId"]?.toString()
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("BehandlingsId mangler")
            } else {
                call.respond(service.hentBehandling(behandlingId, getAccessToken(call)))
            }
        }

        get("hendelser") {
            val behandlingId = call.parameters["behandlingId"]
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("BehandlingsId mangler")
            } else {
                call.respond(service.hentHendelserForBehandling(behandlingId, getAccessToken(call)))
            }
        }

    }


    /*
    Skal hente persondata og sakene for denne personen?
     */
    route("personer") {
        get("{fnr}") {

            val fnr = call.parameters["fnr"]
            if (fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {
                try {
                    val accessToken = getAccessToken(call)
                    val list = service.hentPerson(fnr, accessToken)
                    call.respond(list)
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        /*
        Hente alle saker med metadata om saken
         */
        // Opprette saker på en person
        post("{fnr}/saker") {
            val fnr = call.parameters["fnr"]
            if (fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {
                try {
                    val accessToken = getAccessToken(call)
                    service.opprettSak(
                        fnr,
                        SoeknadType.GJENLEVENDEPENSJON,
                        accessToken
                    ) // sakType blir nok en enum etter hvert
                    call.respond("Ok");
                } catch (e: Exception) {
                    throw e
                }
            }
        }
    }

}