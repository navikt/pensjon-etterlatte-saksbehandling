package no.nav.etterlatte.tidshendelser.klient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.HentSakerRequest
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.tidshendelser.etteroppgjoer.EtteroppgjoerKonfigurasjon
import java.time.Duration
import java.time.YearMonth

class BehandlingKlient(
    private val behandlingHttpClient: HttpClient,
    private val behandlingUrl: String,
) {
    fun hentSaker(sakIder: List<SakId>): Map<SakId, Sak> {
        if (sakIder.isEmpty()) {
            return emptyMap()
        }

        return runBlocking {
            behandlingHttpClient
                .post("$behandlingUrl/saker/hent") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(
                        HentSakerRequest(
                            spesifikkeSaker = sakIder,
                            ekskluderteSaker = emptyList(),
                            sakType = null,
                            loependeFom = null,
                        ),
                    )
                }.body<SakerDto>()
        }.saker
    }

    fun hentSakerForPleieforholdetOpphoerte(doedsfallsmaaned: YearMonth): List<SakId> =
        runBlocking {
            behandlingHttpClient
                .get("$behandlingUrl/saker/pleieforholdet-opphoerte/$doedsfallsmaaned")
                .body()
        }

    fun hentSakerForFoedselsmaaned(foedselsmaaned: YearMonth): List<SakId> =
        runBlocking {
            behandlingHttpClient
                .get("$behandlingUrl/api/grunnlag/aldersovergang/$foedselsmaaned") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }.body()
        }

    fun hentSakerForDoedsfall(doedsfallsmaaned: YearMonth): List<SakId> =
        runBlocking {
            behandlingHttpClient
                .get("$behandlingUrl/api/grunnlag/doedsdato/$doedsfallsmaaned") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }.body()
        }

    fun hentSakerBpFylt18IMaaned(maaned: YearMonth): List<SakId> =
        runBlocking {
            behandlingHttpClient
                .get("$behandlingUrl/saker/bp-fyller-18-i-maaned/$maaned") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    timeout {
                        requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
                        socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
                        connectTimeoutMillis = Duration.ofSeconds(30).toMillis()
                    }
                }.body()
        }

    fun hentSkjermedeSakerBarnepensjon(): List<SakId> =
        runBlocking {
            behandlingHttpClient
                .get("$behandlingUrl/egenansatt/saker/${SakType.BARNEPENSJON.name}") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }.body()
        }

    fun startOpprettelseAvEtteroppgjoerForbehandling(etteroppgjoerKonfigurasjon: EtteroppgjoerKonfigurasjon) {
        runBlocking {
            behandlingHttpClient.post("$behandlingUrl/api/etteroppgjoer/forbehandling/bulk") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    StartOpprettelseAvEtteroppgjoerForbehandlingRequest(
                        inntektsaar = etteroppgjoerKonfigurasjon.inntektsaar,
                        antall = etteroppgjoerKonfigurasjon.antall,
                        etteroppgjoerFilter = etteroppgjoerKonfigurasjon.etteroppgjoerFilter,
                        spesifikkeSaker = etteroppgjoerKonfigurasjon.spesifikkeSaker,
                        ekskluderteSaker = etteroppgjoerKonfigurasjon.ekskluderteSaker,
                        spesifikkeEnheter = etteroppgjoerKonfigurasjon.spesifikkeEnheter,
                    ),
                )
            }
        }
    }
}

data class SakerDto(
    val saker: Map<SakId, Sak>,
)

data class StartOpprettelseAvEtteroppgjoerForbehandlingRequest(
    val inntektsaar: Int,
    val antall: Int,
    val etteroppgjoerFilter: EtteroppgjoerFilter,
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
    val spesifikkeEnheter: List<String>,
)
