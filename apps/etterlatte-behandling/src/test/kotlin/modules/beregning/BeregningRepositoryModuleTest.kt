package no.nav.etterlatte.modules.beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.config.modules.BeregningDatabaseKey.BRUK_EGEN_DATABASE_FOR_BEREGNING
import no.nav.etterlatte.config.modules.BeregningRepositoryModule
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.modules.beregning.anvendttrygdetid.AnvendtTrygdetidPeriodeUtrekning
import no.nav.etterlatte.modules.beregning.anvendttrygdetid.AnvendtTrygdetidRepository
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingRepository
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerRepository
import no.nav.etterlatte.modules.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.modules.beregning.klienter.BeregningCrudKlient
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

internal class BeregningRepositoryModuleTest {
    private val dataSource = mockk<DataSource>()
    private val crudKlient = mockk<BeregningCrudKlient>()

    @Test
    fun `manglende toggle bruker CRUD for alle repositories uten aa aapne lokal database`() {
        verifiserEksternDatabase(Miljoevariabler.httpClient(emptyMap()))
    }

    @Test
    fun `avslatt toggle bruker CRUD for alle repositories`() {
        verifiserEksternDatabase(Miljoevariabler.httpClient(mapOf(BRUK_EGEN_DATABASE_FOR_BEREGNING to "false")))
    }

    @Test
    fun `paaslatt toggle bruker lokal database for alle repositories uten aa opprette HTTP-klienten`() {
        val modul =
            BeregningRepositoryModule(dataSource, Miljoevariabler.httpClient(mapOf(BRUK_EGEN_DATABASE_FOR_BEREGNING to "true"))) {
                error("HTTP-klienten skal ikke opprettes ved lokal lagring")
            }
        modul.beregningRepository.shouldBeInstanceOf<BeregningRepository>()
        modul.beregningsGrunnlagRepository.shouldBeInstanceOf<BeregningsGrunnlagRepository>()
        modul.anvendtTrygdetidRepository.shouldBeInstanceOf<AnvendtTrygdetidRepository>()
        modul.avkortingRepository.shouldBeInstanceOf<AvkortingRepository>()
        modul.etteroppgjoerRepository.shouldBeInstanceOf<EtteroppgjoerRepository>()
        modul.sanksjonRepository.shouldBeInstanceOf<SanksjonRepository>()

        every { dataSource.connection } throws SQLException("Lokal databasefeil")
        val behandlingId = UUID.randomUUID()
        val operasjoner =
            listOf<() -> Any?>(
                { modul.beregningRepository.hent(behandlingId) },
                { modul.beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId) },
                {
                    modul.anvendtTrygdetidRepository.lagreAnvendtTrygdetid(
                        behandlingId,
                        AnvendtTrygdetidPeriodeUtrekning(emptyList(), emptyList()),
                    )
                },
                { modul.avkortingRepository.hentAvkorting(behandlingId) },
                { modul.etteroppgjoerRepository.hentEtteroppgjoerResultat(2024, behandlingId, behandlingId) },
                { modul.sanksjonRepository.hentSanksjon(behandlingId) },
            )
        operasjoner.forEach { operasjon ->
            assertThrows(SQLException::class.java) { operasjon() }
        }
        verify(exactly = 6) { dataSource.connection }
        verify { crudKlient wasNot Called }
    }

    @Test
    fun `ugyldig toggle stopper oppstart i stedet for aa velge feil database`() {
        assertThrows(IllegalArgumentException::class.java) {
            BeregningRepositoryModule(dataSource, Miljoevariabler.httpClient(mapOf(BRUK_EGEN_DATABASE_FOR_BEREGNING to "tru"))) {
                error("HTTP-klienten skal ikke opprettes")
            }
        }
        verify { dataSource wasNot Called }
    }

    @Test
    fun `feil fra CRUD gir ikke fallback til den lokale databasen`() {
        val behandlingId = UUID.randomUUID()
        every { crudKlient.hent(behandlingId) } throws IllegalStateException("CRUD-feil")
        val modul = BeregningRepositoryModule(dataSource, Miljoevariabler.httpClient(emptyMap())) { crudKlient }

        assertThrows(IllegalStateException::class.java) { modul.beregningRepository.hent(behandlingId) }
        verify { dataSource wasNot Called }
    }

    private fun verifiserEksternDatabase(env: Miljoevariabler) {
        var antallKlienter = 0
        val modul =
            BeregningRepositoryModule(dataSource, env) {
                antallKlienter++
                crudKlient
            }
        antallKlienter shouldBe 0
        modul.beregningRepository shouldBe crudKlient
        modul.beregningsGrunnlagRepository shouldBe crudKlient
        modul.anvendtTrygdetidRepository shouldBe crudKlient
        modul.avkortingRepository shouldBe crudKlient
        modul.etteroppgjoerRepository shouldBe crudKlient
        modul.sanksjonRepository shouldBe crudKlient
        antallKlienter shouldBe 1
        verify { dataSource wasNot Called }
    }
}
