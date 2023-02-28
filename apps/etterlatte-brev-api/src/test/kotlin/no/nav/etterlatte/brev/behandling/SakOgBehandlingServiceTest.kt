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
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.token.Bruker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*
import no.nav.etterlatte.brev.behandling.Beregningsperiode as BrevBeregningsperiode

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
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()

        val behandling = runBlocking {
            service.hentBehandling(SAK_ID, BEHANDLING_ID, ACCESS_TOKEN)
        }

        assertEquals(SAK_ID, behandling.sakId)
        assertEquals(BEHANDLING_ID, behandling.behandlingId)
        assertEquals(Spraak.NB, behandling.spraak)
        assertEquals("Innsend Innsender", behandling.persongalleri.innsender.navn)
        assertEquals("Søker Barn", behandling.persongalleri.soeker.navn)
        assertEquals("Død Far", behandling.persongalleri.avdoed.navn)
        assertEquals(VedtakType.INNVILGELSE, behandling.vedtak.type)
        assertEquals(123L, behandling.vedtak.id)
        assertEquals(SAKSBEHANDLER_IDENT, behandling.vedtak.saksbehandler.ident)
        assertEquals(ATTESTANT_IDENT, behandling.vedtak.attestant?.ident)
        assertEquals(YearMonth.now().atDay(1), behandling.utbetalingsinfo?.virkningsdato)

        coVerify(exactly = 1) {
            vedtaksvurderingKlient.hentVedtak(BEHANDLING_ID, any())
            grunnlagKlient.hentGrunnlag(SAK_ID, any())
            beregningKlient.hentBeregning(BEHANDLING_ID, any())
        }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt informasjon`() {
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()

        val behandling = runBlocking {
            service.hentBehandling(SAK_ID, BEHANDLING_ID, ACCESS_TOKEN)
        }

        assertEquals(null, behandling.utbetalingsinfo?.antallBarn)
        assertEquals(3063, behandling.utbetalingsinfo?.beloep)
        assertEquals(YearMonth.now().atDay(1), behandling.utbetalingsinfo?.virkningsdato)
        assertEquals(false, behandling.utbetalingsinfo?.soeskenjustering)
        assertEquals(
            listOf(BREV_BEREGNINGSPERIODE),
            behandling.utbetalingsinfo?.beregningsperioder
        )

        coVerify(exactly = 1) {
            vedtaksvurderingKlient.hentVedtak(BEHANDLING_ID, any())
            grunnlagKlient.hentGrunnlag(SAK_ID, any())
            beregningKlient.hentBeregning(BEHANDLING_ID, any())
        }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt antall barn ved soeskenjustering`() {
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregningSoeskenjustering()

        val behandling = runBlocking {
            service.hentBehandling(SAK_ID, BEHANDLING_ID, ACCESS_TOKEN)
        }

        assertEquals(2, behandling.utbetalingsinfo?.antallBarn)
        assertTrue(behandling.utbetalingsinfo?.soeskenjustering!!)

        coVerify(exactly = 1) { vedtaksvurderingKlient.hentVedtak(any(), any()) }
        coVerify(exactly = 1) { grunnlagKlient.hentGrunnlag(any(), any()) }
        coVerify(exactly = 1) { beregningKlient.hentBeregning(any(), any()) }
    }

    private fun opprettBeregning() = mockk<BeregningDTO> {
        every { beregningsperioder } returns listOf(
            opprettBeregningsperiode(
                YearMonth.now(),
                beloep = 3063
            )
        )
    }

    private fun opprettBeregningSoeskenjustering() = mockk<BeregningDTO> {
        every { beregningsperioder } returns listOf(
            opprettBeregningsperiode(
                YearMonth.now(),
                beloep = 3063,
                soeskenFlokk = listOf("barn2")
            )
        )
    }

    private fun opprettVedtak() = mockk<Vedtak> {
        every { sak } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID)
        every { behandling.id } returns BEHANDLING_ID
        every { vedtakId } returns 123L
        every { type } returns VedtakType.INNVILGELSE
        every { virk } returns Periode(YearMonth.now(), null)
        every { vedtakFattet } returns VedtakFattet(SAKSBEHANDLER_IDENT, "Ansvarlig enhet", ZonedDateTime.now())
        every { attestasjon } returns Attestasjon(ATTESTANT_IDENT, "Attestant enhet", ZonedDateTime.now())
    }

    private fun opprettGrunnlag() = GrunnlagTestData(
        opplysningsmapSakOverrides = mapOf(
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
    ).hentOpplysningsgrunnlag()

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            STATISK_UUID,
            GRUNNLAGSOPPLYSNING_PDL,
            jsonNode
        )

    private fun opprettBeregningsperiode(
        fom: YearMonth,
        tom: YearMonth? = null,
        beloep: Int,
        soeskenFlokk: List<String>? = null
    ) = Beregningsperiode(
        fom,
        tom,
        beloep,
        soeskenFlokk,
        1000,
        10000,
        10
    )

    private companion object {
        private val FNR = Foedselsnummer.of("11057523044")
        private val GRUNNLAGSOPPLYSNING_PDL = Grunnlagsopplysning.Pdl("pdl", Instant.now(), null, null)
        private val STATISK_UUID = UUID.randomUUID()
        private val BEHANDLING_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "Z1235"
        private val ACCESS_TOKEN = Bruker.of("321", SAKSBEHANDLER_IDENT, null, null)
        private const val ATTESTANT_IDENT = "Z54321"
        private const val SAK_ID = 123L
        private val BREV_BEREGNINGSPERIODE = BrevBeregningsperiode(YearMonth.now().atDay(1), null, 10000, 1, 3063, 10)
    }
}