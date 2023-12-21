package no.nav.etterlatte.brev.hentinformasjon

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.UUID

internal class TrygdetidServiceTest {
    private val trygdetidKlient = mockk<TrygdetidKlient>()

    @Test
    fun `henter trygdetid nasjonal beregning`() {
        val service = TrygdetidService(trygdetidKlient)
        val behandlingId = UUID.randomUUID()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetidDto(behandlingId))

        val beregning =
            mockk<BeregningDTO> {
                every { beregningsperioder } returns
                    listOf(
                        mockk {
                            every { trygdetidForIdent } returns AVDOED_FOEDSELSNUMMER.value
                            every { samletNorskTrygdetid } returns 23
                            every { beregningsMetode } returns BeregningsMetode.NASJONAL
                        },
                    )
            }
        val trygdetid = runBlocking { service.finnTrygdetidsgrunnlag(behandlingId, beregning, mockk()) }
        Assertions.assertEquals(23, trygdetid!!.aarTrygdetid)
        Assertions.assertEquals(0, trygdetid.maanederTrygdetid)
        Assertions.assertEquals(BeregnetTrygdetidGrunnlagDto(dager = 0, maaneder = 10, aar = 2), trygdetid.perioder[0].opptjeningsperiode)
    }

    @Test
    fun `henter ut prorata riktig`() {
        val service = TrygdetidService(trygdetidKlient)
        val behandlingId = UUID.randomUUID()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns listOf(trygdetidDto(behandlingId))

        val beregning =
            mockk<BeregningDTO> {
                every { beregningsperioder } returns
                    listOf(
                        mockk {
                            every { trygdetidForIdent } returns AVDOED_FOEDSELSNUMMER.value
                            every { samletTeoretiskTrygdetid } returns 40
                            every { broek } returns IntBroek(13, 37)
                            every { beregningsMetode } returns BeregningsMetode.PRORATA
                        },
                    )
            }
        val trygdetid = runBlocking { service.finnTrygdetidsgrunnlag(behandlingId, beregning, mockk()) }
        Assertions.assertEquals(40, trygdetid!!.aarTrygdetid)
        Assertions.assertEquals(13, trygdetid.prorataBroek?.teller)
        Assertions.assertEquals(37, trygdetid.prorataBroek?.nevner)
        Assertions.assertEquals(0, trygdetid.maanederTrygdetid)
        Assertions.assertEquals(BeregnetTrygdetidGrunnlagDto(dager = 0, maaneder = 10, aar = 2), trygdetid.perioder[0].opptjeningsperiode)
    }

    private fun trygdetidDto(behandlingId: UUID) =
        TrygdetidDto(
            id = UUID.randomUUID(),
            behandlingId = behandlingId,
            beregnetTrygdetid =
                DetaljertBeregnetTrygdetidDto(
                    resultat =
                        DetaljertBeregnetTrygdetidResultat(
                            faktiskTrygdetidNorge = null,
                            faktiskTrygdetidTeoretisk = null,
                            fremtidigTrygdetidNorge = null,
                            fremtidigTrygdetidTeoretisk = null,
                            samletTrygdetidNorge = 42,
                            samletTrygdetidTeoretisk = null,
                            prorataBroek = null,
                            overstyrt = false,
                        ),
                    tidspunkt = Tidspunkt.now(),
                ),
            trygdetidGrunnlag =
                listOf(
                    TrygdetidGrunnlagDto(
                        id = UUID.randomUUID(),
                        type = "",
                        bosted = "NOR",
                        periodeFra = LocalDate.of(2020, Month.MARCH, 5),
                        periodeTil = LocalDate.of(2023, Month.JANUARY, 1),
                        kilde = null,
                        beregnet =
                            BeregnetTrygdetidGrunnlagDto(
                                dager = 0,
                                maaneder = 10,
                                aar = 2,
                            ),
                        begrunnelse = null,
                        poengInnAar = false,
                        poengUtAar = false,
                        prorata = true,
                    ),
                ),
            opplysninger =
                GrunnlagOpplysningerDto(
                    avdoedDoedsdato = null,
                    avdoedFoedselsdato = null,
                    avdoedFylteSeksten = null,
                    avdoedFyllerSeksti = null,
                ),
            overstyrtNorskPoengaar = null,
            ident = AVDOED_FOEDSELSNUMMER.value,
        )
}
