package behandling.klage

import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.FormkravMedBeslutter
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class KlageDaoImplTest {
    private val dataSource = DatabaseExtension.dataSource
    private lateinit var sakRepo: SakDao
    private lateinit var klageDao: KlageDaoImpl

    @BeforeAll
    fun setup() {
        val connection = dataSource.connection
        sakRepo = SakDao { connection }
        klageDao = KlageDaoImpl { connection }
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.prepareStatement("""TRUNCATE TABLE klage""")
            .executeUpdate()
        dataSource.connection.prepareStatement("""TRUNCATE TABLE sak CASCADE """)
            .executeUpdate()
    }

    @Test
    fun `lagreKlage oppdaterer status og formkrav hvis klagen allerede eksisterer`() {
        val sak = sakRepo.opprettSak(fnr = "en bruker", type = SakType.BARNEPENSJON, enhet = "1337")
        val klage = Klage.ny(sak)
        klageDao.lagreKlage(klage)

        val foersteHentedeKlage = klageDao.hentKlage(klage.id)
        Assertions.assertEquals(klage, foersteHentedeKlage)

        val formkrav =
            FormkravMedBeslutter(
                Formkrav(
                    vedtaketKlagenGjelder =
                        VedtaketKlagenGjelder(
                            id = "",
                            behandlingId = "",
                            datoAttestert = null,
                            vedtakType = null,
                        ),
                    erKlagerPartISaken = JaNei.JA,
                    erKlagenSignert = JaNei.JA,
                    gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                    erKlagenFramsattInnenFrist = JaNei.JA,
                    erFormkraveneOppfylt = JaNei.JA,
                ),
                Grunnlagsopplysning.Saksbehandler.create("en saksbehandler"),
            )

        val oppdatertKlage =
            klage.copy(
                status = KlageStatus.FORMKRAV_OPPFYLT,
                formkrav = formkrav,
            )
        klageDao.lagreKlage(oppdatertKlage)

        val hentetKlage = klageDao.hentKlage(oppdatertKlage.id)

        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, hentetKlage?.status)
        Assertions.assertEquals(formkrav, hentetKlage?.formkrav)
    }

    @Test
    fun `lagreKlage oppdaterer ikke opprettet tidspunkt eller saken hvis klagen allerede eksisterer`() {
        val sak = sakRepo.opprettSak(fnr = "en bruker", type = SakType.BARNEPENSJON, enhet = "1337")
        val sak2 = sakRepo.opprettSak(fnr = "en annen bruker", type = SakType.OMSTILLINGSSTOENAD, enhet = "3137")
        val klage = Klage.ny(sak)
        klageDao.lagreKlage(klage)

        val foersteHentedeKlage = klageDao.hentKlage(klage.id)
        Assertions.assertEquals(klage, foersteHentedeKlage)

        val klageMedEndretSakOgOpprettet = klage.copy(sak = sak2, opprettet = klage.opprettet.plus(2, ChronoUnit.HOURS))
        klageDao.lagreKlage(klageMedEndretSakOgOpprettet)

        val andreHentedeKlage = klageDao.hentKlage(klageMedEndretSakOgOpprettet.id)
        Assertions.assertEquals(foersteHentedeKlage, andreHentedeKlage)
        Assertions.assertNotEquals(foersteHentedeKlage?.sak, klageMedEndretSakOgOpprettet.sak)
        Assertions.assertNotEquals(foersteHentedeKlage?.opprettet, klageMedEndretSakOgOpprettet.opprettet)
    }
}
