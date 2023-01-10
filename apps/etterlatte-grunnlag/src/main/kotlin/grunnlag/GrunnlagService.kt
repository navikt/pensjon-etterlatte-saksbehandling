package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

interface GrunnlagService {
    fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>?
    fun lagreNyeSaksopplysninger(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun lagreNyePersonopplysninger(sak: Long, fnr: Foedselsnummer, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun hentOpplysningsgrunnlag(sak: Long): Grunnlag?
    fun hentOpplysningsgrunnlagMedVersjon(sak: Long, versjon: Long): Grunnlag?
    fun hentOpplysningsgrunnlag(
        sak: Long,
        persongalleri: Persongalleri
    ): Grunnlag // TODO ai: Kan fjernes når kafka flyten fjernes

    suspend fun lagreSoeskenMedIBeregning(
        behandlingId: UUID,
        soeskenMedIBeregning: List<SoeskenMedIBeregning>,
        saksbehandlerId: String,
        accessToken: String
    )
}

class RealGrunnlagService(
    private val opplysningDao: OpplysningDao,
    private val sendToRapid: (String, UUID) -> Unit,
    private val behandlingKlient: BehandlingKlient
) : GrunnlagService {

    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentOpplysningsgrunnlag(sak: Long): Grunnlag? {
        val persongalleriJsonNode = opplysningDao.finnNyesteGrunnlag(sak, Opplysningstype.PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak $sak. Fant ikke persongalleri")
            return null
        }
        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sak)

        return OpplysningsgrunnlagMapper(grunnlag, sak, persongalleri).hentGrunnlag()
    }

    override fun hentOpplysningsgrunnlag(sak: Long, persongalleri: Persongalleri): Grunnlag {
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sak)

        return OpplysningsgrunnlagMapper(grunnlag, sak, persongalleri).hentGrunnlag()
    }

    override fun hentOpplysningsgrunnlagMedVersjon(sak: Long, versjon: Long): Grunnlag? {
        val grunnlag = opplysningDao.finnGrunnlagOpptilVersjon(sak, versjon)

        val personGalleri = grunnlag.find { it.opplysning.opplysningType === Opplysningstype.PERSONGALLERI_V1 }
            ?.let { objectMapper.readValue(it.opplysning.opplysning.toJson(), Persongalleri::class.java) }
            ?: run {
                logger.info(
                    "Klarte ikke å hente ut grunnlag for sak $sak med versjon $versjon. " +
                        "Fant ikke persongalleri"
                )
                return null
            }

        return OpplysningsgrunnlagMapper(grunnlag, sak, personGalleri).hentGrunnlag()
    }

    override suspend fun lagreSoeskenMedIBeregning(
        behandlingId: UUID,
        soeskenMedIBeregning: List<SoeskenMedIBeregning>,
        saksbehandlerId: String,
        accessToken: String
    ) {
        val opplysning: List<Grunnlagsopplysning<JsonNode>> = listOf(
            lagOpplysning(
                opplysningsType = Opplysningstype.SOESKEN_I_BEREGNINGEN,
                kilde = Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
                opplysning = Beregningsgrunnlag(soeskenMedIBeregning).toJsonNode()
            )
        )

        val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)

        val sakId = behandling.sak
        lagreNyeSaksopplysninger(sakId, opplysning)
        val grunnlagEndretMessage = JsonMessage.newMessage(
            eventName = "GRUNNLAG:GRUNNLAGENDRET",
            map = mapOf(correlationIdKey to getCorrelationId(), "sakId" to sakId)
        )
        sendToRapid(grunnlagEndretMessage.toJson(), behandlingId)
        logger.info("Lagt ut melding om grunnlagsendring for behandling $behandlingId")
    }

    fun <T> lagOpplysning(
        opplysningsType: Opplysningstype,
        kilde: Grunnlagsopplysning.Kilde,
        opplysning: T,
        fnr: Foedselsnummer? = null,
        periode: Periode? = null
    ): Grunnlagsopplysning<T> {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = kilde,
            opplysningType = opplysningsType,
            meta = objectMapper.createObjectNode(),
            opplysning = opplysning,
            fnr = fnr,
            periode = periode
        )
    }

    override fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>? {
        return opplysningDao.finnNyesteGrunnlag(sak, opplysningstype)?.opplysning
    }

    override fun lagreNyePersonopplysninger(
        sak: Long,
        fnr: Foedselsnummer,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag")
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
            } else {
                opplysningDao.leggOpplysningTilGrunnlag(sak, opplysning, fnr)
            }
        }
    }

    override fun lagreNyeSaksopplysninger(
        sak: Long,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag")
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
            } else {
                opplysningDao.leggOpplysningTilGrunnlag(sak, opplysning, null)
            }
        }
    }
}