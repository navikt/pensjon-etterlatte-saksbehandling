package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface GrunnlagKlient {
    suspend fun finnPersonOpplysning(
        behandlingId: UUID,
        opplysningsType: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Person>?

    suspend fun hentPersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Persongalleri>?
}

class GrunnlagKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class GrunnlagKlientObo(config: Config, httpClient: HttpClient) : GrunnlagKlient {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    override suspend fun finnPersonOpplysning(
        behandlingId: UUID,
        opplysningsType: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Person>? {
        try {
            logger.info("Henter opplysning ($opplysningsType) fra grunnlag for behandling (id=$behandlingId)")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/grunnlag/behandling/$behandlingId/$opplysningsType",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )
                .mapBoth(
                    success = { resource -> resource.response?.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw GrunnlagKlientException(
                "Henting av opplysning ($opplysningsType) fra grunnlag for behandling (id=$behandlingId) feilet",
                e,
            )
        }
    }

    override suspend fun hentPersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Persongalleri>? {
        try {
            logger.info("Henter persongalleri fra grunnlag for behandling (id=$behandlingId)")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/grunnlag/behandling/$behandlingId/${Opplysningstype.PERSONGALLERI_V1}",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )
                .mapBoth(
                    success = { resource -> resource.response?.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw GrunnlagKlientException("Henting av persongalleri fra grunnlag for behandling (id=$behandlingId) feilet", e)
        }
    }
}
