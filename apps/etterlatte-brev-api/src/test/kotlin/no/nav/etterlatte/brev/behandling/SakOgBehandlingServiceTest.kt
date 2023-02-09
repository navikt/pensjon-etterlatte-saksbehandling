package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

internal class SakOgBehandlingServiceTest {

    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val beregningKlient = mockk<BeregningKlient>()
    private val saksbehandlere = mapOf(
        SAKSBEHANDLER_IDENT to "0904"
    )

    private val service =
        SakOgBehandlingService(vedtaksvurderingKlient, grunnlagKlient, beregningKlient, saksbehandlere)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(vedtaksvurderingKlient, grunnlagKlient, beregningKlient)
    }

    @Test
    fun `SakOgBehandling fungerer som forventet`() {
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(SAK_ID, ACCESS_TOKEN) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettEnkelBeregning()

        val behandling = runBlocking {
            service.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER_IDENT, ACCESS_TOKEN)
        }

        assertEquals(SAK_ID, behandling.sakId)
        assertEquals(BEHANDLING_ID, behandling.behandlingId)
        assertEquals(Spraak.NB, behandling.spraak)
        assertEquals("Innsend Innsender", behandling.persongalleri.innsender.navn)
        assertEquals("Soek Soekeren", behandling.persongalleri.soeker.navn)
        assertEquals("Avdoed Avdoederen", behandling.persongalleri.avdoed.navn)
        assertEquals(VedtakType.INNVILGELSE, behandling.vedtak.type)
        assertEquals(123L, behandling.vedtak.id)
        assertEquals(SAKSBEHANDLER_IDENT, behandling.vedtak.saksbehandler.ident)
        assertEquals(ATTESTANT_IDENT, behandling.vedtak.attestant?.ident)
        assertEquals(YearMonth.now().atDay(1), behandling.utbetalingsinfo?.virkningsdato)

        coVerify(exactly = 1) { vedtaksvurderingKlient.hentVedtak(BEHANDLING_ID, ACCESS_TOKEN) }
        coVerify(exactly = 1) { grunnlagKlient.hentGrunnlag(SAK_ID, ACCESS_TOKEN) }
        coVerify(exactly = 1) { beregningKlient.hentBeregning(BEHANDLING_ID, ACCESS_TOKEN) }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt beloep`() {
        val AVANSERT_BEHANDLING_ID = "456"

        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(SAK_ID, ACCESS_TOKEN) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(BEHANDLING_ID, any()) } returns opprettEnkelBeregning()
        coEvery { beregningKlient.hentBeregning(AVANSERT_BEHANDLING_ID, any()) } returns opprettAvansertBeregning()

        val enkelBeregning = runBlocking {
            service.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER_IDENT, ACCESS_TOKEN)
        }

        val avansertBeregning = runBlocking {
            service.hentBehandling(SAK_ID, AVANSERT_BEHANDLING_ID, SAKSBEHANDLER_IDENT, ACCESS_TOKEN)
        }

        assertEquals(3063, enkelBeregning.utbetalingsinfo?.beloep)
        assertEquals(2000, avansertBeregning.utbetalingsinfo?.beloep)

        coVerify(exactly = 1) { vedtaksvurderingKlient.hentVedtak(BEHANDLING_ID, ACCESS_TOKEN) }
        coVerify(exactly = 1) { vedtaksvurderingKlient.hentVedtak(AVANSERT_BEHANDLING_ID, ACCESS_TOKEN) }
        coVerify(exactly = 2) { grunnlagKlient.hentGrunnlag(SAK_ID, ACCESS_TOKEN) }
        coVerify(exactly = 1) { beregningKlient.hentBeregning(BEHANDLING_ID, ACCESS_TOKEN) }
        coVerify(exactly = 1) { beregningKlient.hentBeregning(AVANSERT_BEHANDLING_ID, ACCESS_TOKEN) }
    }

    private fun opprettEnkelBeregning() = mockk<BeregningDTO> {
        every { beregningsperioder } returns listOf(
            opprettBeregningsperiode(
                YearMonth.now(),
                beloep = 3063
            )
        )
    }

    private fun opprettAvansertBeregning() = mockk<BeregningDTO> {
        every { beregningsperioder } returns listOf(
            opprettBeregningsperiode(
                YearMonth.of(2022, 6),
                YearMonth.of(2022, 12),
                1000
            ),
            opprettBeregningsperiode(
                YearMonth.of(2023, 1),
                YearMonth.of(2023, 6),
                2000
            ),
            opprettBeregningsperiode(
                YearMonth.of(2023, 7),
                beloep = 3000
            )
        )
    }

    private fun opprettVedtak() = mockk<Vedtak> {
        every { sak.ident } returns "ident"
        every { vedtakId } returns 123L
        every { type } returns VedtakType.INNVILGELSE
        every { virk } returns Periode(YearMonth.now(), null)
        every { vedtakFattet } returns VedtakFattet(SAKSBEHANDLER_IDENT, "Ansvarlig enhet", ZonedDateTime.now())
        every { attestasjon } returns Attestasjon(ATTESTANT_IDENT, "Attestant enhet", ZonedDateTime.now())
    }

    private fun opprettGrunnlag() = mockk<Grunnlag> {
        every { sak } returns mapOf(
            Opplysningstype.SPRAAK to opprettOpplysning(Spraak.NB.toJsonNode()),
            Opplysningstype.INNSENDER_SOEKNAD_V1 to opprettOpplysning(
                InnsenderSoeknad(
                    PersonType.INNSENDER,
                    "Innsend",
                    "Innsender",
                    FNR
                ).toJsonNode()
            )
        )
        every { metadata.sakId } returns 123L
        every { soeker } returns mapOf(
            Opplysningstype.NAVN to opprettOpplysning(Navn("Soek", "Soekeren").toJsonNode()),
            Opplysningstype.FOEDSELSNUMMER to opprettOpplysning(FNR.toJsonNode())
        )
        every { familie } returns listOf(mapOf())
        every { hentAvdoed() } returns mapOf(
            Opplysningstype.NAVN to opprettOpplysning(Navn("Avdoed", "Avdoederen").toJsonNode()),
            Opplysningstype.DOEDSDATO to opprettOpplysning(LocalDate.now().toJsonNode())
        )
    }

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            STATISK_UUID,
            GRUNNLAGSOPPLYSNING_PDL,
            jsonNode
        )

    private fun opprettBeregningsperiode(fom: YearMonth, tom: YearMonth? = null, beloep: Int) = Beregningsperiode(
        fom,
        tom,
        beloep,
        null,
        1000,
        10000,
        10
    )

    private companion object {
        private val FNR = Foedselsnummer.of("11057523044")
        private val GRUNNLAGSOPPLYSNING_PDL = Grunnlagsopplysning.Pdl("pdl", Instant.now(), null, null)
        private val STATISK_UUID = UUID.randomUUID()
        private val BEHANDLING_ID = "123"
        private val ACCESS_TOKEN = "321"
        private val SAKSBEHANDLER_IDENT = "Z1235"
        private val ATTESTANT_IDENT = "Z54321"
        private val SAK_ID = 123L
    }
}