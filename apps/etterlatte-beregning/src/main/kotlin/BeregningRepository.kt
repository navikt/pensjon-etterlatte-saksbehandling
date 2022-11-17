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
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
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
            }
        }
        return hent(beregning.behandlingId)
    }

    override fun hent(behandlingId: UUID): Beregning = using(sessionOf(dataSource)) {
        it.transaction { tx ->
            queryOf(
                statement = Queries.hentBeregning,
                paramMap = mapOf("behandlingId" to behandlingId)
            ).let { query -> tx.run(query.map(::toBeregning).asSingle) }
        }
    } ?: throw NotFoundException("Beregning med id $behandlingId finnes ikke i databasen")
}

private fun toBeregning(row: Row) = with(row) {
    Beregning(
        beregningId = uuid(DatabaseColumns.BeregningId.navn),
        behandlingId = uuid(DatabaseColumns.BehandlingId.navn),
        beregnetDato = localDateTime(DatabaseColumns.BeregnetDato.navn),
        beregningsperioder = objectMapper.readValue(string(DatabaseColumns.Beregningsperioder.navn)),
        grunnlagMetadata = Metadata(
            sakId = long(DatabaseColumns.SakId.navn),
            versjon = long(DatabaseColumns.GrunnlagVersjon.navn)
        )
    )
}

private enum class DatabaseColumns(val navn: String) {
    BeregningId("beregningId"),
    BehandlingId("behandlingId"),
    Beregningsperioder("beregningsperioder"),
    BeregnetDato("beregnetDato"),
    SakId("sakId"),
    GrunnlagVersjon("grunnlagVersjon")
}

private object Queries {
    val hentBeregning = """
        |SELECT * 
        |FROM beregning WHERE ${DatabaseColumns.BehandlingId.navn} = :behandlingId::UUID
    """.trimMargin()

    val lagreBeregning = """
        |INSERT INTO beregning(${DatabaseColumns.BeregningId.navn}, ${DatabaseColumns.BehandlingId.navn}, ${DatabaseColumns.BeregnetDato.navn}, ${DatabaseColumns.Beregningsperioder.navn}, ${DatabaseColumns.SakId.navn}, ${DatabaseColumns.GrunnlagVersjon.navn}) 
        |VALUES(:beregningId::UUID, :behandlingId::UUID, :beregnetDato::TIMESTAMP, :beregningsperioder::JSONB, :sakId::BIGINT, :grunnlagVersjon::BIGINT) 
        |ON CONFLICT (${DatabaseColumns.BeregningId.navn})
        |DO UPDATE SET 
        |   ${DatabaseColumns.Beregningsperioder.navn} = EXCLUDED.${DatabaseColumns.Beregningsperioder.navn}, ${DatabaseColumns.BeregnetDato} = EXCLUDED.${DatabaseColumns.BeregnetDato}
    """.trimMargin()
}