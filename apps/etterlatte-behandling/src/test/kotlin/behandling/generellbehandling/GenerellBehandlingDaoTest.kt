package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.generellbehandling.DokumentMedSendtDato
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class GenerellBehandlingDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var dao: GenerellBehandlingDao

    @BeforeAll
    fun beforeAll() {
        dao = GenerellBehandlingDao(ConnectionAutoclosingTest(dataSource))
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE generellbehandling CASCADE;").execute()
        }
    }

    @Test
    fun `opprette kun med type`() {
        val generellBehandlingUtland = GenerellBehandling.opprettUtland(sakId1, null)
        val hentetGenBehandling = dao.opprettGenerellbehandling(generellBehandlingUtland)

        Assertions.assertEquals(generellBehandlingUtland.id, hentetGenBehandling.id)
        Assertions.assertEquals(generellBehandlingUtland.innhold, hentetGenBehandling.innhold)
    }

    @Test
    fun `Assert skal catche at man oppretter med feil type`() {
        assertThrows<IllegalArgumentException> {
            GenerellBehandling(
                UUID.randomUUID(),
                sakId1,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                Innhold.Annen("content"),
                status = GenerellBehandling.Status.OPPRETTET,
            )
        }
    }

    @Test
    fun `Kan opprette og hente en generell behandling utland`() {
        val kravpakkeUtland =
            GenerellBehandling(
                UUID.randomUUID(),
                sakId1,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                Innhold.KravpakkeUtland(
                    listOf("AFG"),
                    listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                    "2grwg2",
                    "rita",
                ),
                status = GenerellBehandling.Status.OPPRETTET,
            )
        val hentetGenBehandling = dao.opprettGenerellbehandling(kravpakkeUtland)

        Assertions.assertEquals(kravpakkeUtland.id, hentetGenBehandling.id)
        Assertions.assertEquals(kravpakkeUtland.innhold, hentetGenBehandling.innhold)
    }

    @Test
    fun `Kan hente for sak`() {
        val sakId = sakId1
        val kravpakkeUtland =
            GenerellBehandling(
                UUID.randomUUID(),
                sakId1,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                Innhold.KravpakkeUtland(
                    listOf("AFG"),
                    listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                    "2grwg2",
                    "rita",
                ),
                status = GenerellBehandling.Status.OPPRETTET,
            )
        val annengenerebehandling =
            GenerellBehandling(
                UUID.randomUUID(),
                sakId1,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.ANNEN,
                Innhold.Annen("vlabla"),
                status = GenerellBehandling.Status.OPPRETTET,
            )

        dao.opprettGenerellbehandling(kravpakkeUtland)
        dao.opprettGenerellbehandling(annengenerebehandling)
        val hentetGenBehandling = dao.hentGenerellBehandlingForSak(sakId)
        Assertions.assertEquals(2, hentetGenBehandling.size)
        val generellBehandling = hentetGenBehandling.single { it.innhold is Innhold.KravpakkeUtland }
        Assertions.assertEquals(kravpakkeUtland.id, generellBehandling.id)
        Assertions.assertEquals(kravpakkeUtland.innhold, generellBehandling.innhold)
    }
}
