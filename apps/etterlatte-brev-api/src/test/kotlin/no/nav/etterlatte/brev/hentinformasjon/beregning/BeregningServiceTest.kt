package no.nav.etterlatte.brev.hentinformasjon.beregning

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsGrunnlagFellesDto
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class BeregningServiceTest {
    private val beregningKlient = mockk<BeregningKlient>()

    private val service = BeregningService(beregningKlient = beregningKlient)

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt informasjon`() {
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { beregningKlient.hentBeregningsGrunnlag(any(), any(), any()) } returns opprettBeregningsgrunnlag()

        val utbetalingsinfo =
            runBlocking {
                service.finnUtbetalingsinfo(BEHANDLING_ID, YearMonth.now(), BRUKERTokenInfo, SakType.BARNEPENSJON)
            }

        Assertions.assertEquals(Kroner(3063), utbetalingsinfo.beloep)
        Assertions.assertEquals(YearMonth.now().atDay(1), utbetalingsinfo.virkningsdato)
        Assertions.assertEquals(false, utbetalingsinfo.soeskenjustering)
        Assertions.assertEquals(
            listOf(BREV_BEREGNINGSPERIODE),
            utbetalingsinfo.beregningsperioder,
        )

        coVerify(exactly = 1) {
            service.hentBeregning(BEHANDLING_ID, any())
            service.hentBeregningsGrunnlag(BEHANDLING_ID, any(), any())
        }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt antall barn ved soeskenjustering`() {
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregningSoeskenjustering()
        coEvery { beregningKlient.hentBeregningsGrunnlag(any(), any(), any()) } returns opprettBeregningsgrunnlag()

        val utbetalingsinfo =
            runBlocking {
                service.finnUtbetalingsinfo(BEHANDLING_ID, YearMonth.now(), BRUKERTokenInfo, SakType.BARNEPENSJON)
            }

        Assertions.assertEquals(2, utbetalingsinfo.antallBarn)
        Assertions.assertTrue(utbetalingsinfo.soeskenjustering)

        coVerify(exactly = 1) {
            service.hentBeregning(any(), any())
            service.hentBeregningsGrunnlag(any(), any(), any())
        }
    }

    private fun opprettBeregning() =
        mockk<BeregningDTO> {
            every { beregningsperioder } returns
                listOf(
                    opprettBeregningsperiode(
                        YearMonth.now(),
                        beloep = 3063,
                    ),
                )
        }

    private fun opprettBeregningsperiode(
        fom: YearMonth,
        tom: YearMonth? = null,
        beloep: Int,
        soeskenFlokk: List<String>? = null,
    ) = Beregningsperiode(
        UUID.randomUUID(),
        fom,
        tom,
        beloep,
        soeskenFlokk,
        null,
        1000,
        10000,
        10,
        beregningsMetode = BeregningsMetode.NASJONAL,
        samletNorskTrygdetid = 10,
        samletTeoretiskTrygdetid = 20,
        broek = null,
    )

    private fun opprettBeregningSoeskenjustering() =
        mockk<BeregningDTO> {
            every { beregningsperioder } returns
                listOf(
                    opprettBeregningsperiode(
                        YearMonth.now(),
                        beloep = 3063,
                        soeskenFlokk = listOf("barn2"),
                    ),
                )
        }

    private fun opprettBeregningsgrunnlag() =
        mockk<BeregningsGrunnlagFellesDto> {
            every { beregningsMetode } returns
                mockk {
                    every { beregningsMetode } returns BeregningsMetode.BEST
                }
        }

    private companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "Z1235"
        private val BRUKERTokenInfo = simpleSaksbehandler(SAKSBEHANDLER_IDENT)
        private val BREV_BEREGNINGSPERIODE =
            no.nav.etterlatte.brev.behandling.Beregningsperiode(
                YearMonth.now().atDay(1),
                null,
                Kroner(10000),
                1,
                Kroner(3063),
                10,
                null,
                null,
                false,
                BeregningsMetode.NASJONAL,
                BeregningsMetode.BEST,
            )
    }
}
