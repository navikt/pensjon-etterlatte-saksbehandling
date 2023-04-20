package no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.innsendtsoeknad.omstillingsstoenad.Omstillingsstoenad
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class InnsendtSoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingClient: BehandlingClient
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(InnsendtSoeknadRiver::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(SoeknadInnsendt.eventNameInnsendt)
            correlationId()
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoKey) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoTypeKey, SoeknadType.OMSTILLINGSSTOENAD.name) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoVersjonKey, "1") }
            validate { it.requireKey(SoeknadInnsendt.lagretSoeknadIdKey) }
            validate { it.requireKey(SoeknadInnsendt.hendelseGyldigTilKey) }
            validate { it.requireKey(SoeknadInnsendt.adressebeskyttelseKey) }
            validate { it.requireKey(SoeknadInnsendt.fnrSoekerKey) }
            validate { it.rejectKey(SoeknadInnsendt.dokarkivReturKey) }
            validate { it.rejectKey(GyldigSoeknadVurdert.behandlingIdKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            try {
                val soeknad = packet.soeknad()

                // Persongalleri skal utgå
                val personGalleri = Persongalleri(
                    soeker = soeknad.soeker.foedselsnummer.svar.value,
                    innsender = soeknad.innsender.foedselsnummer.svar.value,
                    avdoed = listOf(soeknad.avdoed.foedselsnummer.svar.value),
                    soesken = soeknad.barn.map { it.foedselsnummer.svar.value }
                )

                // Skal vurderes manuelt av saksbehandler
                val gyldighetsVurdering = GyldighetsResultat(
                    KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
                    listOf(),
                    Tidspunkt.now().toLocalDatetimeUTC()
                )

                val sak = behandlingClient.skaffSak(personGalleri.soeker, SakType.OMSTILLINGSSTOENAD.name)
                val behandlingId = behandlingClient.initierBehandling(sak.id, soeknad.mottattDato, personGalleri)
                behandlingClient.lagreGyldighetsVurdering(behandlingId, gyldighetsVurdering)
                logger.info("Behandling {} startet på sak {}", behandlingId, sak.id)

                sendOpplysningsbehov(sak, personGalleri, context, packet)

                context.publish(
                    packet.apply {
                        set(GyldigSoeknadVurdert.sakIdKey, sak.id)
                        set(GyldigSoeknadVurdert.behandlingIdKey, behandlingId)
                    }.toJson()
                )
                logger.info("Vurdert gyldighet av søknad om omstillingsstønad er fullført")
            } catch (e: Exception) {
                logger.error("Gyldighetsvurdering av søknad om omstillingsstønad feilet", e)
            }
        }
    }

    private fun sendOpplysningsbehov(
        sak: Sak,
        persongalleri: Persongalleri,
        context: MessageContext,
        packet: JsonMessage
    ) {
        context.publish(
            JsonMessage.newMessage(
                mapOf(
                    BEHOV_NAME_KEY to Opplysningstype.SOEKER_PDL_V1,
                    "sakId" to sak.id,
                    "sakType" to sak.sakType,
                    "fnr" to persongalleri.soeker,
                    "rolle" to PersonRolle.GJENLEVENDE,
                    CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                )
            ).toJson()
        )

        persongalleri.avdoed.forEach { fnr ->
            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.AVDOED_PDL_V1,
                        "sakId" to sak.id,
                        "sakType" to sak.sakType,
                        "fnr" to fnr,
                        "rolle" to PersonRolle.AVDOED,
                        CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                    )
                ).toJson()
            )
        }
    }

    private fun JsonMessage.soeknad() = this[FordelerFordelt.skjemaInfoKey].let {
        objectMapper.treeToValue<Omstillingsstoenad>(
            it
        )
    }
}