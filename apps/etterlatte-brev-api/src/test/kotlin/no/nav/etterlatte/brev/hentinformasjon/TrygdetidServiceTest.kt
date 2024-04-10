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
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.trygdetid.TrygdetidType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.UUID

internal class TrygdetidServiceTest {
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val beregningKlient = mockk<BeregningKlient>()
    private val service = TrygdetidService(trygdetidKlient, beregningKlient)

    @Test
    fun `henter trygdetid nasjonal beregning`() {
        val behandlingId = UUID.randomUUID()
        val bruker = mockk<BrukerTokenInfo>()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns
            listOf(
                trygdetidDto(
                    behandlingId,
                    samletNorge = 23,
                ),
            )

        coEvery { beregningKlient.hentBeregning(behandlingId, bruker) } returns
            mockk<BeregningDTO> {
                every { beregningsperioder } returns
                    listOf(
                        mockk {
                            every { trygdetidForIdent } returns AVDOED_FOEDSELSNUMMER.value
                            every { beregningsMetode } returns BeregningsMetode.NASJONAL
                        },
                    )
            }
        val trygdetid = runBlocking { service.finnTrygdetid(behandlingId, bruker) }.single()
        Assertions.assertEquals(23, trygdetid.aarTrygdetid)
        Assertions.assertEquals(0, trygdetid.maanederTrygdetid)
        Assertions.assertEquals(BeregnetTrygdetidGrunnlagDto(dager = 0, maaneder = 10, aar = 2), trygdetid.perioder[0].opptjeningsperiode)
    }

    @Test
    fun `henter ut prorata riktig`() {
        val behandlingId = UUID.randomUUID()
        val bruker = mockk<BrukerTokenInfo>()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns
            listOf(
                trygdetidDto(
                    behandlingId,
                    samletTrygdetidTeoretisk = 40,
                    prorataBroek = IntBroek(13, 37),
                ),
            )

        coEvery { beregningKlient.hentBeregning(behandlingId, bruker) } returns
            mockk<BeregningDTO> {
                every { beregningsperioder } returns
                    listOf(
                        mockk {
                            every { trygdetidForIdent } returns AVDOED_FOEDSELSNUMMER.value
                            every { beregningsMetode } returns BeregningsMetode.PRORATA
                        },
                    )
            }
        val trygdetid = runBlocking { service.finnTrygdetid(behandlingId, bruker) }.single()
        Assertions.assertEquals(40, trygdetid.aarTrygdetid)
        Assertions.assertEquals(13, trygdetid.prorataBroek?.teller)
        Assertions.assertEquals(37, trygdetid.prorataBroek?.nevner)
        Assertions.assertEquals(0, trygdetid.maanederTrygdetid)
        Assertions.assertEquals(BeregnetTrygdetidGrunnlagDto(dager = 0, maaneder = 10, aar = 2), trygdetid.perioder[0].opptjeningsperiode)
    }

    @Test
    fun `setter overstyrt trygdetid uten trygdetidsgrunnlag`() {
        val behandlingId = UUID.randomUUID()
        val bruker = mockk<BrukerTokenInfo>()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns
            listOf(
                trygdetidDto(
                    behandlingId = behandlingId,
                    overstyrt = true,
                    trygdetidsgrunnlag = emptyList(),
                    samletTrygdetidTeoretisk = 40,
                    prorataBroek = IntBroek(13, 37),
                ),
            )

        coEvery { beregningKlient.hentBeregning(behandlingId, bruker) } returns
            mockk<BeregningDTO> {
                every { beregningsperioder } returns
                    listOf(
                        mockk {
                            every { trygdetidForIdent } returns AVDOED_FOEDSELSNUMMER.value
                            every { beregningsMetode } returns BeregningsMetode.PRORATA
                        },
                    )
            }
        val trygdetid = runBlocking { service.finnTrygdetid(behandlingId, bruker) }.single()

        Assertions.assertTrue(trygdetid.overstyrt)
        Assertions.assertTrue(trygdetid.perioder.isEmpty())
        Assertions.assertEquals(40, trygdetid.aarTrygdetid)
    }

    private fun trygdetidDto(
        behandlingId: UUID,
        overstyrt: Boolean = false,
        trygdetidsgrunnlag: List<TrygdetidGrunnlagDto> =
            listOf(
                TrygdetidGrunnlagDto(
                    id = UUID.randomUUID(),
                    type = TrygdetidType.FAKTISK.name,
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
        samletNorge: Int? = null,
        samletTrygdetidTeoretisk: Int? = null,
        prorataBroek: IntBroek? = null,
    ) = TrygdetidDto(
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
                        samletTrygdetidNorge = samletNorge,
                        samletTrygdetidTeoretisk = samletTrygdetidTeoretisk,
                        prorataBroek = prorataBroek,
                        overstyrt = overstyrt,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                tidspunkt = Tidspunkt.now(),
            ),
        trygdetidGrunnlag = trygdetidsgrunnlag,
        opplysninger =
            GrunnlagOpplysningerDto(
                avdoedDoedsdato = null,
                avdoedFoedselsdato = null,
                avdoedFylteSeksten = null,
                avdoedFyllerSeksti = null,
            ),
        overstyrtNorskPoengaar = null,
        ident = AVDOED_FOEDSELSNUMMER.value,
        opplysningerDifferanse = OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>()),
    )
}
