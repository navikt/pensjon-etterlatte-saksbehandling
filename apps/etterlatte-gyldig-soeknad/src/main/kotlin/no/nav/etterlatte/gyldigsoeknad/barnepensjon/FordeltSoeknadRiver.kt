package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

internal class FordeltSoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val gyldigSoeknadService: GyldigSoeknadService,
    private val behandlingClient: BehandlingClient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(FordeltSoeknadRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV) {
            validate { it.requireKey(FordelerFordelt.skjemaInfoKey) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoTypeKey, SoeknadType.BARNEPENSJON.name) }
            validate { it.rejectKey(GyldigSoeknadVurdert.behandlingIdKey) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Starter gyldighetsvurdering av mottatt søknad om barnepensjon")

        try {
            val soeknad = packet.soeknad()
            val personGalleri = gyldigSoeknadService.hentPersongalleriFraSoeknad(soeknad)
            val sak = behandlingClient.finnEllerOpprettSak(personGalleri.soeker, SakType.BARNEPENSJON.toString())
            val gyldighetsVurdering =
                GyldighetsResultat(
                    VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
                    emptyList(),
                    Tidspunkt.now().toLocalDatetimeUTC(),
                )

            val behandlingId = behandlingClient.opprettBehandling(sak.id, soeknad.mottattDato, personGalleri)
            behandlingClient.lagreGyldighetsVurdering(behandlingId, gyldighetsVurdering)
            logger.info("Behandling {} startet på sak {}", behandlingId, sak.id)

            context.publish(
                packet.apply {
                    set(GyldigSoeknadVurdert.sakIdKey, sak.id)
                    set(GyldigSoeknadVurdert.behandlingIdKey, behandlingId)
                }.toJson(),
            )

            logger.info("Vurdert gyldighet av søknad er fullført")
        } catch (e: Exception) {
            logger.error("Gyldighetsvurdering av søknad om barnepensjon feilet", e)
            throw e
        }
    }

    private fun JsonMessage.soeknad() =
        this[FordelerFordelt.skjemaInfoKey].let {
            objectMapper.treeToValue<Barnepensjon>(
                it,
            )
        }
}
