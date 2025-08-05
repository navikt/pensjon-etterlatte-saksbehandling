package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.ETTEROPPGJOER_RESULTAT_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.ETTEROPPGJOER_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.EtteroppgjoerForbehandlingStatistikkDto
import no.nav.etterlatte.libs.common.behandling.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class EtteroppgjoerHendelseService(
    private val rapidPubliserer: KafkaProdusent<String, String>,
    private val hendelseDao: HendelseDao,
) {
    private val logger: Logger = LoggerFactory.getLogger(EtteroppgjoerHendelseService::class.java)

    fun registrerOgSendEtteroppgjoerHendelse(
        etteroppgjoerForbehandling: EtteroppgjoerForbehandling,
        etteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto? = null,
        hendelseType: EtteroppgjoerHendelseType,
        saksbehandler: String?,
        utlandstilknytningType: UtlandstilknytningType?,
    ) {
        hendelseDao.etteroppgjoerHendelse(
            forbehandlingId = etteroppgjoerForbehandling.id,
            sakId = etteroppgjoerForbehandling.sak.id,
            hendelseType = hendelseType,
            inntruffet = Tidspunkt.Companion.now(),
            saksbehandler = saksbehandler,
            kommentar = null,
            begrunnelse = null,
        )

        if (hendelseType.skalSendeStatistikk) {
            sendKafkaMelding(
                etteroppgjoerForbehandling = etteroppgjoerForbehandling,
                hendelseType = hendelseType,
                etteroppgjoerResultat = etteroppgjoerResultat,
                utlandstilknytningType = utlandstilknytningType,
                saksbehandler = saksbehandler,
            )
        }
    }

    private fun sendKafkaMelding(
        etteroppgjoerForbehandling: EtteroppgjoerForbehandling,
        hendelseType: EtteroppgjoerHendelseType,
        etteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto?,
        utlandstilknytningType: UtlandstilknytningType?,
        saksbehandler: String?,
    ) {
        val correlationId = getCorrelationId()
        val standardfelter =
            mapOf(
                CORRELATION_ID_KEY to correlationId,
                TEKNISK_TID_KEY to LocalDateTime.now(),
                ETTEROPPGJOER_STATISTIKK_RIVER_KEY to
                    EtteroppgjoerForbehandlingStatistikkDto(
                        forbehandling = etteroppgjoerForbehandling.tilDto(),
                        utlandstilknytningType = utlandstilknytningType,
                        saksbehandler = saksbehandler,
                    ),
            )
        val meldingMap =
            when (etteroppgjoerResultat) {
                null -> standardfelter
                else ->
                    standardfelter +
                        mapOf(
                            ETTEROPPGJOER_RESULTAT_RIVER_KEY to etteroppgjoerResultat,
                        )
            }

        rapidPubliserer
            .publiser(
                noekkel = etteroppgjoerForbehandling.id.toString(),
                verdi =
                    JsonMessage.Companion
                        .newMessage(
                            eventName = hendelseType.lagEventnameForType(),
                            map = meldingMap,
                        ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Sendte ${hendelseType.lagEventnameForType()}-melding for forbehandling med id=" +
                        "${etteroppgjoerForbehandling.id}, partition: $partition, offset: $offset, " +
                        "correlationId: $correlationId.",
                )
            }
    }
}
