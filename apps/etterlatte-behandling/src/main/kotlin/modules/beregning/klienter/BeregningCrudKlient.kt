package no.nav.etterlatte.modules.beregning.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.modules.beregning.AnvendtTrygdetidRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.AvkortingRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.Beregning
import no.nav.etterlatte.modules.beregning.BeregningRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.BeregningsGrunnlagRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.EtteroppgjoerRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.OverstyrBeregning
import no.nav.etterlatte.modules.beregning.SanksjonRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.anvendttrygdetid.AnvendtTrygdetidPeriodeUtrekning
import no.nav.etterlatte.modules.beregning.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.FaktiskInntekt
import no.nav.etterlatte.modules.beregning.etteroppgjoer.BeregnetEtteroppgjoerResultat
import no.nav.etterlatte.modules.beregning.sanksjon.LagreSanksjon
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.UUID

class BeregningCrudKlient(
    private val httpClient: HttpClient,
    resourceUrl: String,
) : BeregningRepositoryOperasjoner,
    BeregningsGrunnlagRepositoryOperasjoner,
    AnvendtTrygdetidRepositoryOperasjoner,
    AvkortingRepositoryOperasjoner,
    EtteroppgjoerRepositoryOperasjoner,
    SanksjonRepositoryOperasjoner {
    private val logger = LoggerFactory.getLogger(BeregningCrudKlient::class.java)
    private val url = "${resourceUrl.trimEnd('/')}/api/beregning/intern"

    override fun hent(behandlingId: UUID): Beregning? = hentValgfritt("/beregning/$behandlingId")

    override fun lagreEllerOppdaterBeregning(beregning: Beregning): Beregning =
        medSvar(HttpMethod.Put, "/beregning", beregning, referanse = "behandlingId=${beregning.behandlingId}")

    override fun slettOverstyrtBeregningsgrunnlag(behandlingId: UUID) =
        utenSvar(HttpMethod.Delete, "/beregning/$behandlingId/overstyrt-grunnlag")

    override fun hentOverstyrBeregning(sakId: SakId): OverstyrBeregning? = hentValgfritt("/beregning/overstyring/${sakId.sakId}")

    override fun opprettOverstyrBeregning(overstyrBeregning: OverstyrBeregning): OverstyrBeregning? =
        runBlocking {
            kall(
                HttpMethod.Put,
                "/beregning/overstyring",
                overstyrBeregning,
                setOf(HttpStatusCode.OK, HttpStatusCode.NoContent),
                referanse = "sakId=${overstyrBeregning.sakId.sakId}",
            ) { response ->
                if (response.status == HttpStatusCode.NoContent) null else response.body<OverstyrBeregning>()
            }
        }

    override fun deaktiverOverstyrtBeregning(sakId: SakId) = utenSvar(HttpMethod.Delete, "/beregning/overstyring/${sakId.sakId}")

    override fun finnBeregningsGrunnlag(id: UUID): BeregningsGrunnlag? = hentValgfritt("/beregningsgrunnlag/$id")

    override fun lagreBeregningsGrunnlag(beregningsGrunnlag: BeregningsGrunnlag): Boolean =
        medSvar(
            HttpMethod.Put,
            "/beregningsgrunnlag",
            beregningsGrunnlag,
            referanse = "behandlingId=${beregningsGrunnlag.behandlingId}",
        )

    override fun finnOverstyrBeregningGrunnlagForBehandling(id: UUID): List<OverstyrBeregningGrunnlagDao> =
        medSvar(HttpMethod.Get, "/beregningsgrunnlag/$id/overstyrt")

    override fun lagreOverstyrBeregningGrunnlagForBehandling(
        behandlingId: UUID,
        data: List<OverstyrBeregningGrunnlagDao>,
    ) = utenSvar(HttpMethod.Put, "/beregningsgrunnlag/$behandlingId/overstyrt", data)

    override fun lagreAnvendtTrygdetid(
        behandlingId: UUID,
        utrekning: AnvendtTrygdetidPeriodeUtrekning,
    ): Int = medSvar(HttpMethod.Post, "/anvendt-trygdetid/$behandlingId", utrekning, HttpStatusCode.Created)

    override fun harSakInntektForAar(harInntektForAarDto: InntektsjusteringAvkortingInfoRequest): Boolean =
        medSvar(HttpMethod.Post, "/avkorting/inntekt-for-aar", harInntektForAarDto, skriver = false)

    override fun hentAvkorting(behandlingId: UUID): Avkorting? = hentValgfritt("/avkorting/$behandlingId")

    override fun hentFaktiskInntekt(aarsoppgjoerId: UUID): FaktiskInntekt? = hentValgfritt("/avkorting/faktisk-inntekt/$aarsoppgjoerId")

    override fun lagreAvkorting(
        behandlingId: UUID,
        sakId: SakId,
        avkorting: Avkorting,
    ) = utenSvar(HttpMethod.Put, "/avkorting/$behandlingId", LagreAvkortingRequest(sakId, avkorting))

    override fun slettForBehandling(behandling: UUID) = utenSvar(HttpMethod.Delete, "/avkorting/$behandling")

    override fun hentAlleAarsoppgjoer(sakId: SakId): List<AarsoppgjoerLoepende> =
        medSvar(HttpMethod.Get, "/avkorting/sak/${sakId.sakId}/aarsoppgjoer")

    override fun hentAlleBehandlingerMedAarsoppgjoer(sakId: SakId): List<UUID> =
        medSvar(HttpMethod.Get, "/avkorting/sak/${sakId.sakId}/behandlinger")

    override fun hentAlleAarsoppgjoer(behandlinger: List<UUID>): List<AarsoppgjoerLoepende> =
        medSvar(HttpMethod.Post, "/avkorting/aarsoppgjoer", behandlinger, skriver = false)

    override fun lagreEtteroppgjoerResultat(resultat: BeregnetEtteroppgjoerResultat) =
        utenSvar(
            HttpMethod.Put,
            "/etteroppgjoer/resultat",
            resultat,
            referanse = "forbehandlingId=${resultat.forbehandlingId}",
        )

    override fun lagreAvkortingOgEtteroppgjoerResultat(
        sakId: SakId,
        avkorting: Avkorting,
        resultat: BeregnetEtteroppgjoerResultat,
    ) = utenSvar(
        HttpMethod.Put,
        "/etteroppgjoer/${resultat.forbehandlingId}",
        LagreAvkortingOgEtteroppgjoerResultatRequest(sakId, avkorting, resultat),
    )

    override fun hentEtteroppgjoerResultat(
        aar: Int,
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
    ): BeregnetEtteroppgjoerResultat? = hentValgfritt("/etteroppgjoer/resultat/$forbehandlingId/$aar/$sisteIverksatteBehandlingId")

    override fun hentSanksjon(behandlingId: UUID): List<Sanksjon> = medSvar(HttpMethod.Get, "/sanksjon/behandling/$behandlingId")

    override fun opprettSanksjon(
        behandlingId: UUID,
        sakId: SakId,
        saksbehandlerIdent: String,
        sanksjon: LagreSanksjon,
    ) = utenSvar(
        HttpMethod.Post,
        "/sanksjon/behandling/$behandlingId",
        OpprettSanksjonRequest(sakId, saksbehandlerIdent, sanksjon),
        HttpStatusCode.Created,
    )

    override fun opprettSanksjonFraKopi(
        behandlingId: UUID,
        sakId: SakId,
        sanksjon: Sanksjon,
    ) = utenSvar(
        HttpMethod.Post,
        "/sanksjon/behandling/$behandlingId/kopi",
        OpprettSanksjonFraKopiRequest(sakId, sanksjon),
        HttpStatusCode.Created,
    )

    override fun oppdaterSanksjon(
        sanksjon: LagreSanksjon,
        saksbehandlerIdent: String,
    ) = utenSvar(
        HttpMethod.Put,
        "/sanksjon",
        OppdaterSanksjonRequest(saksbehandlerIdent, sanksjon),
        referanse = "sanksjonId=${sanksjon.id}",
    )

    override fun slettSanksjon(sanksjonId: UUID): Int = medSvar(HttpMethod.Delete, "/sanksjon/$sanksjonId")

    override fun hentSanksjonMedId(sanksjonId: UUID): Sanksjon? = hentValgfritt("/sanksjon/$sanksjonId")

    private inline fun <reified T : Any> hentValgfritt(sti: String): T? =
        runBlocking {
            kall(HttpMethod.Get, sti, statuser = setOf(HttpStatusCode.OK, HttpStatusCode.NotFound)) { response ->
                if (response.status == HttpStatusCode.NotFound) null else response.body<T>()
            }
        }

    private inline fun <reified T : Any> medSvar(
        metode: HttpMethod,
        sti: String,
        data: Any? = null,
        status: HttpStatusCode = HttpStatusCode.OK,
        skriver: Boolean = metode != HttpMethod.Get,
        referanse: String? = null,
    ): T = runBlocking { kall(metode, sti, data, setOf(status), skriver, referanse) { it.body<T>() } }

    private fun utenSvar(
        metode: HttpMethod,
        sti: String,
        data: Any? = null,
        status: HttpStatusCode = HttpStatusCode.NoContent,
        referanse: String? = null,
    ) {
        runBlocking { kall(metode, sti, data, setOf(status), referanse = referanse) {} }
    }

    private suspend fun <T> kall(
        metode: HttpMethod,
        sti: String,
        data: Any? = null,
        statuser: Set<HttpStatusCode>,
        skriver: Boolean = metode != HttpMethod.Get,
        referanse: String? = null,
        lesSvar: suspend (HttpResponse) -> T,
    ): T =
        try {
            val response =
                httpClient.request("$url$sti") {
                    method = metode
                    expectSuccess = false
                    if (data != null) {
                        contentType(ContentType.Application.Json)
                        setBody(data)
                    }
                }
            if (response.status !in statuser) {
                if (skriver && (response.status.value >= 500 || response.status.value in 200..299)) {
                    loggUavklartSkriveutfall(metode, sti, referanse, "HTTP ${response.status.value}")
                }
                throw InternfeilException("Beregning CRUD svarte med ${response.status} på ${metode.value} $sti")
            }
            lesSvar(response)
        } catch (feil: IOException) {
            if (skriver) loggUavklartSkriveutfall(metode, sti, referanse, feil.javaClass.simpleName)
            throw feil
        } catch (feil: ContentConvertException) {
            if (skriver) loggUavklartSkriveutfall(metode, sti, referanse, feil.javaClass.simpleName)
            throw feil
        } catch (feil: NoTransformationFoundException) {
            if (skriver) loggUavklartSkriveutfall(metode, sti, referanse, feil.javaClass.simpleName)
            throw feil
        }

    private fun loggUavklartSkriveutfall(
        metode: HttpMethod,
        sti: String,
        referanse: String?,
        feiltype: String,
    ) {
        logger.warn(
            "Uavklart utfall av Beregning CRUD {} {}{}. Feiltype={}. " +
                "Skrivingen kan være gjennomført selv om behandling ruller tilbake.",
            metode.value,
            sti,
            referanse?.let { " ($it)" } ?: "",
            feiltype,
        )
    }
}

private data class LagreAvkortingRequest(
    val sakId: SakId,
    val avkorting: Avkorting,
)

private data class LagreAvkortingOgEtteroppgjoerResultatRequest(
    val sakId: SakId,
    val avkorting: Avkorting,
    val resultat: BeregnetEtteroppgjoerResultat,
)

private data class OpprettSanksjonRequest(
    val sakId: SakId,
    val saksbehandlerIdent: String,
    val sanksjon: LagreSanksjon,
)

private data class OpprettSanksjonFraKopiRequest(
    val sakId: SakId,
    val sanksjon: Sanksjon,
)

private data class OppdaterSanksjonRequest(
    val saksbehandlerIdent: String,
    val sanksjon: LagreSanksjon,
)
