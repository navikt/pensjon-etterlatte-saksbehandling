package no.nav.etterlatte.behandling.sjekkliste

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class SjekklisteIntegrationTest(
    val dataSource: DataSource,
) {
    private val user =
        mockk<SaksbehandlerMedEnheterOgRoller>().apply {
            every { this@apply.name() } returns "Z123456"
        }

    private val behandlingService = mockk<BehandlingService>()
    private val oppgaveService = mockk<OppgaveService>()
    private lateinit var sjekklisteDao: SjekklisteDao
    private lateinit var sjekklisteService: SjekklisteService

    @BeforeAll
    fun setup() {
        nyKontekstMedBrukerOgDatabaseContext(user, DatabaseContextTest(dataSource))
        sjekklisteDao = SjekklisteDao(ConnectionAutoclosingTest(dataSource))
        sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService, oppgaveService)

        every { user.name() } returns "Sak B. Handlersen"
        every {
            oppgaveService.hentOppgaveUnderBehandling(any())
        } returns
            mockk<OppgaveIntern> {
                every { saksbehandler } returns OppgaveSaksbehandler(user.name(), "Sak B. Handlersen")
            }
    }

    @AfterAll
    fun afterAll() {
        clearAllMocks()
    }

    @Test
    fun `Opprett sjekkliste for BP`() {
        val behandling = foerstegangsbehandling(sakId = 33L)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        val opprettet = sjekklisteService.opprettSjekkliste(behandling.id)

        opprettet.id shouldBe behandling.id
        opprettet.versjon shouldBe 1

        opprettet.sjekklisteItems shouldHaveAtLeastSize 8
        opprettet.sjekklisteItems.forEach {
            it.avkrysset shouldBe false
            it.versjon shouldBe 1
        }
    }

    @Test
    fun `Opprett sjekkliste for OMS`() {
        val behandling = foerstegangsbehandling(sakId = 33L, sakType = SakType.OMSTILLINGSSTOENAD)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        val opprettet = sjekklisteService.opprettSjekkliste(behandling.id)

        opprettet.id shouldBe behandling.id
        opprettet.versjon shouldBe 1
        opprettet.sjekklisteItems.map { it.beskrivelse } shouldContainExactlyInAnyOrder sjekklisteItemsFoerstegangsbehandlingOMS

        opprettet.sjekklisteItems shouldHaveAtLeastSize 12
        opprettet.sjekklisteItems.forEach {
            it.avkrysset shouldBe false
            it.versjon shouldBe 1
        }
    }

    @Test
    fun `Hent eksisterende sjekkliste`() {
        val behandling = foerstegangsbehandling(sakId = 33L)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        sjekklisteService.opprettSjekkliste(behandling.id)

        val sjekkliste = sjekklisteService.hentSjekkliste(behandling.id)!!

        sjekkliste.id shouldBe behandling.id
        sjekkliste.versjon shouldBe 1
        sjekkliste.sjekklisteItems shouldHaveAtLeastSize 8
    }

    @Test
    fun `Oppdatere sjekkliste i db`() {
        val behandling = foerstegangsbehandling(sakId = 33L)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        sjekklisteService.opprettSjekkliste(behandling.id)

        sjekklisteService.oppdaterSjekkliste(
            behandling.id,
            OppdatertSjekkliste(
                kommentar = "noe rart her",
                kontonrRegistrert = "5555.5555.5555",
                onsketSkattetrekk = "30%",
                bekreftet = true,
                versjon = 3,
            ),
        )

        val sjekkliste = sjekklisteService.hentSjekkliste(behandling.id)!!

        assertAll(
            { sjekkliste.versjon shouldBe 2 },
            { sjekkliste.onsketSkattetrekk shouldBe "30%" },
            { sjekkliste.kommentar shouldBe "noe rart her" },
            { sjekkliste.bekreftet shouldBe true },
        )
    }

    @Test
    fun `Oppdatere et sjekkliste-element til avkrysset`() {
        val behandling = foerstegangsbehandling(sakId = 33L)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        val opprettet = sjekklisteService.opprettSjekkliste(behandling.id)

        val item = opprettet.sjekklisteItems.first()
        item.avkrysset shouldBe false

        with(
            sjekklisteService.oppdaterSjekklisteItem(
                behandling.id,
                item.id,
                oppdatering = OppdaterSjekklisteItem(avkrysset = true, versjon = 2),
            ),
        ) {
            this.id shouldBe item.id
            this.avkrysset shouldBe true
            this.versjon shouldBe 2
        }
    }
}
