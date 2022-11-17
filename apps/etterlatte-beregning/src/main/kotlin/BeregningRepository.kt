import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.plugins.NotFoundException
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.objectMapper
import java.util.*
import javax.sql.DataSource

interface BeregningRepository {
    fun lagre(beregning: BeregningsResultat, beregningstyper: Beregningstyper): BeregningsResultat
    fun hent(id: UUID): BeregningsResultat
}

class BeregningRepositoryImpl(private val dataSource: DataSource) : BeregningRepository {
    override fun lagre(beregning: BeregningsResultat, beregningstyper: Beregningstyper): BeregningsResultat {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                queryOf(
                    statement = Queries.lagreBeregning,
                    paramMap = mapOf("beregningstype" to beregningstyper, "beregning" to beregning)
                ).let { query -> tx.run(query.asUpdate) }
            }
        }
        return hent(beregning.id)
    }

    override fun hent(id: UUID): BeregningsResultat = using(sessionOf(dataSource)) {
        it.transaction { tx ->
            queryOf(
                statement = Queries.hentBeregning,
                paramMap = mapOf("beregningId" to id)
            ).let { query -> tx.run(query.map(::toBeregning).asSingle) }
        }
    } ?: throw NotFoundException("Beregning med id $id finnes ikke i databasen")

    private fun toBeregning(row: Row) = with(row) {
        BeregningsResultat(
            id = uuid(DatabaseColumns.BeregningId.navn),
            type = Beregningstyper.valueOf(string(DatabaseColumns.Beregningstype.navn)),
            endringskode = Endringskode.NY,
            resultat = BeregningsResultatType.BEREGNET,
            beregnetDato = localDateTime(DatabaseColumns.BeregnetDato.navn),
            beregningsperioder = objectMapper.readValue(string(DatabaseColumns.Beregningsperioder.navn))
        )
    }
}

private enum class DatabaseColumns(val navn: String) {
    BeregningId("beregningId"),
    Beregningstype("beregningstype"),
    Beregning("beregning"),
    Beregningsperioder("beregningsperioder"),
    BeregnetDato("beregnetDato"),
    Grunnlagsversjon("grunnlagsversjon")
}

private object Queries {
    val hentBeregning = """x
        |SELECT * 
        |FROM beregning WHERE beregningId = :beregningId::UUID
    """.trimMargin()

    val lagreBeregning = """
        |INSERT INTO beregning(beregningId, beregningstype, beregning) 
        |VALUES(:beregningId::UUID, :beregningstype::TEXT, :beregning::JSONB) 
        |ON CONFLICT (behandlingId)  
        |DO UPDATE SET 
        |   BEREGNINGSTYPE = EXCLUDED.beregningstype, beregning = EXCLUDED.beregning,  
    """.trimMargin()
}