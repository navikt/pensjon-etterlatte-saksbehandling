package no.nav.etterlatte.brev.hentinformasjon

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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
    fun `henter trygdetid`() {
        val service = TrygdetidService(trygdetidKlient)
        val behandlingId = UUID.randomUUID()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns trygdetidDto(behandlingId)
        val trygdetid = runBlocking { service.finnTrygdetid(behandlingId, mockk()) }
        Assertions.assertEquals(3, trygdetid!!.aarTrygdetid)
        Assertions.assertEquals(6, trygdetid.maanederTrygdetid)
        Assertions.assertEquals("2", trygdetid.perioder[0].opptjeningsperiode)
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
                        bosted = "Danmark",
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
                        prorata = false,
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
