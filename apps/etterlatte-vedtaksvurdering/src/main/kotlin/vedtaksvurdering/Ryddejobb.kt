package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.util.UUID

class Ryddejobb(
    private val vedtakBehandlingService: VedtakBehandlingService,
    private val publiser: (String, UUID) -> Unit,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun resendJournalfoeringbehovForAlleAttesterteOgIverksatteVedtak() {
        logger.info("Kjører i gang med å sende alle attesterte vedtak ut for journalføring. RYDDEJOBB!")
        val alleAttesterteOgIverksatteVedtak =
            vedtakBehandlingService.hentAttesterteEllerIverksatteVedtakSomSkalSendeBrev()
        alleAttesterteOgIverksatteVedtak.forEach {
            try {
                publiser(
                    JsonMessage.newMessage(
                        mapOf(
                            EVENT_NAME_KEY to VedtakKafkaHendelseType.ATTESTERT.toString(),
                            "vedtak" to it.toNyDto(),
                        ),
                    ).toJson(),
                    it.behandlingId,
                )
                logger.info("Sendte vedtak ${it.id} på nytt!")
            } catch (e: Exception) {
                logger.error("Kunne ikke sende vedtak ${it.id} på nytt, på grunn av feil", e)
            }
        }
    }
}
