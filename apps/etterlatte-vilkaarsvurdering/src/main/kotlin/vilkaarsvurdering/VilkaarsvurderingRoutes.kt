package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withParam
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.libs.ktor.receive
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import vilkaarsvurdering.VilkaarsvurderingAPI
import java.util.*

fun Route.vilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService, behandlingKlient: BehandlingKlient) {
    route(VilkaarsvurderingAPI.basisrute) {
        val logger = application.log

        get("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter vilkårsvurdering for $behandlingId")
                val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)

                if (vilkaarsvurdering != null) {
                    call.respond(vilkaarsvurdering.toDto())
                } else {
                    logger.info("Fant ingen vilkårsvurdering for behandling ($behandlingId)")
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        post(VilkaarsvurderingAPI.opprett("{$BEHANDLINGSID_CALL_PARAMETER}").url) {
            withBehandlingId(behandlingKlient) { behandlingId ->
                try {
                    logger.info("Oppretter vilkårsvurdering for $behandlingId")
                    val vilkaarsvurdering = vilkaarsvurderingService.opprettVilkaarsvurdering(behandlingId, bruker)

                    call.respond(vilkaarsvurdering.toDto())
                } catch (e: VirkningstidspunktIkkeSattException) {
                    logger.info("Virkningstidspunkt er ikke satt for behandling $behandlingId")
                    call.respond(HttpStatusCode.PreconditionFailed)
                } catch (e: BehandlingstilstandException) {
                    logger.error(
                        "Kunne ikke opprette vilkaarsvurdering for behandling $behandlingId. " +
                            "Statussjekk for behandling feilet"
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                }
            }
        }

        val kopier = VilkaarsvurderingAPI.kopier("{$BEHANDLINGSID_CALL_PARAMETER}")

        post(kopier.url) {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val forrigeBehandling = call.receive(kopier.clazz).forrigeBehandling

                try {
                    logger.info("Kopierer vilkårsvurdering for $behandlingId fra $forrigeBehandling")
                    val vilkaarsvurdering = vilkaarsvurderingService.kopierVilkaarsvurdering(
                        behandlingId = behandlingId,
                        kopierFraBehandling = forrigeBehandling,
                        bruker = bruker
                    )

                    call.respond(vilkaarsvurdering.toDto())
                } catch (e: VirkningstidspunktIkkeSattException) {
                    logger.info("Virkningstidspunkt er ikke satt for behandling $behandlingId")
                    call.respond(HttpStatusCode.PreconditionFailed)
                } catch (e: BehandlingstilstandException) {
                    logger.error(
                        "Kunne ikke opprette vilkaarsvurdering for behandling $behandlingId. " +
                            "Statussjekk for behandling feilet"
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                } catch (e: NullPointerException) {
                    logger.error(
                        "Kunne ikke kopiere vilkårsvurdering fra $forrigeBehandling. Fant ikke vilkårsvurdering"
                    )
                    call.respond(HttpStatusCode.NotFound, "Fant ikke vilkårsvurdering")
                }
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val vurdertVilkaarDto = call.receive<VurdertVilkaarDto>()
                val vurdertVilkaar = vurdertVilkaarDto.toVurdertVilkaar(bruker.ident())

                logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
                try {
                    val vilkaarsvurdering =
                        vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(behandlingId, bruker, vurdertVilkaar)
                    call.respond(vilkaarsvurdering.toDto())
                } catch (e: BehandlingstilstandException) {
                    logger.error(
                        "Kunne ikke oppdatere vilkaarsvurdering for behandling $behandlingId. " +
                            "Statussjekk for behandling feilet"
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                } catch (e: VilkaarsvurderingTilstandException) {
                    logger.error(e.message)
                    call.respond(
                        HttpStatusCode.PreconditionFailed,
                        "Kan ikke endre vurdering av vilkår på en vilkårsvurdering som har et resultat."
                    )
                }
            }
        }

        delete("/{$BEHANDLINGSID_CALL_PARAMETER}/{vilkaarId}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                withParam("vilkaarId") { vilkaarId ->
                    logger.info("Sletter vurdering på vilkår $vilkaarId for $behandlingId")
                    try {
                        val vilkaarsvurdering =
                            vilkaarsvurderingService.slettVurderingPaaVilkaar(behandlingId, bruker, vilkaarId)
                        call.respond(vilkaarsvurdering.toDto())
                    } catch (e: BehandlingstilstandException) {
                        logger.error(
                            "Kunne ikke slette vilkaarsvurdering for behandling $behandlingId. " +
                                "Statussjekk for behandling feilet"
                        )
                        call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                    } catch (e: VilkaarsvurderingTilstandException) {
                        logger.error(e.message)
                        call.respond(
                            HttpStatusCode.PreconditionFailed,
                            "Kan ikke slette vurdering av vilkår på en vilkårsvurdering som har et resultat."
                        )
                    }
                }
            }
        }

        route("/resultat") {
            post("/{$BEHANDLINGSID_CALL_PARAMETER}") {
                withBehandlingId(behandlingKlient) { behandlingId ->
                    val vurdertResultatDto = call.receive<VurdertVilkaarsvurderingResultatDto>()
                    val vurdertResultat = vurdertResultatDto.toVilkaarsvurderingResultat(
                        bruker.ident()
                    )

                    logger.info("Oppdaterer vilkårsvurderingsresultat for $behandlingId")
                    try {
                        val vilkaarsvurdering =
                            vilkaarsvurderingService.oppdaterTotalVurdering(behandlingId, bruker, vurdertResultat)
                        call.respond(vilkaarsvurdering.toDto())
                    } catch (e: BehandlingstilstandException) {
                        logger.error(
                            "Kunne ikke oppdatere total-vurdering for behandling $behandlingId. " +
                                "Statussjekk for behandling feilet"
                        )
                        call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                    }
                }
            }

            delete("/{$BEHANDLINGSID_CALL_PARAMETER}") {
                withBehandlingId(behandlingKlient) { behandlingId ->
                    logger.info("Sletter vilkårsvurderingsresultat for $behandlingId")
                    try {
                        val vilkaarsvurdering = vilkaarsvurderingService.slettTotalVurdering(behandlingId, bruker)
                        call.respond(vilkaarsvurdering.toDto())
                    } catch (e: BehandlingstilstandException) {
                        logger.error(
                            "Kunne ikke slette vilkårsvurderingsresultat for behandling $behandlingId. " +
                                "Statussjekk feilet for behandling feilet"
                        )
                        call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                    }
                }
            }
        }
    }
}

private fun VurdertVilkaarDto.toVurdertVilkaar(saksbehandler: String) =
    VurdertVilkaar(
        vilkaarId = vilkaarId,
        hovedvilkaar = hovedvilkaar,
        unntaksvilkaar = unntaksvilkaar,
        vurdering = VilkaarVurderingData(
            kommentar = kommentar,
            tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
            saksbehandler = saksbehandler
        )
    )

private fun VurdertVilkaarsvurderingResultatDto.toVilkaarsvurderingResultat(saksbehandler: String) =
    VilkaarsvurderingResultat(
        utfall = resultat,
        kommentar = kommentar,
        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
        saksbehandler = saksbehandler
    )

data class VurdertVilkaarDto(
    val vilkaarId: UUID,
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val kommentar: String?
)

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfall,
    val kommentar: String?
)