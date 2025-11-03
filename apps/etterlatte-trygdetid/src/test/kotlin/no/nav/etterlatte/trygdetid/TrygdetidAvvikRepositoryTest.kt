package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.FremtidigTrygdetid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Period
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrygdetidAvvikRepositoryTest(
    val dataSource: DataSource,
) {
    private val behandlingId = UUID.randomUUID()
    private val trygdetidId = UUID.randomUUID()
    private val sakId = randomSakId()

    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val repository = TrygdetidAvvikRepository(dataSource)

    @BeforeEach
    fun `sette inn et ikke sjekket avvik`() {
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    INSERT INTO trygdetid_avvik (trygdetid_id, behandling_id, sak_id)
                    VALUES (:trygdetidId, :behandlingId, :sakId)
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "trygdetidId" to trygdetidId,
                        "behandlingId" to behandlingId,
                        "sakId" to sakId.sakId,
                    ),
            ).let { query ->
                session.run(query.asUpdate)
            }
        }
    }

    @AfterEach
    fun `rydde opp`() {
        dbExtension.resetDb()
    }

    @Test
    fun `henter og oppdaterer trygdetider med potensielle avvik riktig`() {
        repository.hentIkkeSjekketTrygdetid() shouldNotBe null
        val trygdetid =
            DetaljertBeregnetTrygdetidResultat(
                faktiskTrygdetidNorge =
                    FaktiskTrygdetid(
                        periode = Period.of(22, 1, 0),
                        antallMaaneder = 22 * 12 + 1,
                    ),
                faktiskTrygdetidTeoretisk =
                    FaktiskTrygdetid(
                        periode = Period.of(22, 1, 0),
                        antallMaaneder = 22 * 12 + 1,
                    ),
                fremtidigTrygdetidNorge =
                    FremtidigTrygdetid(
                        periode = Period.of(25, 0, 0),
                        antallMaaneder = 25 * 12,
                        opptjeningstidIMaaneder = 22 * 12 + 1,
                        mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                    ),
                fremtidigTrygdetidTeoretisk =
                    FremtidigTrygdetid(
                        periode = Period.of(25, 0, 0),
                        antallMaaneder = 25 * 12,
                        opptjeningstidIMaaneder = 22 * 12 + 1,
                        mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                    ),
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = 40,
                prorataBroek = null,
                overstyrt = false,
                yrkesskade = false,
                beregnetSamletTrygdetidNorge = 0,
                overstyrtBegrunnelse = "",
            )
        val avvikendeTrygdetid =
            trygdetid.copy(
                samletTrygdetidNorge = 39,
            )
        val avvik =
            TrygdetidAvvik(
                gammelBeregnet = trygdetid,
                nyBeregnet = avvikendeTrygdetid,
            )
        repository.oppdaterAvvikResultat(behandlingId, trygdetidId, avvik)
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """select * from trygdetid_avvik where behandling_id = :behandlingId and trygdetid_id = :trygdetidId""",
                paramMap =
                    mapOf(
                        "behandlingId" to behandlingId,
                        "trygdetidId" to trygdetidId,
                    ),
            ).let { query ->
                session.run(
                    query
                        .map { row ->
                            row.string("status") shouldBe "SJEKKET_HAR_AVVIK"
                            row.stringOrNull("avvik")?.let { objectMapper.readValue<TrygdetidAvvik>(it) }
                        }.asSingle,
                )
            }
        }
        repository.oppdaterAvvikResultat(behandlingId, trygdetidId, null)
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """select * from trygdetid_avvik where behandling_id = :behandlingId and trygdetid_id = :trygdetidId""",
                paramMap =
                    mapOf(
                        "behandlingId" to behandlingId,
                        "trygdetidId" to trygdetidId,
                    ),
            ).let { query ->
                session.run(
                    query
                        .map {
                            it.string("status") shouldBe "SJEKKET_INGEN_AVVIK"
                            it.stringOrNull("avvik") shouldBe null
                        }.asSingle,
                )
            }
        }
        repository.hentIkkeSjekketTrygdetid() shouldBe null
    }
}
