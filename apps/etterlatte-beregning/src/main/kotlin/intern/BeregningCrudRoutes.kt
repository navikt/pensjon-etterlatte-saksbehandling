package no.nav.etterlatte.intern

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.etteroppgjoer.BeregnetEtteroppgjoerResultat
import no.nav.etterlatte.avkorting.etteroppgjoer.EtteroppgjoerRepository
import no.nav.etterlatte.beregning.AnvendtTrygdetidRepository
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.OverstyrBeregning
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.parseUuidParameter
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonRepository
import java.util.UUID

fun Route.beregningCrudRoutes(
    beregningRepository: BeregningRepository,
    beregningsGrunnlagRepository: BeregningsGrunnlagRepository,
    anvendtTrygdetidRepository: AnvendtTrygdetidRepository,
    avkortingRepository: AvkortingRepository,
    etteroppgjoerRepository: EtteroppgjoerRepository,
    sanksjonRepository: SanksjonRepository,
    behandlingApp: String,
    aktivert: Boolean,
) {
    val tilgangPlugin =
        createRouteScopedPlugin("BeregningCrudTilgang") {
            on(AuthenticationChecked) { call ->
                val bruker = call.brukerTokenInfo
                if (bruker !is Systembruker || bruker.ident() != behandlingApp) {
                    throw IkkeTillattException(
                        code = "KUN_BEHANDLING",
                        detail = "Bare behandling har tilgang til beregning sine interne repository-operasjoner",
                    )
                }
            }
        }

    if (aktivert) {
        route("/api/beregning/intern") {
            install(tilgangPlugin)
            beregningRepositoryRoutes(beregningRepository)
            beregningsGrunnlagRepositoryRoutes(beregningsGrunnlagRepository)
            anvendtTrygdetidRepositoryRoutes(anvendtTrygdetidRepository)
            avkortingRepositoryRoutes(avkortingRepository)
            etteroppgjoerRepositoryRoutes(etteroppgjoerRepository)
            sanksjonRepositoryRoutes(sanksjonRepository)
        }
    }
}

