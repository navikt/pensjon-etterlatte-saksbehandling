package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningRepositoryTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var beregningRepository: BeregningRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        val ds =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }

        beregningRepository = BeregningRepository(ds)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `lagre() skal returnere samme data som faktisk ble lagret`() {
        val beregning = beregning()
        val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)

        assertEquals(beregning, lagretBeregning)
    }

    @Test
    fun `det som hentes ut skal vaere likt det som originalt ble lagret`() {
        val beregningLagret = beregning()
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)

        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(beregningLagret, beregningHentet)
    }

    @Test
    fun `skal oppdatere og eller lagre beregning`() {
        val beregningLagret = beregning()

        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)
        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(beregningLagret, beregningHentet)

        val nyBeregning = beregning(beregningLagret.behandlingId, YearMonth.of(2022, 2))

        beregningRepository.lagreEllerOppdaterBeregning(nyBeregning)
        val beregningHentetNy = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(nyBeregning, beregningHentetNy)
    }

    private fun beregning(
        behandlingId: UUID = randomUUID(),
        datoFOM: YearMonth = YearMonth.of(2021, 2),
    ) = Beregning(
        beregningId = randomUUID(),
        behandlingId = behandlingId,
        type = Beregningstype.BP,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = no.nav.etterlatte.libs.common.grunnlag.Metadata(1, 1),
        beregningsperioder =
            listOf(
                Beregningsperiode(
                    datoFOM = datoFOM,
                    datoTOM = null,
                    utbetaltBeloep = 3000,
                    soeskenFlokk = listOf(FNR_1),
                    institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                    grunnbelopMnd = 10_000,
                    grunnbelop = 100_000,
                    trygdetid = 40,
                    regelResultat = mapOf("regel" to "resultat").toObjectNode(),
                    regelVersjon = "1",
                    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
                ),
            ),
    )
}
