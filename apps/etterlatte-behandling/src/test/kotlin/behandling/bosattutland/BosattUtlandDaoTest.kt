package no.nav.etterlatte.behandling.bosattutland

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.utland.LandMedDokumenter
import no.nav.etterlatte.behandling.utland.MottattDokument
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class BosattUtlandDaoTest {
    private val dataSource = DatabaseExtension.dataSource
    private lateinit var bosattUtlandDao: BosattUtlandDao

    @BeforeAll
    fun beforeAll() {
        val connection = dataSource.connection
        bosattUtlandDao = BosattUtlandDao { connection }
    }

    @Test
    fun `kan lagre og hente bosattutland`() {
        val seder =
            listOf(
                LandMedDokumenter(
                    landIsoKode = "AFG",
                    dokumenter =
                        listOf(
                            MottattDokument(
                                dokumenttype = "P2000",
                                dato = LocalDate.now(),
                                kommentar = "kom",
                            ),
                        ),
                ),
            )
        val behandlingid = UUID.randomUUID()
        val bosattUtland = BosattUtland(behandlingid, "rinannumer", seder, seder)
        bosattUtlandDao.lagreBosattUtland(bosattUtland)
        val hentBosattUtland = bosattUtlandDao.hentBosattUtland(behandlingid)
        hentBosattUtland shouldBe bosattUtland

        val nyttrinanummer = "nyttrinanummer"
        bosattUtlandDao.lagreBosattUtland(bosattUtland.copy(rinanummer = nyttrinanummer))
        val oppdatertrinanummer = bosattUtlandDao.hentBosattUtland(bosattUtland.behandlingId)
        oppdatertrinanummer?.rinanummer shouldBe nyttrinanummer
    }
}
