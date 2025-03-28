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
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.klage.kodeverk.Fagsystem
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class BehandlingKlient(
    private val behandlingHttpClient: HttpClient,
    private val resourceUrl: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun haandterHendelse(record: ConsumerRecord<String, String>) {
        logger.debug(
            "Behandler klage-record med id: {}, partition {}, offset: {}",
            record.key(),
            record.partition(),
            record.offset(),
        )
        val klageHendelse = objectMapper.readValue<BehandlingEvent>(record.value())
        logger.info(
            "Håndterer klagehendelse ${klageHendelse.eventId} fra opprinnelig kilde ${klageHendelse.kilde}",
        )

        if (klageHendelse.kilde == Fagsystem.EY.name) {
            postTilBehandling(klageHendelse)
        }
    }

    private fun postTilBehandling(klageHendelse: BehandlingEvent) =
        runBlocking {
            logger.info("Så en klagehendelse som har kilde EY. Sender hendelsen til behandling")

            val body =
                try {
                    when (klageHendelse.type) {
                        BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                            Kabalrespons(
                                KabalStatus.FERDIGSTILT,
                                krevIkkeNull(klageHendelse.detaljer.klagebehandlingAvsluttet) {
                                    "Detaljer på klage mangler"
                                }.utfall.tilResultat(),
                            )

                        // TODO: Se på hvordan vi håndterer anke -- det burde nok sette et eget flagg
                        //  og ikke overstyre første status
                        BehandlingEventType.ANKEBEHANDLING_OPPRETTET ->
                            Kabalrespons(
                                KabalStatus.OPPRETTET,
                                BehandlingResultat.IKKE_SATT,
                            )

                        BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                            Kabalrespons(
                                KabalStatus.FERDIGSTILT,
                                krevIkkeNull(klageHendelse.detaljer.ankebehandlingAvsluttet) {
                                    "Detailjer på avsluttet anke mangler"
                                }.utfall.tilResultat(),
                            )

                        BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET ->
                            Kabalrespons(
                                KabalStatus.OPPRETTET,
                                krevIkkeNull(klageHendelse.detaljer.ankeITrygderettenbehandlingOpprettet) {
                                    "Detaljer på anke i trygderetten mangler"
                                }.utfall?.tilResultat() ?: BehandlingResultat.IKKE_SATT,
                            )

                        BehandlingEventType.BEHANDLING_FEILREGISTRERT ->
                            Kabalrespons(
                                KabalStatus.FERDIGSTILT,
                                BehandlingResultat.HENLAGT,
                            )

                        BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET ->
                            Kabalrespons(
                                KabalStatus.FERDIGSTILT,
                                krevIkkeNull(klageHendelse.detaljer.klagebehandlingAvsluttet) {
                                    "Detaljer på avsluttet klagebehandling mangler"
                                }.utfall.tilResultat(),
                            )
                        BehandlingEventType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET ->
                            Kabalrespons(
                                KabalStatus.FERDIGSTILT,
                                krevIkkeNull(klageHendelse.detaljer.klagebehandlingAvsluttet) {
                                    "Detaljer på avsluttet klagebehandling mangler"
                                }.utfall.tilResultat(),
                            )
                    }
                } catch (e: Exception) {
                    logger.error("Kunne ikke mappe ut kabalresponsen riktig. Hendelsen er logget til sikkerlogg")
                    sikkerLogg.error("Kunne ikke mappe ut kabalresponsen riktig, eventet vi mottok var $klageHendelse")
                    throw e
                }

            behandlingHttpClient.patch(
                "$resourceUrl/api/klage/${klageHendelse.kildeReferanse}/kabalstatus",
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
        KlageinstansUtfall.HEVET -> BehandlingResultat.HEVET
        KlageinstansUtfall.RETUR -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.OPPHEVET -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.MEDHOLD -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.DELVIS_MEDHOLD -> BehandlingResultat.MEDHOLD
        KlageinstansUtfall.STADFESTELSE -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.UGUNST -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.AVVIST -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.INNSTILLING_STADFESTELSE -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.INNSTILLING_AVVIST -> BehandlingResultat.IKKE_MEDHOLD
        KlageinstansUtfall.MEDHOLD_ETTER_FVL_35 -> BehandlingResultat.MEDHOLD
    }
