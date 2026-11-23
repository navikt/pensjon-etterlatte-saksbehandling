package no.nav.etterlatte.modules.beregning.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
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
    private val url = "${resourceUrl.trimEnd('/')}/api/beregning/intern"

    override fun hent(behandlingId: UUID): Beregning? = hentValgfritt("/beregning/$behandlingId")

    override fun lagreEllerOppdaterBeregning(beregning: Beregning): Beregning = medSvar(HttpMethod.Put, "/beregning", beregning)

    override fun slettOverstyrtBeregningsgrunnlag(behandlingId: UUID) =
        utenSvar(HttpMethod.Delete, "/beregning/$behandlingId/overstyrt-grunnlag")

    override fun hentOverstyrBeregning(sakId: SakId): OverstyrBeregning? = hentValgfritt("/beregning/overstyring/${sakId.sakId}")

    override fun opprettOverstyrBeregning(overstyrBeregning: OverstyrBeregning): OverstyrBeregning? =
        runBlocking {
            val response =
                kall(
                    HttpMethod.Put,
                    "/beregning/overstyring",
                    overstyrBeregning,
                    setOf(HttpStatusCode.OK, HttpStatusCode.NoContent),
                )
            if (response.status == HttpStatusCode.NoContent) null else response.body<OverstyrBeregning>()
        }

    override fun deaktiverOverstyrtBeregning(sakId: SakId) = utenSvar(HttpMethod.Delete, "/beregning/overstyring/${sakId.sakId}")

    override fun finnBeregningsGrunnlag(id: UUID): BeregningsGrunnlag? = hentValgfritt("/beregningsgrunnlag/$id")

    override fun lagreBeregningsGrunnlag(beregningsGrunnlag: BeregningsGrunnlag): Boolean =
        medSvar(HttpMethod.Put, "/beregningsgrunnlag", beregningsGrunnlag)

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
        medSvar(HttpMethod.Post, "/avkorting/inntekt-for-aar", harInntektForAarDto)

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
        medSvar(HttpMethod.Post, "/avkorting/aarsoppgjoer", behandlinger)

    override fun lagreEtteroppgjoerResultat(resultat: BeregnetEtteroppgjoerResultat) =
        utenSvar(HttpMethod.Put, "/etteroppgjoer/resultat", resultat)

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
    ) = utenSvar(HttpMethod.Put, "/sanksjon", OppdaterSanksjonRequest(saksbehandlerIdent, sanksjon))

    override fun slettSanksjon(sanksjonId: UUID): Int = medSvar(HttpMethod.Delete, "/sanksjon/$sanksjonId")

    override fun hentSanksjonMedId(sanksjonId: UUID): Sanksjon? = hentValgfritt("/sanksjon/$sanksjonId")

    private inline fun <reified T : Any> hentValgfritt(sti: String): T? =
        runBlocking {
            val response = kall(HttpMethod.Get, sti, statuser = setOf(HttpStatusCode.OK, HttpStatusCode.NotFound))
            if (response.status == HttpStatusCode.NotFound) null else response.body<T>()
        }

    private inline fun <reified T : Any> medSvar(
        metode: HttpMethod,
        sti: String,
        data: Any? = null,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): T = runBlocking { kall(metode, sti, data, setOf(status)).body<T>() }

    private fun utenSvar(
        metode: HttpMethod,
        sti: String,
        data: Any? = null,
        status: HttpStatusCode = HttpStatusCode.NoContent,
    ) {
        runBlocking { kall(metode, sti, data, setOf(status)) }
    }

    private suspend fun kall(
        metode: HttpMethod,
        sti: String,
        data: Any? = null,
        statuser: Set<HttpStatusCode>,
    ): HttpResponse {
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
            throw InternfeilException("Beregning CRUD svarte med ${response.status} på ${metode.value} $sti")
        }
        return response
    }
}

private data class LagreAvkortingRequest(
    val sakId: SakId,
    val avkorting: Avkorting,
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
