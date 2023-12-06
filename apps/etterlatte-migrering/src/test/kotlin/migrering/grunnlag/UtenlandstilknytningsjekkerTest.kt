package migrering.grunnlag

import grunnlag.Bostedsland
import grunnlag.VurdertBostedsland
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.migrering.grunnlag.GrunnlagKlient
import no.nav.etterlatte.migrering.grunnlag.Utenlandstilknytningsjekker
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UtenlandstilknytningsjekkerTest {
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val sjekker = Utenlandstilknytningsjekker(grunnlagKlient)

    @Test
    fun `bosatt i Norge enhet Steinkjer folketrygdberegnet gir nasjonal`() {
        coEvery { grunnlagKlient.hentBostedsland(any()) } returns VurdertBostedsland(Bostedsland.NOR.name)
        val request = lagRequest(Enheter.STEINKJER).also { every { it.erFolketrygdberegnet() } returns true }
        val resultat = sjekker.finnUtenlandstilknytning(request)
        assertEquals(UtlandstilknytningType.NASJONAL, resultat)
    }

    @Test
    fun `bosatt i Norge enhet Steinkjer men ikke folketrygdberegnet gir null`() {
        coEvery { grunnlagKlient.hentBostedsland(any()) } returns VurdertBostedsland(Bostedsland.NOR.name)
        val request = lagRequest(Enheter.STEINKJER).also { every { it.erFolketrygdberegnet() } returns false }
        val resultat = sjekker.finnUtenlandstilknytning(request)
        assertNull(resultat)
    }

    @Test
    fun `bosatt utenlands enhet Steinkjer gir null`() {
        coEvery { grunnlagKlient.hentBostedsland(any()) } returns VurdertBostedsland(Bostedsland.XUK.name)
        val resultat = sjekker.finnUtenlandstilknytning(lagRequest(Enheter.STEINKJER))
        assertNull(resultat)
    }

    @Test
    fun `bosatt i Norge enhet Aalesund utland gir utlandstilsnitt`() {
        coEvery { grunnlagKlient.hentBostedsland(any()) } returns VurdertBostedsland(Bostedsland.NOR.name)
        val resultat = sjekker.finnUtenlandstilknytning(lagRequest(Enheter.AALESUND_UTLAND))
        assertEquals(UtlandstilknytningType.UTLANDSTILSNITT, resultat)
    }

    @Test
    fun `bosatt utenlands enhet Aalesund utland gir null`() {
        coEvery { grunnlagKlient.hentBostedsland(any()) } returns VurdertBostedsland(Bostedsland.XUK.name)
        val resultat = sjekker.finnUtenlandstilknytning(lagRequest(Enheter.AALESUND_UTLAND))
        assertNull(resultat)
    }

    @Test
    fun `bosatt utland enhet utland gir bosatt_utland`() {
        coEvery { grunnlagKlient.hentBostedsland(any()) } returns VurdertBostedsland(Bostedsland.XUK.name)
        val resultat = sjekker.finnUtenlandstilknytning(lagRequest(Enheter.UTLAND))
        assertEquals(UtlandstilknytningType.BOSATT_UTLAND, resultat)
    }

    @Test
    fun `bosatt i Norge enhet utland gir null`() {
        coEvery { grunnlagKlient.hentBostedsland(any()) } returns VurdertBostedsland(Bostedsland.NOR.name)
        val resultat = sjekker.finnUtenlandstilknytning(lagRequest(Enheter.UTLAND))
        assertNull(resultat)
    }

    private fun lagRequest(enhet: Enheter) =
        mockk<MigreringRequest>()
            .also { every { it.enhet } returns Enhet(enhet.enhetNr) }
            .also { every { it.soeker } returns SOEKER_FOEDSELSNUMMER }
}
