package no.nav.etterlatte.brev.varselbrev

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.RedigerbartVedleggHenter
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.opprettInMemoryDatabase
import no.nav.etterlatte.token.Systembruker
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

class VarselbrevTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var datasource: DataSource
    private lateinit var service: VarselbrevService

    val sak = Sak("ident1", SakType.BARNEPENSJON, 1L, Enheter.STEINKJER.enhetNr)

    @BeforeEach
    fun start() {
        datasource = opprettInMemoryDatabase(postgreSQLContainer).dataSource

        val brevRepository = BrevRepository(datasource)
        val adresseService =
            mockk<AdresseService>().also {
                coEvery { it.hentMottakerAdresse(any()) } returns Mottaker.tom(SOEKER_FOEDSELSNUMMER)
            }
        val brevdataFacade =
            mockk<BrevdataFacade>().also {
                coEvery {
                    it.hentGenerellBrevData(
                        sak.id,
                        any(),
                        any(),
                    )
                } returns
                    mockk<GenerellBrevData>().also {
                        every { it.vedtakstype() } returns ""
                        every { it.spraak } returns Spraak.NN
                        every { it.erMigrering() } returns false
                        every { it.sak } returns Sak("", SakType.BARNEPENSJON, 1L, "")
                        every { it.forenkletVedtak } returns null
                        every { it.personerISak } returns
                            PersonerISak(
                                null,
                                Soeker("", "", "", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                                listOf(),
                                null,
                            )
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
                    )
                } returns listOf()
            }
        val brevoppretter =
            Brevoppretter(
                adresseService,
                brevRepository,
                brevdataFacade,
                brevbaker,
                redigerbartVedleggHenter,
                mockk(),
            )
        val behandlingKlient = mockk<BehandlingKlient>().also { coEvery { it.hentSak(sak.id, any()) } returns sak }
        val pdfGenerator = mockk<PDFGenerator>()
        service = VarselbrevService(brevRepository, brevoppretter, behandlingKlient, pdfGenerator)
    }

    @AfterEach
    fun stop() = postgreSQLContainer.stop()

    @Test
    fun `lager varselbrev`() {
        val behandling = UUID.randomUUID()

        assertEquals(listOf<Brev>(), service.hentVarselbrev(behandling))

        val varselbrev =
            runBlocking {
                service.opprettVarselbrev(
                    sak.id,
                    behandling,
                    Systembruker.brev,
                )
            }

        val henta = service.hentVarselbrev(behandling)
        assertEquals(varselbrev, henta.first())
    }
}
