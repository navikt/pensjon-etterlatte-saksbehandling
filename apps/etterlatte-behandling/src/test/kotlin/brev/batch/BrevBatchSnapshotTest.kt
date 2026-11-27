package no.nav.etterlatte.brev.batch

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevBatchSnapshotTest {
    private val sak = snapshotRequest().sak

    private fun uten(
        g: Grunnlag,
        type: Opplysningstype,
    ) = Grunnlag(g.soeker, g.familie, g.sak - type, g.metadata)

    @Test
    fun `spraak maa finnes og vaere eksakt registrert`() {
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        assertEquals(
            "SPRAAK_MANGLER",
            assertThrows(BrevBatchSakFeil::class.java) {
                BrevBatchSnapshot.map(sak, uten(grunnlag, Opplysningstype.SPRAAK), batchRequest())
            }.kode,
        )
        assertEquals(
            "SPRAAK_AVVIK",
            assertThrows(BrevBatchSakFeil::class.java) {
                BrevBatchSnapshot.map(sak, grunnlag, batchRequest().copy(spraak = Spraak.NN))
            }.kode,
        )
    }

    @Test
    fun `voksen OMS og BP beholder persondata tittel tekst og spraak uten verge`() {
        val data =
            GrunnlagTestData(
                opplysningsmapSoekerOverrides =
                    mapOf(
                        Opplysningstype.FOEDSELSDATO to
                            Opplysning.Konstant(
                                java.util.UUID.randomUUID(),
                                no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.automatiskSaksbehandler,
                                LocalDate.now().minusYears(20).toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()
        for (type in listOf(SakType.OMSTILLINGSSTOENAD, SakType.BARNEPENSJON)) {
            val resultat = BrevBatchSnapshot.map(sak.copy(sakType = type), data, batchRequest())
            assertEquals(batchRequest().tittel, resultat.tittel)
            assertEquals(batchRequest().innhold, resultat.innhold)
            assertEquals(Spraak.NB, resultat.spraak)
            assertEquals(false, resultat.soeker.under18)
            assertNull(resultat.verge)
        }
    }

    @Test
    fun `BP under atten bruker forelder eller blokkerer uavklart mottaker`() {
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapSoekerOverrides =
                    mapOf(
                        Opplysningstype.FOEDSELSDATO to
                            Opplysning.Konstant(
                                java.util.UUID.randomUUID(),
                                no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.automatiskSaksbehandler,
                                LocalDate.now().minusYears(12).toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()
        val bp = sak.copy(sakType = SakType.BARNEPENSJON)
        assertNotNull(BrevBatchSnapshot.map(bp, grunnlag, batchRequest()).verge)
        val utenForelder = Grunnlag(grunnlag.soeker, emptyList(), grunnlag.sak, grunnlag.metadata)
        assertEquals(
            "MOTTAKER_UAVKLART",
            assertThrows(BrevBatchSakFeil::class.java) {
                BrevBatchSnapshot.map(bp, utenForelder, batchRequest())
            }.kode,
        )
    }

    @Test
    fun `ukjent og flere verger avvises mens identifisert verge brukes`() {
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val person = grunnlag.soeker.hentSoekerPdlV1()
        val kjent =
            no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt(
                null,
                null,
                no.nav.etterlatte.libs.common.person.VergeEllerFullmektig(
                    no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER,
                    "Vergens navn",
                    null,
                    true,
                ),
                null,
            )

        fun medVerger(verger: List<no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt>): Grunnlag =
            Grunnlag(
                grunnlag.soeker + (
                    Opplysningstype.SOEKER_PDL_V1 to
                        Opplysning.Konstant(
                            java.util.UUID.randomUUID(),
                            no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.automatiskSaksbehandler,
                            person!!.verdi.copy(vergemaalEllerFremtidsfullmakt = verger).toJsonNode(),
                        )
                ),
                grunnlag.familie,
                grunnlag.sak,
                grunnlag.metadata,
            )
        val ukjent = kjent.copy(vergeEllerFullmektig = kjent.vergeEllerFullmektig.copy(motpartsPersonident = null))
        listOf(listOf(ukjent), listOf(kjent, kjent)).forEach {
            assertEquals(
                "MOTTAKER_UAVKLART",
                assertThrows(BrevBatchSakFeil::class.java) {
                    BrevBatchSnapshot.map(sak, medVerger(it), batchRequest())
                }.kode,
            )
        }
        assertEquals(
            "Vergens navn",
            (
                BrevBatchSnapshot
                    .map(
                        sak,
                        medVerger(listOf(kjent)),
                        batchRequest(),
                    ).verge as no.nav.etterlatte.libs.common.person.Vergemaal
            ).navn,
        )
    }
}
