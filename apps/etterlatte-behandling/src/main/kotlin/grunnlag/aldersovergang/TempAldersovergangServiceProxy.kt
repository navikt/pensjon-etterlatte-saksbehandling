package no.nav.etterlatte.grunnlag.aldersovergang

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class TempAldersovergangServiceProxy(
    config: Config,
    httpClient: HttpClient,
) : IAldersovergangService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    private val url = "$resourceUrl/api"

    override suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): YearMonth? {
        logger.info("Sjekker om det allerede finnes grunnlag på sak=$sakId")

        return downstreamResourceClient
            .get(
                Resource(clientId, "$url/grunnlag/aldersovergang/sak/$sakId/$sakType"),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { deserialize(it.response!!.toString()) },
                failure = { throw it },
            )
    }

    override suspend fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<String> {
        logger.info("Henter søkere født i måned $maaned")

        return downstreamResourceClient
            .get(
                Resource(clientId, "$url/grunnlag/aldersovergang/$maaned"),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { deserialize(it.response!!.toString()) },
                failure = { throw it },
            )
    }

    override suspend fun hentSakerHvorDoedsfallForekomIGittMaaned(
        behandlingsmaaned: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<String> {
        logger.info("Henter saker hvor dødsfall forekom i gitt måned $behandlingsmaaned")

        return downstreamResourceClient
            .get(
                Resource(clientId, "$url/grunnlag/doedsdato/$behandlingsmaaned"),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { deserialize(it.response!!.toString()) },
                failure = { throw it },
            )
    }

    override fun hentAlder(
        sakId: SakId,
        rolle: PersonRolle,
        paaDato: LocalDate,
    ): Alder? {
        TODO("Ikke implementert siden det ikke brukes av route")
    }
}
