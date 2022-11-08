package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.sikkerLogg
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.YearMonth

class GrunnlagEndretRiver(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(GrunnlagEndretRiver::class.java)

    init {
        // Kopi av river for eksisterende app
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            validate { it.requireKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey) }
            validate { it.requireKey("behandlingOpprettet") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("behandling.type") }
            validate { it.requireKey("sak.sakType") }
            validate { it.requireKey("fnrSoeker") }
            validate { it.requireKey("virkningstidspunkt") }
            validate { it.interestedIn(BehandlingGrunnlagEndret.revurderingAarsakKey) }
            validate { it.interestedIn(BehandlingGrunnlagEndret.manueltOpphoerAarsakKey) }
            validate { it.interestedIn(BehandlingGrunnlagEndret.manueltOpphoerfritekstAarsakKey) }
            validate { it.rejectKey("vilkaarsvurdering") }
            validate { it.rejectKey("gyldighetsvurdering") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                // todo: Denne flyten skal erstattes med http kall for å hente inn data.
                val grunnlagEndretPayload = objectMapper.readTree(packet.toJson())
                val behandlingId = packet["behandlingId"].asText().toUUID()
                val behandlingType = BehandlingType.valueOf(packet["behandling.type"].asText())
                val sakType = SakType.valueOf(packet["sak.sakType"].asText())
                val virkningstidspunkt = packet["virkningstidspunkt"].asYearMonth().atDay(1)
                    // TODO fjern dette når det blir hentet direkte fra behandling
                    .let {
                        Virkningstidspunkt(
                            dato = YearMonth.of(it.year, it.month),
                            kilde = Grunnlagsopplysning.Saksbehandler("todo", Instant.now())
                        )
                    }
                val grunnlag =
                    requireNotNull(
                        objectMapper.treeToValue<Grunnlag>(
                            packet[BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey]
                        )
                    )
                val revurderingAarsak: RevurderingAarsak? = kotlin.runCatching {
                    RevurderingAarsak.valueOf(packet[BehandlingGrunnlagEndret.revurderingAarsakKey].asText())
                }.getOrNull()

                // todo: Midlertidig fiks for å unngå at behandling forsøkes å opprettes
                // Vil kaste NPE dersom grunnlag ikke eksisterer for avdød.
                grunnlag.hentAvdoed()

                // Må få kopiert over alle disse tingene med virkningsdato osv
                val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)

                // Inntil videre oppretter / oppdateres vilkårsvurdering med nyeste payload fra grunnlag for å
                // kunne sende dette videre sendere.
                if (vilkaarsvurdering != null) {
                    logger.info("Oppdaterer eksisterende vilkårsvurdering for behandlingId=$behandlingId")
                    vilkaarsvurderingService.oppdaterVilkaarsvurderingPayload(
                        behandlingId = behandlingId,
                        payload = grunnlagEndretPayload,
                        virkningstidspunkt = virkningstidspunkt
                    )
                } else {
                    logger.info("Oppretter ny vilkårsvurdering for behandlingId=$behandlingId")
                    vilkaarsvurderingService.opprettVilkaarsvurdering(
                        behandlingId = behandlingId,
                        sakType = sakType,
                        behandlingType = behandlingType,
                        virkningstidspunkt = virkningstidspunkt,
                        payload = grunnlagEndretPayload,
                        grunnlag = grunnlag,
                        revurderingAarsak = revurderingAarsak
                    )
                }
            } catch (e: JsonMappingException) {
                sikkerLogg.error("Feilet under deserialisering ved opprettelse/oppdatering av vilkårsvurdering", e)
                logger.error(
                    "Feilet under deserialisering ved opprettelse/oppdatering av vilkårsvurdering. " +
                        "Sjekk sikkerlogg for detaljert feilmelding"
                )
            } catch (e: Exception) {
                logger.error("Feilet ved opprettelse/oppdatering av vilkårsvurdering", e)
            }
        }
}