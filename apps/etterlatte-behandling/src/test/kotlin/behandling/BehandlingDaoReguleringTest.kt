package no.nav.etterlatte.behandling

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoReguleringTest(
    val dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private lateinit var sakRepo: SakSkrivDao
    private lateinit var behandlingRepo: BehandlingDao

    @BeforeAll
    fun beforeAll() {
        sakRepo = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() })
        behandlingRepo =
            BehandlingDao(
                KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                ConnectionAutoclosingTest(dataSource),
            )
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    private fun hentMigrerbareStatuses() = BehandlingStatus.entries - BehandlingStatus.skalIkkeOmregnesVedGRegulering().toSet()

    @ParameterizedTest(
        name = "behandling med status {0} skal endres til aa vaere VILKAARSVURDERT",
    )
    @MethodSource("hentMigrerbareStatuses")
    fun `behandlinger som er beregnet maa beregnes paa nytt`(status: BehandlingStatus) {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val opprettBehandling =
            opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id, status = status)
        behandlingRepo.opprettBehandling(opprettBehandling)

        val trengerNyBeregning =
            behandlingRepo.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(Saker(listOf(sak)))

        with(behandlingRepo.hentBehandling(opprettBehandling.id)) {
            val expected = BehandlingStatus.TRYGDETID_OPPDATERT
            val actual = (this as Foerstegangsbehandling).status

            assertEquals(expected, actual)
        }
        assertEquals(listOf(opprettBehandling.id), trengerNyBeregning.filter { it.sakId == sak.id }.map { it.behandlingId })
    }

    @Test
    fun `Kun behandlinger som er sendt inn skal tilbakestilles`() {
        val relevantSak = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val ikkeRelevantSak = sakRepo.opprettSak("321", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val sakOgBehandling: List<OpprettBehandling> =
            listOf(relevantSak, ikkeRelevantSak).map {
                val opprettBehandling =
                    opprettBehandling(
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        sakId = it.id,
                        status = BehandlingStatus.BEREGNET,
                    )

                return behandlingRepo.opprettBehandling(opprettBehandling)
            }

        val trengerNyBeregning =
            behandlingRepo.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(Saker(listOf(relevantSak)))

        trengerNyBeregning.map { it.sakId } shouldContainExactly listOf(relevantSak.id)
        with(behandlingRepo.hentBehandling(sakOgBehandling.find { it.sakId == relevantSak.id }!!.id)) {
            val expected = BehandlingStatus.TRYGDETID_OPPDATERT
            val actual = (this as Foerstegangsbehandling).status

            assertEquals(expected, actual)
        }
        with(behandlingRepo.hentBehandling(sakOgBehandling.find { it.sakId == ikkeRelevantSak.id }!!.id)) {
            val expected = BehandlingStatus.BEREGNET
            val actual = (this as Foerstegangsbehandling).status

            assertEquals(expected, actual)
        }
    }

    private fun hentStatuser() = BehandlingStatus.skalIkkeOmregnesVedGRegulering()

    @ParameterizedTest(name = "behandling med status {0} skal fortsette aa ha samme status ved migrering")
    @MethodSource("hentStatuser")
    fun `irrelevante behandlinger skal ikke endre status ved migrering`(status: BehandlingStatus) {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val opprettBehandling =
            opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id, status = status)
        behandlingRepo.opprettBehandling(opprettBehandling)

        behandlingRepo.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(Saker(listOf(sak)))

        with(behandlingRepo.hentBehandling(opprettBehandling.id)) {
            val expected = status
            val actual = (this as Foerstegangsbehandling).status

            assertEquals(expected, actual)
        }
    }
}
