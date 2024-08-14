package no.nav.etterlatte.grunnlagsendring.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import org.slf4j.LoggerFactory
import java.util.UUID

interface GrunnlagKlient : Pingable {
    suspend fun hentGrunnlag(sakId: Long): Grunnlag?

    suspend fun hentAlleSakIder(fnr: String): Set<Long>

    suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller

    suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        request: OppdaterGrunnlagRequest,
    )

    suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>?

    suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
    )

    suspend fun lagreNyeSaksopplysningerBareSak(
        sakId: Long,
        saksopplysninger: NyeSaksopplysninger,
    )

    suspend fun leggInnNyttGrunnlagSak(
        sakId: Long,
        opplysningsbehov: Opplysningsbehov,
    )

    suspend fun laasTilGrunnlagIBehandling(
        id: UUID,
        forrigeBehandling: UUID,
    )
}

class GrunnlagKlientImpl(
    config: Config,
    private val client: HttpClient,
) : GrunnlagKlient {
    private val url = config.getString("grunnlag.resource.url")
    private val apiUrl = url.plus("/api")
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    ) {
        client
            .post("$apiUrl/grunnlag/behandling/$behandlingId/opprett-grunnlag") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(opplysningsbehov)
            }
    }

    override suspend fun leggInnNyttGrunnlagSak(
        sakId: Long,
        opplysningsbehov: Opplysningsbehov,
    ) {
        client
            .post("$apiUrl/grunnlag/sak/$sakId/opprett-grunnlag") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(opplysningsbehov)
            }
    }

    override suspend fun laasTilGrunnlagIBehandling(
        id: UUID,
        forrigeBehandling: UUID,
    ) {
        client.post("$apiUrl/grunnlag/behandling/$id/laas-til-behandling/$forrigeBehandling")
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        request: OppdaterGrunnlagRequest,
    ) {
        client
            .post("$apiUrl/grunnlag/behandling/$behandlingId/oppdater-grunnlag") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
    }

    override suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
    ) {
        client
            .post("$apiUrl/grunnlag/behandling/$behandlingId/nye-opplysninger") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(saksopplysninger)
            }
    }

    override suspend fun lagreNyeSaksopplysningerBareSak(
        sakId: Long,
        saksopplysninger: NyeSaksopplysninger,
    ) {
        client
            .post("$apiUrl/grunnlag/sak/$sakId/nye-opplysninger") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(saksopplysninger)
            }
    }

    /**
     * Henter komplett grunnlag for sak
     **/
    override suspend fun hentGrunnlag(sakId: Long): Grunnlag? =
        client
            .get("$apiUrl/grunnlag/sak/$sakId") {
                accept(ContentType.Application.Json)
            }.body()

    override suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>? =
        client
            .get("$apiUrl/grunnlag/behandling/$behandlingId/${Opplysningstype.PERSONGALLERI_V1}") {
                accept(ContentType.Application.Json)
            }.body()

    override suspend fun hentAlleSakIder(fnr: String): Set<Long> =
        client
            .post("$apiUrl/grunnlag/person/saker") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.body()

    override suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller =
        client
            .post("$apiUrl/grunnlag/person/roller") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.body()

    override val serviceName: String
        get() = "Grunnlagklient"
    override val beskrivelse: String
        get() = "Henter lagret grunnlag for sak eller behandling"
    override val endpoint: String
        get() = this.url

    override suspend fun ping(konsument: String?): PingResult =
        client.ping(
            pingUrl = url.plus("/isready"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )
}
