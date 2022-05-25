package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

class OpplysningDao(private val connection: () -> Connection) {
    data class GrunnlagHendelse(
        val opplysning: Grunnlagsopplysning<ObjectNode>,
        val sakId: Long,
        val hendelseNummer: Long
    )
    fun ResultSet.asBehandlingOpplysning(): Grunnlagsopplysning<ObjectNode> {
        return Grunnlagsopplysning(
            getObject("opplysning_id") as UUID,
            objectMapper.readValue(getString("kilde")),
            Opplysningstyper.valueOf(getString("opplysning_type")),
            objectMapper.createObjectNode(),
            getString("opplysning").deSerialize()!!,
        )
    }

    fun ResultSet.asGrunnlagshendelse(): GrunnlagHendelse {
        return GrunnlagHendelse(
            asBehandlingOpplysning(),
            getLong("sak_id"),
            getLong("hendelsenummer"),
        )

    }

    fun finnHendelserIGrunnlag(sakId: Long): List<GrunnlagHendelse> {
        return connection().prepareStatement("""
            SELECT sak_id, opplysning_id, kilde,  opplysning_type, opplysning, hendelsenummer   
            FROM grunnlagshendelse hendelse 
            WHERE hendelse.sak_id = ? AND NOT EXISTS(SELECT 1 FROM grunnlagshendelse annen where annen.sak_id = hendelse.sak_id AND hendelse.opplysning_type = annen.opplysning_type AND annen.hendelsenummer > hendelse.hendelsenummer)""".trimIndent())
            .apply {
                setLong(1, sakId)
            }.executeQuery().toList { asGrunnlagshendelse() }
    }

    fun leggOpplysningTilGrunnlag(sakId: Long, behandlingsopplysning: Grunnlagsopplysning<ObjectNode>): Long {
        return connection().prepareStatement("""INSERT INTO grunnlagshendelse(opplysning_id, sak_id, opplysning, kilde, opplysning_type, hendelsenummer)
            | VALUES(?, ?, ?, ?, ?, COALESCE((select max (hendelsenummer) + 1 from grunnlagshendelse where sak_id = ?), 1)) returning hendelsenummer """.trimMargin())
            .apply {
                setObject(1, behandlingsopplysning.id)
                setLong(2, sakId)
                setString(3, behandlingsopplysning.opplysning.serialize())
                setString(4, behandlingsopplysning.kilde.toJson())
                setString(5, behandlingsopplysning.opplysningType.name)
                setLong(6, sakId)
            }.executeQuery().apply { next() }.getLong("hendelsenummer")
    }
}

fun ObjectNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
fun String?.deSerialize() = this?.let { objectMapper.readValue(this, ObjectNode::class.java) }