package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.sql.ResultSet
import java.sql.Types.VARCHAR
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class OpplysningDao(private val datasource: DataSource) {
    private val connection get() = datasource.connection

    data class GrunnlagHendelse(
        val opplysning: Grunnlagsopplysning<JsonNode>,
        val sakId: Long,
        val hendelseNummer: Long
    )

    private fun ResultSet.asBehandlingOpplysning(): Grunnlagsopplysning<JsonNode> {
        return Grunnlagsopplysning(
            id = getObject("opplysning_id") as UUID,
            kilde = objectMapper.readValue(getString("kilde")),
            opplysningType = Opplysningstyper.valueOf(getString("opplysning_type")),
            meta = objectMapper.createObjectNode(),
            opplysning = getString("opplysning").deSerialize()!!,
            fnr = getString("fnr")?.let { Foedselsnummer.of(it) },
            periode = getString("fom")?.let { fom ->
                Periode(
                    fom = YearMonth.parse(fom),
                    tom = getString("tom")?.let { tom -> YearMonth.parse(tom) }
                )
            }
        )
    }

    private fun ResultSet.asGrunnlagshendelse(): GrunnlagHendelse {
        return GrunnlagHendelse(
            opplysning = asBehandlingOpplysning(),
            sakId = getLong("sak_id"),
            hendelseNummer = getLong("hendelsenummer")
        )
    }

    fun hentAlleGrunnlagForSak(sakId: Long): List<GrunnlagHendelse> = connection.use {
        it.prepareStatement(
            """
            SELECT sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelsenummer, fnr, fom, tom
            FROM grunnlagshendelse hendelse 
            WHERE hendelse.sak_id = ?
            """.trimIndent()
        )
            .apply {
                setLong(1, sakId)
            }.executeQuery().toList { asGrunnlagshendelse() }
    }

    fun finnHendelserIGrunnlag(sakId: Long): List<GrunnlagHendelse> = connection.use {
        it.prepareStatement(
            """
            SELECT sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelsenummer, fnr, fom, tom
            FROM grunnlagshendelse hendelse 
            WHERE hendelse.sak_id = ? AND NOT EXISTS(SELECT 1 FROM grunnlagshendelse annen where annen.sak_id = hendelse.sak_id AND hendelse.opplysning_type = annen.opplysning_type AND annen.hendelsenummer > hendelse.hendelsenummer)
            """.trimIndent()
        )
            .apply {
                setLong(1, sakId)
            }.executeQuery().toList { asGrunnlagshendelse() }
    }

    fun finnNyesteGrunnlag(sakId: Long, opplysningType: Opplysningstyper): GrunnlagHendelse? =
        connection.use {
            it.prepareStatement(
                """
                SELECT sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelsenummer, fnr, fom, tom
                FROM grunnlagshendelse hendelse 
                WHERE hendelse.sak_id = ? AND hendelse.opplysning_type = ? AND NOT EXISTS(SELECT 1 FROM grunnlagshendelse annen where annen.sak_id = hendelse.sak_id AND hendelse.opplysning_type = annen.opplysning_type AND annen.hendelsenummer > hendelse.hendelsenummer)
                """.trimIndent()
            )
                .apply {
                    setLong(1, sakId)
                    setString(2, opplysningType.name)
                }.executeQuery().singleOrNull { asGrunnlagshendelse() }
        }

    fun leggOpplysningTilGrunnlag(
        sakId: Long,
        behandlingsopplysning: Grunnlagsopplysning<JsonNode>,
        fnr: Foedselsnummer? = null
    ): Long =
        connection.use {
            it.prepareStatement(
                """INSERT INTO grunnlagshendelse(opplysning_id, sak_id, opplysning, kilde, opplysning_type, hendelsenummer, fnr, fom, tom)
                | VALUES(?, ?, ?, ?, ?, COALESCE((select max (hendelsenummer) + 1 from grunnlagshendelse where sak_id = ?), 1), ?, ?, ?) returning hendelsenummer 
                """.trimMargin()
            )
                .apply {
                    setObject(1, behandlingsopplysning.id)
                    setLong(2, sakId)
                    setString(3, behandlingsopplysning.opplysning.serialize())
                    setString(4, behandlingsopplysning.kilde.toJson())
                    setString(5, behandlingsopplysning.opplysningType.name)
                    setLong(6, sakId)
                    if (fnr != null) setString(7, fnr.value) else setNull(7, VARCHAR)
                    behandlingsopplysning.periode?.fom?.let { setString(8, it.toString()) } ?: setNull(8, VARCHAR)
                    behandlingsopplysning.periode?.tom?.let { setString(9, it.toString()) } ?: setNull(9, VARCHAR)
                }.executeQuery().apply { next() }.getLong("hendelsenummer")
        }

    fun slettAlleOpplysningerISak(sakId: Long) =
        connection.use {
            it.prepareStatement("""DELETE FROM grunnlagshendelse where sak_id = ?""")
                .apply {
                    setLong(1, sakId)
                }.execute()
        }
}

fun JsonNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
fun String?.deSerialize() = this?.let { objectMapper.readValue(this, JsonNode::class.java) }