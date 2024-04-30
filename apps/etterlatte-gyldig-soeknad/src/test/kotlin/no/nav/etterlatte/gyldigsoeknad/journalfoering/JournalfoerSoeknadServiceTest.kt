package no.nav.etterlatte.gyldigsoeknad.journalfoering

import io.mockk.mockk
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.sak.Sak
import org.junit.jupiter.api.Test
import pdf.PdfGenerator
import kotlin.random.Random

internal class JournalfoerSoeknadServiceTest {
    private val dokarkivKlient = mockk<DokarkivKlient>()
    private val pdfgenKlient = mockk<PdfGenerator>()

    private val sut = JournalfoerSoeknadService(dokarkivKlient, pdfgenKlient)

    @Test
    fun `BARNEPENSJON - Opprett journalpost`() {
        val soeknadId = Random.nextLong()
        val sak = Sak("ident", SakType.BARNEPENSJON, Random.nextLong(), Enheter.PORSGRUNN.enhetNr)
        val soeknad = getSoeknad("/soeknad/barnepensjon.json")

        sut.opprettJournalpost(soeknadId, sak, soeknad)
    }

    private fun getSoeknad(resource: String) = deserialize<InnsendtSoeknad>(javaClass.getResource(resource)!!.readText())
}
