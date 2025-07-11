package no.nav.etterlatte.brev.varselbrev

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.DatabaseExtension
import no.nav.etterlatte.brev.InnholdTilRedigerbartBrevHenter
import no.nav.etterlatte.brev.RedigerbartVedleggHenter
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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

    val sak = Sak("ident1", SakType.BARNEPENSJON, sakId1, Enheter.STEINKJER.enhetNr, null, null)

    @BeforeEach
    fun start() {
        val adresseService =
            mockk<AdresseService>().also {
                coEvery { it.hentMottakere(any(), any(), any()) } returns
                    listOf(
                        Mottaker(
                            id = UUID.fromString("d762e98d-514d-4bb5-a6b5-a3fbf4d65887"),
                            navn = "Navn Navnesen",
                            foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                            orgnummer = null,
                            adresse =
                                Adresse(
                                    adresseType = AdresseType.VEGADRESSE.name,
                                    adresselinje1 = "Adresse1",
                                    landkode = "NO",
                                    land = "Norge",
                                ),
                        ),
                    )

                coEvery { it.hentAvsender(any(), any()) } returns
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
                        every { it.spraak } returns Spraak.NN
                        every { it.sak } returns Sak("", SakType.BARNEPENSJON, sakId1, Enheter.defaultEnhet.enhetNr, null, null)
                        every { it.forenkletVedtak } returns null
                        every { it.personerISak } returns
                            PersonerISak(
                                null,
                                Soeker("", "", "", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                                emptyList(),
                                null,
                            )
                        every { it.utlandstilknytning } returns null
                        every { it.revurderingsaarsak } returns null
                        every { it.systemkilde } returns Vedtaksloesning.GJENNY
                        every { it.behandlingId } returns null
                        every { it.prosesstype } returns null
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
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns emptyList()
            }
        val behandlingService =
            mockk<BehandlingService>().also {
                coEvery { it.hentSak(sak.id, any()) } returns sak
                coEvery { it.hentBehandling(any(), any()) } returns
                    mockk<DetaljertBehandling> {
                        every { id } returns UUID.randomUUID()
                        every { utlandstilknytning } returns
                            Utlandstilknytning(
                                UtlandstilknytningType.NASJONAL,
                                Grunnlagsopplysning.Saksbehandler(ident = "Z123", tidspunkt = Tidspunkt.now()),
                                "begrunnelse",
                            )
                        every { revurderingsaarsak } returns Revurderingaarsak.AKTIVITETSPLIKT
                    }
            }

        val behandlingklient =
            mockk<BehandlingKlient>().also {
                coEvery { it.hentGrunnlag(any(), any()) } returns mockk()
            }

        val innholdTilRedigerbartBrevHenter =
            InnholdTilRedigerbartBrevHenter(brevdataFacade, brevbaker, adresseService, redigerbartVedleggHenter)

        val brevoppretter =
            Brevoppretter(
                adresseService,
                brevRepository,
                innholdTilRedigerbartBrevHenter,
            )
        val pdfGenerator = mockk<PDFGenerator>()
        service = VarselbrevService(brevRepository, brevoppretter, behandlingService, pdfGenerator, mockk(), behandlingklient)
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
                    systembruker(),
                )
            }

        val henta = service.hentVarselbrev(behandling)
        assertEquals(varselbrev, henta.first())
    }
}