private fun Route.beregningRepositoryRoutes(repository: BeregningRepository) {
    route("/beregning") {
        get("/{behandlingId}") {
            call.respond(repository.hent(behandlingId) ?: throw GenerellIkkeFunnetException())
        }

        put {
            call.respond(repository.lagreEllerOppdaterBeregning(call.receiveCrudRequest<Beregning>()))
        }

        delete("/{behandlingId}/overstyrt-grunnlag") {
            repository.slettOverstyrtBeregningsgrunnlag(behandlingId)
            call.respond(HttpStatusCode.NoContent)
        }

        route("/overstyring") {
            get("/{sakId}") {
                call.respond(repository.hentOverstyrBeregning(sakId) ?: throw GenerellIkkeFunnetException())
            }

            put {
                val overstyring = repository.opprettOverstyrBeregning(call.receiveCrudRequest<OverstyrBeregning>())
                if (overstyring == null) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(overstyring)
                }
            }

            delete("/{sakId}") {
                repository.deaktiverOverstyrtBeregning(sakId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Route.beregningsGrunnlagRepositoryRoutes(repository: BeregningsGrunnlagRepository) {
    route("/beregningsgrunnlag") {
        get("/{behandlingId}") {
            call.respond(repository.finnBeregningsGrunnlag(behandlingId) ?: throw GenerellIkkeFunnetException())
        }

        put {
            call.respond(repository.lagreBeregningsGrunnlag(call.receiveCrudRequest<BeregningsGrunnlag>()))
        }

        get("/{behandlingId}/overstyrt") {
            call.respond(repository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId))
        }

        put("/{behandlingId}/overstyrt") {
            val grunnlag = call.receiveCrudRequest<List<OverstyrBeregningGrunnlagDao>>()
            if (grunnlag.any { it.behandlingId != behandlingId }) {
                throw UgyldigForespoerselException(
                    code = "ULIK_BEHANDLING_ID",
                    detail = "BehandlingId i grunnlaget må være lik behandlingId i URL-en",
                )
            }
            repository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, grunnlag)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Route.anvendtTrygdetidRepositoryRoutes(repository: AnvendtTrygdetidRepository) {
    post("/anvendt-trygdetid/{behandlingId}") {
        val request = call.receiveCrudRequest<LagreAnvendtTrygdetidRequest>()
        call.respond(
            HttpStatusCode.Created,
            repository.lagreAnvendtTrygdetid(behandlingId, request.utrekning, request.anvendt),
        )
    }
}

private fun Route.avkortingRepositoryRoutes(repository: AvkortingRepository) {
    route("/avkorting") {
        get("/{behandlingId}") {
            call.respond(repository.hentAvkorting(behandlingId) ?: throw GenerellIkkeFunnetException())
        }

        put("/{behandlingId}") {
            val request = call.receiveCrudRequest<LagreAvkortingRequest>()
            repository.lagreAvkorting(behandlingId, request.sakId, request.avkorting)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{behandlingId}") {
            repository.slettForBehandling(behandlingId)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/faktisk-inntekt/{aarsoppgjoerId}") {
            val aarsoppgjoerId = call.parameters.parseUuidParameter("aarsoppgjoerId")
            call.respond(repository.hentFaktiskInntekt(aarsoppgjoerId) ?: throw GenerellIkkeFunnetException())
        }

        get("/sak/{sakId}/aarsoppgjoer") {
            call.respond(repository.hentAlleAarsoppgjoer(sakId))
        }

        get("/sak/{sakId}/behandlinger") {
            call.respond(repository.hentAlleBehandlingerMedAarsoppgjoer(sakId))
        }

        post("/aarsoppgjoer") {
            call.respond(repository.hentAlleAarsoppgjoer(call.receiveCrudRequest<List<UUID>>()))
        }

        post("/inntekt-for-aar") {
            call.respond(repository.harSakInntektForAar(call.receiveCrudRequest<InntektsjusteringAvkortingInfoRequest>()))
        }
    }
}

private fun Route.etteroppgjoerRepositoryRoutes(repository: EtteroppgjoerRepository) {
    route("/etteroppgjoer/resultat") {
        get("/{forbehandlingId}/{aar}/{sisteIverksatteBehandlingId}") {
            val forbehandlingId = call.parameters.parseUuidParameter("forbehandlingId")
            val sisteIverksatteBehandlingId = call.parameters.parseUuidParameter("sisteIverksatteBehandlingId")
            val aar =
                call.parameters["aar"]?.toIntOrNull()
                    ?: throw UgyldigForespoerselException("UGYLDIG_AAR", "År må være et heltall")
            call.respond(
                repository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)
                    ?: throw GenerellIkkeFunnetException(),
            )
        }

        put {
            repository.lagreEtteroppgjoerResultat(call.receiveCrudRequest<BeregnetEtteroppgjoerResultat>())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Route.sanksjonRepositoryRoutes(repository: SanksjonRepository) {
    route("/sanksjon") {
        get("/behandling/{behandlingId}") {
            call.respond(repository.hentSanksjon(behandlingId))
        }

        post("/behandling/{behandlingId}") {
            val request = call.receiveCrudRequest<OpprettSanksjonRequest>()
            repository.opprettSanksjon(behandlingId, request.sakId, request.saksbehandlerIdent, request.sanksjon)
            call.respond(HttpStatusCode.Created)
        }

        post("/behandling/{behandlingId}/kopi") {
            val request = call.receiveCrudRequest<OpprettSanksjonFraKopiRequest>()
            repository.opprettSanksjonFraKopi(behandlingId, request.sakId, request.sanksjon)
            call.respond(HttpStatusCode.Created)
        }

        put {
            val request = call.receiveCrudRequest<OppdaterSanksjonRequest>()
            if (request.sanksjon.id == null) {
                throw UgyldigForespoerselException("MANGLER_SANKSJON_ID", "Sanksjon må ha en id ved oppdatering")
            }
            repository.oppdaterSanksjon(request.sanksjon, request.saksbehandlerIdent)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/{sanksjonId}") {
            val sanksjonId = call.parameters.parseUuidParameter("sanksjonId")
            call.respond(repository.hentSanksjonMedId(sanksjonId) ?: throw GenerellIkkeFunnetException())
        }

        delete("/{sanksjonId}") {
            val sanksjonId = call.parameters.parseUuidParameter("sanksjonId")
            call.respond(repository.slettSanksjon(sanksjonId))
        }
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.receiveCrudRequest(): T =
    try {
        receive<T>()
    } catch (e: BadRequestException) {
        throw UgyldigForespoerselException(
            "UGYLDIG_CRUD_REQUEST",
            "Forespørselen har ugyldig JSON eller mangler påkrevde felter",
            cause = e,
        )
    } catch (e: ContentTransformationException) {
        throw UgyldigForespoerselException("UGYLDIG_CRUD_REQUEST", "Forespørselen må inneholde JSON", cause = e)
    }
