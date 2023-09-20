package no.nav.etterlatte.egenansatt

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.TilgangService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Connection
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EgenAnsattServiceTest {
    val sikkerLogg: Logger = sikkerlogger()

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var dataSource: DataSource
    private lateinit var sakRepo: SakDao
    private lateinit var sakService: SakService
    private lateinit var egenAnsattService: EgenAnsattService

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }

        val pdlKlient = mockk<PdlKlient>()
        val norg2Klient = mockk<Norg2Klient>()
        val featureToggleService = mockk<FeatureToggleService>()
        val tilgangService = mockk<TilgangService>()
        val skjermingKlient = mockk<SkjermingKlient>()
        val connection = dataSource.connection
        sakRepo = SakDao { connection }
        sakService =
            spyk(
                RealSakService(sakRepo, pdlKlient, norg2Klient, featureToggleService, tilgangService, skjermingKlient),
            )
        egenAnsattService = EgenAnsattService(sakService, sikkerLogg)

        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        every { pdlKlient.hentGeografiskTilknytning(any(), any()) } returns GeografiskTilknytning(kommune = "0301")
        every {
            norg2Klient.hentEnheterForOmraade("EYB", "0301")
        } returns listOf(ArbeidsFordelingEnhet("NAV Familie- og pensjonsytelser Steinkjer", "4817"))

        every { featureToggleService.isEnabled(any(), any()) } returns false
    }

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                mockk(),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(
                        gjenbruk: Boolean,
                        block: () -> T,
                    ): T {
                        return block()
                    }
                },
            ),
        )
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun sjekkAtSettingAvSkjermingFungererEtterOpprettelseAvSak() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        sakService.finnEllerOpprettSak(fnr, SakType.BARNEPENSJON, enhet = Enheter.EGNE_ANSATTE.enhetNr)
        val fnr2 = Folkeregisteridentifikator.of("19078504903").value
        sakService.finnEllerOpprettSak(fnr2, SakType.BARNEPENSJON, enhet = Enheter.EGNE_ANSATTE.enhetNr)

        assertNotNull(sakService.finnSak(fnr, SakType.BARNEPENSJON))
        assertNotNull(sakService.finnSak(fnr2, SakType.BARNEPENSJON))

        val egenAnsattSkjermet = EgenAnsattSkjermet(fnr, Tidspunkt.now(), true)
        egenAnsattService.haandterSkjerming(egenAnsattSkjermet)

        verify { sakService.markerSakerMedSkjerming(any(), any()) }
    }
}
