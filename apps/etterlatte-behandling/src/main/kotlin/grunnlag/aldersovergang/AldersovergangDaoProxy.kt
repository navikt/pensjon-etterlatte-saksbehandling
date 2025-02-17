package no.nav.etterlatte.grunnlag.aldersovergang

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotliquery.TransactionalSession
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

@Deprecated("Dette kan fjernes så fort grunnlag er migrert over til behandling")
class AldersovergangDaoProxy(
    config: Config,
    httpClient: HttpClient,
) : IAldersovergangDao {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val url = config.getString("grunnlag.resource.url") + "/api/grunnlag/dao"

    override fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession?,
    ): List<String> {
        logger.info("Kaller hentSoekereFoedtIEnGittMaaned")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/hentSoekereFoedtIEnGittMaaned?maaned=$maaned"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun hentSakerHvorDoedsfallForekomIGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession?,
    ): List<String> {
        logger.info("Kaller hentSakerHvorDoedsfallForekomIGittMaaned")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/hentSakerHvorDoedsfallForekomIGittMaaned?maaned=$maaned"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun hentFoedselsdato(
        sakId: SakId,
        opplysningType: Opplysningstype,
        tx: TransactionalSession?,
    ): LocalDate? {
        logger.info("Kaller hentFoedselsdato")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/hentFoedselsdato?sakId=$sakId&opplysningType=$opplysningType"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }
}
