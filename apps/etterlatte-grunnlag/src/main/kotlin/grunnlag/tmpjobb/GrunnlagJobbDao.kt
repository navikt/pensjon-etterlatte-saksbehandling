package no.nav.etterlatte.grunnlag.tmpjobb

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.firstOrNull
import javax.sql.DataSource

class GrunnlagJobbDao(
    private val datasource: DataSource,
) {
    private val connection get() = datasource.connection

    fun hentTommePersongalleriV1(): String? =
        connection.use {
            it
                .prepareStatement(
                    """
                    select opplysning_id from grunnlagshendelse where opplysning_type = 'PERSONGALLERI_V1' and opplysning_id = '3b5f0d72-d188-463c-9854-01eecac52641' AND opplysning is null limit 1;
                    """.trimIndent(),
                ).executeQuery()
                .firstOrNull { getString("opplysning_id") }
        }

    fun oppdaterTomtPersongalleri(opplysning: OpplysningDao.GrunnlagHendelse) =
        connection.use {
            it
                .prepareStatement(
                    """
                    UPDATE grunnlagshendelse
                    set opplysning = ?
                    where opplysning_id = ?;
                    """.trimIndent(),
                ).apply {
                    setString(1, opplysning.opplysning.opplysning.serialize())
                    setObject(2, opplysning.opplysning.id)
                }.executeUpdate()
        }
}

fun JsonNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
