package nav.no.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.plugins.NotFoundException
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import nav.no.etterlatte.model.Beregning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import java.util.*
import javax.sql.DataSource

interface BeregningRepository {
    fun lagre(beregning: Beregning): Beregning
    fun hent(behandlingId: UUID): Beregning
}

class BeregningRepositoryImpl(private val dataSource: DataSource) : BeregningRepository {
    override fun lagre(
        beregning: Beregning
    ): Beregning {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                {
                    queryOf(
                        statement = Queries.lagreBeregning,
                        paramMap = mapOf(
                            "beregningId" to beregning.beregningId,
                            "behandlingId" to beregning.behandlingId,
                            "beregnetDato" to beregning.beregnetDato,
                            "beregningsperioder" to beregning.beregningsperioder.toJson(),
                            "sakId" to beregning.grunnlagMetadata.sakId,
                            "grunnlagVersjon" to beregning.grunnlagMetadata.versjon
                        )
                    ).let { query -> tx.run(query.asUpdate) }

                    beregning.beregningsperioder.forEach {
                        queryOf(
                            statement = Queries.lagreBeregningPeriode,
                            paramMap = mapOf(
                                "beregningId" to beregning.beregningId,
                                "datoFOM" to it.datoFOM,
                                "datoTOM" to it.datoTOM,
                                "utbetaltBeloep" to it.utbetaltBeloep,
                                "soeskenFlokk" to it.soeskenFlokk,
                                "grunnbeloepMnd" to it.grunnbelopMnd,
                                "grunnbeloep" to it.grunnbelopMnd
                            )
                        ).let { query -> tx.run(query.asUpdate) }
                    }
                }
            }
        }
        return hent(beregning.behandlingId)
    }

    override fun hent(behandlingId: UUID): Beregning = using(sessionOf(dataSource)) {
        it.transaction { tx -> // TODO: Få inn henting av beregningsperioder
            queryOf(
                statement = Queries.hentBeregning,
                paramMap = mapOf("behandlingId" to behandlingId)
            ).let { query -> tx.run(query.map(::toBeregning).asSingle) }
        }
    } ?: throw NotFoundException("Beregning med id $behandlingId finnes ikke i databasen")
}

private fun toBeregning(row: Row) = with(row) {
    Beregning(
        beregningId = uuid(BeregningDatabaseColumns.BeregningId.navn),
        behandlingId = uuid(BeregningDatabaseColumns.BehandlingId.navn),
        beregnetDato = localDateTime(BeregningDatabaseColumns.BeregnetDato.navn),
        beregningsperioder = objectMapper.readValue(string(BeregningDatabaseColumns.Beregningsperioder.navn)),
        grunnlagMetadata = Metadata(
            sakId = long(BeregningDatabaseColumns.SakId.navn),
            versjon = long(BeregningDatabaseColumns.GrunnlagVersjon.navn)
        )
    )
}

private enum class BeregningDatabaseColumns(val navn: String) {
    BeregningId("beregningId"),
    BehandlingId("behandlingId"),
    Beregningsperioder("beregningsperioder"),
    BeregnetDato("beregnetDato"),
    SakId("sakId"),
    GrunnlagVersjon("grunnlagVersjon")
}

private enum class BeregningsperiodeDatabaseColumns(val navn: String) {
    BeregningId("beregningId"),
    DatoFOM("datoFOM"),
    DatoTOM("datoTOM"),
    UtbetaltBeloep("utbetaltBeloep"),
    SoeskenFlokk("soeskenFlokk"),
    GrunnbeloepMnd("grunnbeloepMnd"),
    Grunnbeloep("grunnbeloep")
}

private object Queries {
    val hentBeregning = """
        |SELECT * 
        |FROM beregning WHERE ${BeregningDatabaseColumns.BehandlingId.navn} = :behandlingId::UUID
    """.trimMargin()

    val hentBeregningsperioder = """
        |SELECT * 
        |FROM beregningsperioder WHERE ${BeregningsperiodeDatabaseColumns.BeregningId.navn} = :beregningId::UUID
    """.trimMargin()

    val lagreBeregning = """
        |INSERT INTO beregning(${BeregningDatabaseColumns.BeregningId.navn}, ${BeregningDatabaseColumns.BehandlingId.navn}, ${BeregningDatabaseColumns.BeregnetDato.navn}, ${BeregningDatabaseColumns.Beregningsperioder.navn}, ${BeregningDatabaseColumns.SakId.navn}, ${BeregningDatabaseColumns.GrunnlagVersjon.navn}) 
        |VALUES(:beregningId::UUID, :behandlingId::UUID, :beregnetDato::TIMESTAMP, :beregningsperioder::JSONB, :sakId::BIGINT, :grunnlagVersjon::BIGINT) 
        |ON CONFLICT (${BeregningDatabaseColumns.BeregningId.navn})
        |DO UPDATE SET 
        |   ${BeregningDatabaseColumns.Beregningsperioder.navn} = EXCLUDED.${BeregningDatabaseColumns.Beregningsperioder.navn}, ${BeregningDatabaseColumns.BeregnetDato} = EXCLUDED.${BeregningDatabaseColumns.BeregnetDato}
    """.trimMargin()

    val lagreBeregningPeriode = """
        |INSERT INTO beregningsperiode(${BeregningsperiodeDatabaseColumns.BeregningId.navn}, ${BeregningsperiodeDatabaseColumns.DatoFOM.navn}, ${BeregningsperiodeDatabaseColumns.DatoTOM.navn}, ${BeregningsperiodeDatabaseColumns.UtbetaltBeloep.navn}, ${BeregningsperiodeDatabaseColumns.SoeskenFlokk.navn}, ${BeregningsperiodeDatabaseColumns.GrunnbeloepMnd.navn}, ${BeregningsperiodeDatabaseColumns.Grunnbeloep.navn}) 
        |VALUES(:beregningId::UUID, :datoFOM::TEXT, :datoTOM::TEXT, :utbetaltBeloep::BIGINT, :soeskenFlokk::JSONB, :grunnbeloepMnd::BIGINT, :grunnbeloep::BIGINT) 
    """.trimMargin()
}