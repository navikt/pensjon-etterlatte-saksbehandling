package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class OpplysningDao(
    private val datasource: DataSource,
) {
    data class GrunnlagHendelse(
        val opplysning: Grunnlagsopplysning<JsonNode>,
        val sakId: SakId,
        val hendelseNummer: Long,
    )

    private fun ResultSet.asBehandlingOpplysning(): Grunnlagsopplysning<JsonNode> =
        Grunnlagsopplysning(
            id = getObject("opplysning_id") as UUID,
            kilde = objectMapper.readValue(getString("kilde")),
            opplysningType = Opplysningstype.valueOf(getString("opplysning_type")),
            meta = objectMapper.createObjectNode(),
            opplysning = getString("opplysning").deSerialize()!!,
            fnr = getString("fnr")?.let { Folkeregisteridentifikator.of(it) },
            periode =
                getString("fom")?.let { fom ->
                    Periode(
                        fom = YearMonth.parse(fom),
                        tom = getString("tom")?.let { tom -> YearMonth.parse(tom) },
                    )
                },
        )

    private fun ResultSet.asGrunnlagshendelse(): GrunnlagHendelse =
        GrunnlagHendelse(
            opplysning = asBehandlingOpplysning(),
            sakId = SakId(getLong("sak_id")),
            hendelseNummer = getLong("hendelsenummer"),
        )

    fun hent(opplysningId: UUID): GrunnlagHendelse =
        datasource.connection.use {
            it
                .prepareStatement("SELECT * FROM grunnlagshendelse WHERE opplysning_id = ?::uuid")
                .apply { setObject(1, opplysningId) }
                .executeQuery()
                .single { asGrunnlagshendelse() }
        }

    fun hentBulk(opplysningId: List<UUID>): List<GrunnlagHendelse> =
        datasource.connection.use {
            it
                .prepareStatement("SELECT * FROM grunnlagshendelse WHERE opplysning_id = ANY(?::uuid[])")
                .apply {
                    setArray(1, it.createArrayOf("text", opplysningId.toTypedArray()))
                }.executeQuery()
                .toList { asGrunnlagshendelse() }
        }
}

fun String?.deSerialize() = this?.let { objectMapper.readValue(this, JsonNode::class.java) }
