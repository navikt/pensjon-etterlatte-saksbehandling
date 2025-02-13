package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import org.slf4j.LoggerFactory
import java.util.UUID

@Deprecated("Dette kan fjernes så fort grunnlag er migrert over til behandling")
class OpplysningDaoProxy(
    config: Config,
    httpClient: HttpClient,
) : IOpplysningDao {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val url = config.getString("grunnlag.resource.url") + "/api/grunnlag/dao"

    override fun finnesGrunnlagForSak(sakId: SakId): Boolean {
        logger.info("Kaller finnesGrunnlagForSak")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/finnesGrunnlagForSak?sakId=$sakId"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun hentAlleGrunnlagForSak(sakId: SakId): List<OpplysningDao.GrunnlagHendelse> {
        logger.info("Kaller hentAlleGrunnlagForSak")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/hentAlleGrunnlagForSak?sakId=$sakId"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun hentAlleGrunnlagForBehandling(behandlingId: UUID): List<OpplysningDao.GrunnlagHendelse> {
        logger.info("Kaller hentAlleGrunnlagForBehandling")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/hentAlleGrunnlagForBehandling?behandlingId=$behandlingId"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun hentGrunnlagAvTypeForBehandling(
        behandlingId: UUID,
        vararg typer: Opplysningstype,
    ): List<OpplysningDao.GrunnlagHendelse> {
        logger.info("Kaller hentGrunnlagAvTypeForBehandling")

        val typeliste = typer.joinToString("&") { "type=$it" }

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$url/hentGrunnlagAvTypeForBehandling?behandlingId=$behandlingId&$typeliste",
                    ),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun finnHendelserIGrunnlag(sakId: SakId): List<OpplysningDao.GrunnlagHendelse> {
        logger.info("Kaller finnHendelserIGrunnlag")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/finnHendelserIGrunnlag?sakId=$sakId"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun finnNyesteGrunnlagForSak(
        sakId: SakId,
        opplysningType: Opplysningstype,
    ): OpplysningDao.GrunnlagHendelse? {
        logger.info("Kaller finnNyesteGrunnlagForSak")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/finnNyesteGrunnlagForSak?sakId=$sakId&opplysningstype=$opplysningType"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun finnNyesteGrunnlagForBehandling(
        behandlingId: UUID,
        opplysningType: Opplysningstype,
    ): OpplysningDao.GrunnlagHendelse? {
        logger.info("Kaller finnNyesteGrunnlagForBehandling")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/finnNyesteGrunnlagForBehandling?behandlingId=$behandlingId&opplysningstype=$opplysningType"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun leggOpplysningTilGrunnlag(
        sakId: SakId,
        behandlingsopplysning: Grunnlagsopplysning<JsonNode>,
        fnr: Folkeregisteridentifikator?,
    ): Long {
        logger.info("Kaller leggOpplysningTilGrunnlag")

        return runBlocking {
            downstreamResourceClient
                .post(
                    Resource(clientId, "$url/leggOpplysningTilGrunnlag?sakId=$sakId"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                    postBody = NyOpplysningRequest(fnr, behandlingsopplysning),
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun oppdaterVersjonForBehandling(
        behandlingId: UUID,
        sakId: SakId,
        hendelsenummer: Long,
    ): Int {
        logger.info("Kaller oppdaterVersjonForBehandling")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$url/oppdaterVersjonForBehandling?sakId=$sakId&behandlingId=$behandlingId&hendelsesnummer=$hendelsenummer",
                    ),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun laasGrunnlagVersjonForBehandling(behandlingId: UUID): Int {
        logger.info("Kaller laasGrunnlagVersjonForBehandling")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/laasGrunnlagVersjonForBehandling?behandlingId=$behandlingId"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun finnAllePersongalleriHvorPersonFinnes(fnr: Folkeregisteridentifikator): List<OpplysningDao.GrunnlagHendelse> {
        logger.info("Kaller finnAllePersongalleriHvorPersonFinnes")

        return runBlocking {
            downstreamResourceClient
                .post(
                    Resource(clientId, "$url/finnAllePersongalleriHvorPersonFinnes"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                    postBody = FoedselsnummerDTO(fnr.value),
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun finnAlleSakerForPerson(fnr: Folkeregisteridentifikator): Set<SakId> {
        logger.info("Kaller finnAlleSakerForPerson")

        return runBlocking {
            downstreamResourceClient
                .post(
                    Resource(clientId, "$url/finnAlleSakerForPerson"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                    postBody = FoedselsnummerDTO(fnr.value),
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }

    override fun hentBehandlingVersjon(behandlingId: UUID): BehandlingGrunnlagVersjon? {
        logger.info("Kaller hentBehandlingVersjon")

        return runBlocking {
            downstreamResourceClient
                .get(
                    Resource(clientId, "$url/hentBehandlingVersjon?behandlingId=$behandlingId"),
                    brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }
    }
}

// Kan skrotes så fort data er migrert
data class NyOpplysningRequest(
    val fnr: Folkeregisteridentifikator? = null,
    val opplysning: Grunnlagsopplysning<JsonNode>,
)
