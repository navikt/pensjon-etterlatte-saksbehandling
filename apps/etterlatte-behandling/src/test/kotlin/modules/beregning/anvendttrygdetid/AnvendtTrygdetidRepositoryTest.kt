package no.nav.etterlatte.modules.beregning.anvendttrygdetid

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.config.modules.DaoModule
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertResultat
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AnvendtTrygdetidRepositoryTest(
    private val dataSource: DataSource,
) {
    private val repository =
        DaoModule(ConnectionAutoclosingImpl(dataSource), dataSource).anvendtTrygdetidRepository

    @Test
    fun `lagrer utregningen og kombinerte perioder som JSON`() {
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 12, 31)
        val trygdetid = AnvendtTrygdetid(BeregningsMetode.NASJONAL, Beregningstall(40), "test-ident")
        val regel =
            finnFaktumIGrunnlag<AnvendtTrygdetid, AnvendtTrygdetid, AnvendtTrygdetid>(
                gjelderFra = fom,
                beskrivelse = "Trygdetid",
                finnFaktum = { FaktumNode(it, "test", "Trygdetid") },
                finnFelt = { it },
            )
        val periode = RegelPeriode(fom, tom)
        val utregning =
            AnvendtTrygdetidPeriodeUtrekning(
                anvendt = listOf(GrunnlagMedPeriode(listOf(trygdetid), fom, tom)),
                utrekning = listOf(PeriodisertResultat(periode, regel.anvend(trygdetid, periode), "1")),
            )

        repository.lagreAnvendtTrygdetid(behandlingId, utregning)

        val lagret =
            dataSource.transaction { tx ->
                tx.run(
                    queryOf(
                        "SELECT * FROM anvendt_trygdetid WHERE behandling_id = ?",
                        behandlingId,
                    ).map {
                        objectMapper.readTree(it.string("foer_kombinering")) to
                            objectMapper.readTree(it.string("etter_kombinering"))
                    }.asSingle,
                )
            }

        assertEquals(objectMapper.readTree(utregning.utrekning.toJson()), lagret?.first)
        assertEquals(objectMapper.readTree(utregning.anvendt.toJson()), lagret?.second)
    }

    @Test
    fun `beholder tidligere utregninger for samme behandling`() {
        val behandlingId = UUID.randomUUID()
        val utregning = AnvendtTrygdetidPeriodeUtrekning(emptyList(), emptyList())

        repository.lagreAnvendtTrygdetid(behandlingId, utregning)
        repository.lagreAnvendtTrygdetid(behandlingId, utregning)

        val antall =
            dataSource.transaction { tx ->
                tx.run(
                    queryOf(
                        "SELECT count(*) AS antall FROM anvendt_trygdetid WHERE behandling_id = ?",
                        behandlingId,
                    ).map { it.int("antall") }.asSingle,
                )
            }

        antall shouldBe 2
    }
}
