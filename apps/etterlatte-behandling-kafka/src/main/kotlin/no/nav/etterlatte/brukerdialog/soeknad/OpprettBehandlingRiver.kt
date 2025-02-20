package no.nav.etterlatte.brukerdialog.soeknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.brukerdialog.soeknad.client.FeiletVedOpprettBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class OpprettBehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingClient: BehandlingClient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV) {
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoKey) }
            validate { it.interestedIn(GyldigSoeknadVurdert.lagretSoeknadIdKey) }
            validate { it.rejectKey(GyldigSoeknadVurdert.behandlingIdKey) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            val soeknad = packet.soeknad()
            val soeknadId = packet[GyldigSoeknadVurdert.lagretSoeknadIdKey].asText().takeIf { it.isNotBlank() }

            val personGalleri = PersongalleriMapper.hentPersongalleriFraSoeknad(soeknad)

            val sakType =
                when (soeknad.type) {
                    SoeknadType.OMSTILLINGSSTOENAD -> SakType.OMSTILLINGSSTOENAD
                    SoeknadType.BARNEPENSJON -> SakType.BARNEPENSJON
                }

            val sak = behandlingClient.finnEllerOpprettSak(personGalleri.soeker.value, sakType)

            val behandlingId =
                try {
                    behandlingClient.opprettBehandling(sak.id, soeknad.mottattDato, personGalleri)
                } catch (e: FeiletVedOpprettBehandling) {
                    val aapenBehandling =
                        behandlingClient
                            .hentSakMedBehandlinger(sak.id)
                            .behandlinger
                            .find { it.status.aapenBehandling() }

                    if (aapenBehandling == null) {
                        logger.error(
                            "Opprettelse av behandling feilet, uten at det finnes åpen behandling (soeknadId=$soeknadId)",
                            e,
                        )

                        throw e
                    } else {
                        logger.warn(
                            "Mottok søknad med id=$soeknadId, men det finnes allerede en åpen behandling på sak ${sak.id} " +
                                "(behandlingId=${aapenBehandling.id}). Dette burde ikke skje og må kontrolleres manuelt.",
                        )
                        aapenBehandling.id
                    }
                }

            logger.info(
                "Opprettelse av behandling fullført " +
                    "(sakId=${sak.id}, behandlingId=$behandlingId, soeknadId=$soeknadId)",
            )

            context.publish(
                packet
                    .apply {
                        set(GyldigSoeknadVurdert.sakIdKey, sak.id)
                        set(GyldigSoeknadVurdert.behandlingIdKey, behandlingId)
                    }.toJson(),
            )
        } catch (e: Exception) {
            logger.error("Uhåndtert feil ved opprettelse av behandling for søknad", e)
            throw e
        }
    }

    private fun JsonMessage.soeknad() = this[SoeknadInnsendt.skjemaInfoKey].let { objectMapper.treeToValue<InnsendtSoeknad>(it) }
}
