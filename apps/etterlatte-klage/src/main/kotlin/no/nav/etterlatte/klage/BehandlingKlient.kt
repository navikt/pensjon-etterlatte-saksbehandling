package no.nav.etterlatte.klage

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.klage.modell.BehandlingEvent
import no.nav.etterlatte.klage.modell.BehandlingEventType
import no.nav.etterlatte.klage.modell.KlageinstansUtfall
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class BehandlingKlient(val behandlingHttpClient: HttpClient, val resourceUrl: String) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun haandterHendelse(record: ConsumerRecord<String, String>) {
        logger.debug(
            "Behandler klage-record med id: {}, partition {}, offset: {}",
            record.key(),
            record.partition(),
            record.offset(),
        )
        val klageHendelse = objectMapper.readValue<BehandlingEvent>(record.value())
        logger.info(
            "Håndterer klagehendelse ${klageHendelse.eventId}",
        )

        if (klageHendelse.kilde == Vedtaksloesning.GJENNY.name) {
            postTilBehandling(klageHendelse)
        }
    }

    private fun postTilBehandling(klageHendelse: BehandlingEvent) =
        runBlocking {
            val body =
                when (klageHendelse.type) {
                    BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                        Kabalrespons(
                            KabalStatus.FERDIGSTILT,
                            requireNotNull(klageHendelse.detaljer.klagebehandlingAvsluttet).utfall.tilResultat(),
                        )

                    BehandlingEventType.ANKEBEHANDLING_OPPRETTET ->
                        Kabalrespons(
                            KabalStatus.OPPRETTET,
                            BehandlingResultat.IKKE_SATT,
                        )

                    BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                        Kabalrespons(
                            KabalStatus.FERDIGSTILT,
                            requireNotNull(klageHendelse.detaljer.ankebehandlingAvsluttet).utfall.tilResultat(),
                        )

                    BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET ->
                        Kabalrespons(
                            KabalStatus.OPPRETTET,
                            requireNotNull(
                                klageHendelse.detaljer.ankeITrygderettenbehandlingOpprettet,
                            ).utfall?.tilResultat()
                                ?: BehandlingResultat.IKKE_SATT,
                        )

                    BehandlingEventType.BEHANDLING_FEILREGISTRERT ->
                        Kabalrespons(
                            KabalStatus.FERDIGSTILT,
                            BehandlingResultat.HENLAGT,
                        )
                }

            behandlingHttpClient.patch(
                "$resourceUrl/klage/${klageHendelse.kildeReferanse}/kabalstatus",
            ) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
}

// Vi må dobbeltsjekke at denne mappinga blir riktig
fun KlageinstansUtfall.tilResultat(): BehandlingResultat =
    when (this) {
        KlageinstansUtfall.TRUKKET -> BehandlingResultat.HENLAGT
        KlageinstansUtfall.RETUR -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.OPPHEVET -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.MEDHOLD -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.DELVIS_MEDHOLD -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.STADFESTELSE -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.UGUNST -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.AVVIST -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.INNSTILLING_STADFESTELSE -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.INNSTILLING_AVVIST -> BehandlingResultat.IKKE_MEDHOLD
    }
