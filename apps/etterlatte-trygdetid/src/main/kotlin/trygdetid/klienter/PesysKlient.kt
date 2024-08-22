package no.nav.etterlatte.trygdetid.klienter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.Date

interface PesysKlient {
    suspend fun hentTrygdetidsgrunnlag(fnr: String, brukerTokenInfo: BrukerTokenInfo): TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon
}

data class TrygdetidsgrunnlagRequest(val fnr: String, val dato: LocalDate)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trygdetidsgrunnlag(
    val land: String? = null, //ISO 3166-1 alpha-3 code
    val fomDato: Date? = null,
    val tomDato: Date? = null,
    val poengIInnAr: Boolean? = null,
    val poengIUtAr: Boolean? = null,
    val ikkeProRata: Boolean? = null
)

data class TrygdetidsgrunnlagListe(
    val trygdetidsgrunnlagListe: List<Trygdetidsgrunnlag>,
    val trygdetidsgrunnlagKap20Liste: List<Trygdetidsgrunnlag>
)

data class SakIdTrygdetidsgrunnlagListePairResponse(val sakId: Long, val trygdetidsgrunnlag: TrygdetidsgrunnlagListe)

data class TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon(
    val trygdetidUfoeretrygdpensjon: SakIdTrygdetidsgrunnlagListePairResponse?,
    val trygdetidAlderspensjon: SakIdTrygdetidsgrunnlagListePairResponse?
)

class PesysKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : PesysKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("pen.client.id")
    private val resourceUrl = config.getString("pen.client.url")

    override suspend fun hentTrygdetidsgrunnlag(fnr: String, brukerTokenInfo: BrukerTokenInfo): TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon {
        logger.info("Henter trygdetidsgrunnlag(uføre og AP) for  ${fnr.maskerFnr()} fra PEN")

        val (trygdetidUfoerepensjon, trygdetidAlderspensjon) = coroutineScope {
            val ufoereTrygd = async { hentTrygdetidsgrunnlagListeForLopendeUforetrygd(fnr, brukerTokenInfo) }
            val alderspensjon = async { hentTrygdetidslisteForLoependeAlderspensjon(fnr, brukerTokenInfo) }

            awaitAll(ufoereTrygd, alderspensjon)
        }
        return TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon(trygdetidUfoerepensjon, trygdetidAlderspensjon)
    }

    private suspend fun hentTrygdetidsgrunnlagListeForLopendeUforetrygd(fnr: String, brukerTokenInfo: BrukerTokenInfo): SakIdTrygdetidsgrunnlagListePairResponse? {
        return downstreamResourceClient.post(
            resource = Resource(
                clientId = clientId,
                url = "$resourceUrl/api/uforetrygd/grunnlag/trygdetidsgrunnlagListeForLopendeUforetrygd",
            ),
            brukerTokenInfo = brukerTokenInfo,
            postBody = TrygdetidsgrunnlagRequest(fnr, LocalDate.now())
        ).mapBoth(
            success = { resource -> objectMapper.readValue<SakIdTrygdetidsgrunnlagListePairResponse?>(resource.response.toString()) },
            failure = { errorResponse -> throw errorResponse },
        )
    }

    private suspend fun hentTrygdetidslisteForLoependeAlderspensjon(fnr: String, brukerTokenInfo: BrukerTokenInfo): SakIdTrygdetidsgrunnlagListePairResponse? {
        return downstreamResourceClient.post(
            resource = Resource(
                clientId = clientId,
                url = "$resourceUrl/api/alderspensjon/grunnlag/trygdetidsgrunnlagListeForLopendeAlderspensjon"
            ),
            brukerTokenInfo = brukerTokenInfo,
            postBody = TrygdetidsgrunnlagRequest(fnr, LocalDate.now())
        ).mapBoth(
            success = { resource -> objectMapper.readValue<SakIdTrygdetidsgrunnlagListePairResponse?>(resource.response.toString()) },
            failure = { errorResponse -> throw errorResponse },
        )
    }
}