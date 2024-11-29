package no.nav.etterlatte.inntektsjustering

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.OpprettJournalpostResponse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendt
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendtHendelseType
import no.nav.etterlatte.libs.common.inntektsjustering.Inntektsjustering
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.sikkerLogg
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
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
        } catch (e: JsonMappingException) {
            sikkerLogg.error("Feil under deserialisering", e)
            logger.error("Feil under deserialisering av inntektsjustering (id=${inntektsjustering.id}). Se sikkerlogg for detaljer.")
            throw e
        } catch (e: Exception) {
            logger.error("Uhåndtert feilsituasjon TODO : $", e)
            // throw e TODO ta stilling til hvordan feil her skal håndteres...
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
                inntektsaar = inntektsjustering.inntektsaar,
                arbeidsinntekt = inntektsjustering.arbeidsinntekt,
                naeringsinntekt = inntektsjustering.naeringsinntekt,
                afpInntekt = inntektsjustering.afpInntekt,
                inntektFraUtland = inntektsjustering.inntektFraUtland,
                datoForAaGaaAvMedAlderspensjon = YearMonth.from(inntektsjustering.datoForAaGaaAvMedAlderspensjon),
            ),
        )
    }

    private fun JsonMessage.inntektsjustering(): Inntektsjustering =
        objectMapper.treeToValue<Inntektsjustering>(this[InntektsjusteringInnsendt.inntektsjusteringInnhold])
}
