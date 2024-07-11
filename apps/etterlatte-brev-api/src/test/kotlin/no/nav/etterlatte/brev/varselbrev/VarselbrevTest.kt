package no.nav.etterlatte.brev.varselbrev

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.DatabaseExtension
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.RedigerbartVedleggHenter
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Telefonnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VarselbrevTest(
    datasource: DataSource,
) {
    private lateinit var service: VarselbrevService

    private val brevRepository = BrevRepository(datasource)

    val sak = Sak("ident1", SakType.BARNEPENSJON, 1L, Enheter.STEINKJER.enhetNr)

    @BeforeEach
    fun start() {
        val adresseService =
            mockk<AdresseService>().also {
                coEvery { it.hentMottakerAdresse(any(), any()) } returns tomMottaker(SOEKER_FOEDSELSNUMMER)
                coEvery { it.hentAvsender(any()) } returns
                    Avsender(
                        kontor = "",
                        telefonnummer = Telefonnummer("12345678"),
                        saksbehandler = null,
                        attestant = null,
                    )
            }
        val brevdataFacade =
            mockk<BrevdataFacade>().also {
                coEvery {
                    it.hentGenerellBrevData(sak.id, any(), any(), any())
                } returns
                    mockk<GenerellBrevData>().also {
                        every { it.vedtakstype() } returns ""
                        every { it.spraak } returns Spraak.NN
                        every { it.loependeIPesys() } returns false
                        every { it.erForeldreloes() } returns false
                        every { it.sak } returns Sak("", SakType.BARNEPENSJON, 1L, "")
                        every { it.forenkletVedtak } returns null
                        every { it.personerISak } returns
                            PersonerISak(
                                null,
                                Soeker("", "", "", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                                listOf(),
                                null,
                            )
                        every { it.utlandstilknytning } returns null
                        every { it.revurderingsaarsak } returns null
                        every { it.systemkilde } returns Vedtaksloesning.GJENNY
                        every { it.behandlingId } returns null
                    }
            }
        val brevbaker =
            mockk<BrevbakerService>().also {
                coEvery { it.hentRedigerbarTekstFraBrevbakeren(any()) } returns
                    Slate(
                        emptyList(),
                    )
            }
        val redigerbartVedleggHenter =
            mockk<RedigerbartVedleggHenter>().also {
                coEvery {
                    it.hentInitiellPayloadVedlegg(
                        any(),
                        any(),
                        any(),
                    )
                } returns listOf()
            }
        val behandlingService = mockk<BehandlingService>().also { coEvery { it.hentSak(sak.id, any()) } returns sak }
        val brevoppretter =
            Brevoppretter(
                adresseService,
                brevRepository,
                brevdataFacade,
                behandlingService,
                brevbaker,
                redigerbartVedleggHenter,
            )
        val pdfGenerator = mockk<PDFGenerator>()
        service = VarselbrevService(brevRepository, brevoppretter, behandlingService, pdfGenerator, mockk())
    }

    @Test
    fun `lager varselbrev`() {
        val behandling = UUID.randomUUID()

        assertEquals(listOf<Brev>(), service.hentVarselbrev(behandling))

        val varselbrev =
            runBlocking {
                service.opprettVarselbrev(
                    sak.id,
                    behandling,
                    BrukerTokenInfo.of(
                        "",
                        "bruker1",
                        "",
                        "",
                        null,
                        null,
                    ),
                )
            }

        val henta = service.hentVarselbrev(behandling)
        assertEquals(varselbrev, henta.first())
    }
}
