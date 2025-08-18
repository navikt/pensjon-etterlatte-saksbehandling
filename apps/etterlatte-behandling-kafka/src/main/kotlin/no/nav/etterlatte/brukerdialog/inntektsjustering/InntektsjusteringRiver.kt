package no.nav.etterlatte.brukerdialog.inntektsjustering

import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.OpprettJournalpostResponse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendt
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendtHendelseType
import no.nav.etterlatte.libs.common.inntektsjustering.Inntektsjustering
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth

internal class InntektsjusteringRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingClient,
    private val journalfoerInntektsjusteringService: JournalfoerInntektsjusteringService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, InntektsjusteringInnsendtHendelseType.EVENT_NAME_INNSENDT) {
            validate { it.requireKey(InntektsjusteringInnsendt.inntektsjusteringInnhold) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val inntektsjustering = packet.inntektsjustering()
        try {
            logger.info("Mottatt innmeldt inntektsjustering (id=${inntektsjustering.id})")

            val sak =
                runBlocking {
                    behandlingKlient.finnEllerOpprettSak(inntektsjustering.fnr, SakType.OMSTILLINGSSTOENAD)
                }

            val journalpostResponse =
                journalfoerInntektsjusteringService.opprettJournalpost(sak, inntektsjustering)
                    ?: run {
                        logger.warn("Kan ikke fortsette uten respons fra dokarkiv. Retry kjøres automatisk...")
                        return
                    }

            startBehandlingAvInntektsjustering(sak, journalpostResponse, inntektsjustering)
        } catch (e: Exception) {
            // Selvbetjening-backend vil fortsette å sende nye meldinger til dette ikke feiler
            logger.error(
                "Journalføring eller opprettelse av behandling/oppgave for inntektsjustering inntektsjusteringsid=${inntektsjustering.id}",
                e,
            )
        }
    }

    private fun startBehandlingAvInntektsjustering(
        sak: Sak,
        journalpostResponse: OpprettJournalpostResponse,
        inntektsjustering: Inntektsjustering,
    ) {
        behandlingKlient.behandleInntektsjustering(
            MottattInntektsjustering(
                sak = sak.id,
                inntektsjusteringId = inntektsjustering.id,
                journalpostId = journalpostResponse.journalpostId,
                mottattDato = LocalDateTime.ofInstant(inntektsjustering.tidspunkt, norskTidssone),
                inntektsaar = inntektsjustering.inntektsaar,
                arbeidsinntekt = inntektsjustering.arbeidsinntekt,
                naeringsinntekt = inntektsjustering.naeringsinntekt,
                afpInntekt = inntektsjustering.afpInntekt,
                inntektFraUtland = inntektsjustering.inntektFraUtland,
                datoForAaGaaAvMedAlderspensjon =
                    inntektsjustering.datoForAaGaaAvMedAlderspensjon?.let {
                        YearMonth.from(
                            it,
                        )
                    },
            ),
        )
    }

    private fun JsonMessage.inntektsjustering(): Inntektsjustering =
        objectMapper.treeToValue<Inntektsjustering>(this[InntektsjusteringInnsendt.inntektsjusteringInnhold])
}
