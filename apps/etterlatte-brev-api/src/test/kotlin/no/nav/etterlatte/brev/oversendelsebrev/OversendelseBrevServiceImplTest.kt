package no.nav.etterlatte.brev.oversendelsebrev

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.DatabaseExtension
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OversendelseBrevServiceImplTest(
    dataSource: DataSource,
) {
    val sakId = randomSakId()
    private val behandlingId = UUID.randomUUID()
    private val saksbehandler = simpleSaksbehandler()
    private val brevdataFacade = mockk<BrevdataFacade>()
    private val adresseService = mockk<AdresseService>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val brevRepository = spyk(BrevRepository(dataSource))
    private val behandlingService = mockk<BehandlingService>()
    private val service =
        OversendelseBrevServiceImpl(
            brevRepository,
            mockk(),
            adresseService,
            behandlingService,
            grunnlagService,
        )

    @BeforeEach
    fun setUp() {
        coEvery { behandlingService.hentKlage(any(), any()) } returns klage()
        coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns brevData()
        coEvery { adresseService.hentMottakere(any(), any(), any()) } returns listOf(opprettMottaker())
        coEvery { behandlingService.hentVedtaksbehandlingKanRedigeres(any(), any()) } returns true
    }

    @Test
    fun `slett oversendelsesbrev`() {
        val serviceSpy =
            spyk(service).also {
                coEvery { it.hentSpraakOgPersonerISak(any(), any()) } returns
                    Pair(
                        Spraak.NN,
                        PersonerISak(
                            innsender = Innsender(Foedselsnummer(INNSENDER_FOEDSELSNUMMER.value)),
                            soeker = Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                            avdoede =
                                listOf(
                                    Avdoed(Foedselsnummer(AVDOED_FOEDSELSNUMMER.value), "DØD TESTPERSON", LocalDate.now().minusMonths(1)),
                                ),
                            verge = null,
                        ),
                    )
            }
        runBlocking { serviceSpy.opprettOversendelseBrev(behandlingId, saksbehandler) }
        val oversendelsesbrev = brevRepository.hentBrevForBehandling(behandlingId, Brevtype.OVERSENDELSE_KLAGE).single()

        runBlocking { serviceSpy.slettOversendelseBrev(behandlingId, saksbehandler) }

        brevRepository.hentBrevForBehandling(behandlingId, Brevtype.OVERSENDELSE_KLAGE) shouldBe emptyList()
        verify { brevRepository.settBrevSlettet(oversendelsesbrev.id, any()) }
    }

    private fun klage(): Klage =
        Klage(
            behandlingId,
            Sak("ident", SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, null),
            Tidspunkt.now(),
            KlageStatus.OPPRETTET,
            kabalResultat = null,
            kabalStatus = null,
            formkrav = null,
            innkommendeDokument = null,
            resultat = null,
            utfall = null,
            aarsakTilAvbrytelse = null,
            initieltUtfall = null,
        )

    private fun brevData() =
        GenerellBrevData(
            sak = Sak("11057523044", SakType.OMSTILLINGSSTOENAD, sakId, Enheter.PORSGRUNN.enhetNr, null, null),
            personerISak =
                PersonerISak(
                    Innsender(Foedselsnummer("11057523044")),
                    Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer("12345612345")),
                    listOf(Avdoed(Foedselsnummer(""), "DØD TESTPERSON", LocalDate.now().minusMonths(1))),
                    verge = null,
                ),
            behandlingId = UUID.randomUUID(),
            forenkletVedtak =
                ForenkletVedtak(
                    1,
                    VedtakStatus.FATTET_VEDTAK,
                    VedtakType.TILBAKEKREVING,
                    Enheter.PORSGRUNN.enhetNr,
                    "saksbehandler",
                    attestantIdent = null,
                    vedtaksdato = null,
                    virkningstidspunkt = YearMonth.now(),
                    klage = klage(),
                ),
            spraak = Spraak.NB,
            systemkilde = Vedtaksloesning.GJENNY,
        )

    private fun opprettMottaker() =
        Mottaker(
            UUID.randomUUID(),
            "Rød Blanding",
            foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
            orgnummer = null,
            adresse =
                Adresse(
                    adresseType = "NORSKPOSTADRESSE",
                    adresselinje1 = "Testgaten 13",
                    postnummer = "1234",
                    poststed = "OSLO",
                    land = "Norge",
                    landkode = "NOR",
                ),
        )
}
