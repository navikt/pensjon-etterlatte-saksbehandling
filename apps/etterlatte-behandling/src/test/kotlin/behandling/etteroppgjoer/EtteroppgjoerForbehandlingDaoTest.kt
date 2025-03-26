package behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class EtteroppgjoerForbehandlingDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var etteroppgjoerForbehandlingDao: EtteroppgjoerForbehandlingDao

    private lateinit var sak: Sak

    @BeforeAll
    fun setup() {
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        etteroppgjoerForbehandlingDao = EtteroppgjoerForbehandlingDao(ConnectionAutoclosingTest(dataSource))

        nyKontekstMedBrukerOgDatabase(
            mockk<User>().also { every { it.name() } returns this::class.java.simpleName },
            dataSource,
        )
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.use {
            it.prepareStatement("""TRUNCATE TABLE etteroppgjoer_behandling CASCADE""").executeUpdate()
            it.prepareStatement("""TRUNCATE TABLE sak CASCADE """).executeUpdate()
        }
        sak =
            sakSkrivDao.opprettSak(
                fnr = "en bruker",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
    }

    @Test
    fun `lagre og oppdatere forbehandling`() {
        val ny =
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                status = "status",
                hendelseId = UUID.randomUUID(),
                aar = 2024,
                opprettet = Tidspunkt.now(),
                sak = sak,
                brevId = null,
            )

        etteroppgjoerForbehandlingDao.lagreForbehandling(ny)
        val lagret = etteroppgjoerForbehandlingDao.hentForbehandling(ny.id)
        with(lagret!!) {
            id shouldBe ny.id
            status shouldBe ny.status
            aar shouldBe ny.aar
            opprettet shouldBe ny.opprettet
        }
    }

    @Test
    fun `lagre og hente pensjonsgivendeInntekt`() {
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()

        // negative returnere null hvis tomt
        etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId) shouldBe null

        etteroppgjoerForbehandlingDao.lagrePensjonsgivendeInntekt(
            PensjonsgivendeInntektFraSkatt.stub(inntektsaar),
            forbehandlingId,
        )

        with(etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId)!!) {
            this.inntektsaar shouldBe inntektsaar
            inntekter shouldBe PensjonsgivendeInntektFraSkatt.stub(inntektsaar).inntekter
        }
    }

    @Test
    fun `lagre og hente aInntekt`() {
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()

        // negative returnere null hvis tomt
        etteroppgjoerForbehandlingDao.hentAInntekt(forbehandlingId) shouldBe null

        etteroppgjoerForbehandlingDao.lagreAInntekt(
            AInntekt.stub(inntektsaar),
            forbehandlingId,
        )

        with(etteroppgjoerForbehandlingDao.hentAInntekt(forbehandlingId)!!) {
            inntektsaar shouldBe inntektsaar
            inntektsmaaneder shouldBe AInntekt.stub(inntektsaar).inntektsmaaneder
        }
    }
}
