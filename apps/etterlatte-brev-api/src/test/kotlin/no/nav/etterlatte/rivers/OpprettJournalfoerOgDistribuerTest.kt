package no.nav.etterlatte.rivers

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class OpprettJournalfoerOgDistribuer {
    @Test
    fun `melding om attestert vedtak gjoer at vi potensielt oppretter vedtaksbrev, og saa journalfoerer og distribuerer brevet `() {
        val behandlingId = UUID.randomUUID()
        val brev = lagBrev(behandlingId)
        val journalfoerBrevService =
            mockk<JournalfoerBrevService>().also {
                coEvery { it.journalfoerVedtaksbrev(any(), any()) } returns
                    Pair(
                        OpprettJournalpostResponse(
                            journalpostId = "123",
                            journalpostferdigstilt = true,
                        ),
                        brev.id,
                    )
            }
        val distribusjonService =
            mockk<Brevdistribuerer>().also {
                coEvery { it.distribuer(brev.id, any(), any()) } returns ""
            }
        val testRapid =
            TestRapid().apply {
                JournalfoerVedtaksbrevRiver(this, journalfoerBrevService)
                DistribuerBrevRiver(this, distribusjonService)
            }

        testRapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    mapOf(
                        CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                        VedtakKafkaHendelseHendelseType.ATTESTERT.lagParMedEventNameKey(),
                        "vedtak" to lagVedtakDto(behandlingId),
                        KILDE_KEY to Vedtaksloesning.GJENNY.name,
                    ),
                ).toJson(),
        )

        val distribuermelding = testRapid.hentMelding(0)
        Assertions.assertEquals(BrevHendelseType.JOURNALFOERT.lagEventnameForType(), distribuermelding.somMap()[EVENT_NAME_KEY])
        testRapid.sendTestMessage(distribuermelding)

        val distribuert = testRapid.hentMelding(1).somMap()
        Assertions.assertEquals(BrevHendelseType.DISTRIBUERT.lagEventnameForType(), distribuert[EVENT_NAME_KEY])
    }

    private fun lagBrev(behandlingId: UUID?) =
        Brev(
            id = 2L,
            sakId = 1L,
            behandlingId = behandlingId,
            tittel = "tittel",
            spraak = Spraak.NB,
            prosessType = BrevProsessType.AUTOMATISK,
            soekerFnr = "123",
            status = Status.FERDIGSTILT,
            Tidspunkt.now(),
            Tidspunkt.now(),
            mottaker =
                Mottaker(
                    "Langsom Hest",
                    mockk(),
                    null,
                    Adresse(adresseType = "privat", landkode = "NO", land = "Norge"),
                ),
            brevtype = Brevtype.INFORMASJON,
        )

    private fun lagVedtakDto(behandlingId: UUID) =
        VedtakDto(
            id = 1L,
            status = VedtakStatus.IVERKSATT,
            sak =
                VedtakSak(
                    ident = "Sak1",
                    sakType = SakType.BARNEPENSJON,
                    id = 2L,
                ),
            behandlingId = behandlingId,
            type = VedtakType.INNVILGELSE,
            vedtakFattet =
                VedtakFattet(
                    ansvarligSaksbehandler = "Peder Ås",
                    ansvarligEnhet = "Lillevik",
                    tidspunkt = Tidspunkt.now(),
                ),
            attestasjon =
                Attestasjon(
                    attestant = "Lars Holm",
                    attesterendeEnhet = "Lillevik",
                    tidspunkt = Tidspunkt.now(),
                ),
            innhold =
                VedtakInnholdDto.VedtakBehandlingDto(
                    virkningstidspunkt = YearMonth.now(),
                    behandling =
                        Behandling(
                            type = BehandlingType.FØRSTEGANGSBEHANDLING,
                            id = behandlingId,
                            revurderingsaarsak = null,
                            revurderingInfo = null,
                        ),
                    utbetalingsperioder = listOf(),
                    opphoerFraOgMed = null,
                ),
        )
}

private fun String.somMap() = objectMapper.readValue(this, Map::class.java)

fun TestRapid.hentMelding(index: Int) = inspektør.message(index).toJson()
