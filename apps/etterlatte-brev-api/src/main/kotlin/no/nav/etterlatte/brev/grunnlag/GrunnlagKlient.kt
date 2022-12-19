package no.nav.etterlatte.brev.grunnlag

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

class GrunnlagKlient(config: Config, httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val baseUrl = config.getString("grunnlag.resource.url")

    suspend fun hentGrunnlag(sakid: Long, accessToken: String): Grunnlag =
        hentGrunnlagJson(sakid, null, accessToken)
            .let { deserialize(it.toString()) }

    suspend fun hentInnsender(sakid: Long, accessToken: String): Grunnlagsopplysning<InnsenderSoeknad> =
        hentGrunnlagJson(sakid, Opplysningstype.INNSENDER_SOEKNAD_V1, accessToken)
            .let { deserialize(it.toString()) }

    suspend fun hentSpraak(sakId: Long, accessToken: String): Grunnlagsopplysning<Spraak> =
        hentGrunnlagJson(sakId, Opplysningstype.SPRAAK, accessToken)
            .let { deserialize(it.toString()) }

    private suspend fun hentGrunnlagJson(sakid: Long, type: Opplysningstype?, accessToken: String): Any? {
        val typeParam = if (type == null) "" else "/$type"

        return downstreamResourceClient.get(
            Resource(clientId, "$baseUrl/api/grunnlag/$sakid$typeParam"),
            accessToken
        ).mapBoth(
            success = { json -> json },
            failure = { errorMessage ->
                logger.error("Henting av grunnlag (type=$type) for sakid=$sakid feilet", errorMessage.throwable)
                null
            }
        )?.response
    }
}
