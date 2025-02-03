package no.nav.etterlatte.grunnlag.tmpjobb

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class GrunnlagJobbDao(
    private val datasource: DataSource,
) {
    fun hentTommePersongalleriV1(): List<String> =
        datasource.connection.use {
            it
                .prepareStatement(
                    """
                    select opplysning_id from grunnlagshendelse where opplysning_type = 'PERSONGALLERI_V1' AND opplysning is null limit 100;
                    """.trimIndent(),
                ).executeQuery()
                .toList { getString("opplysning_id") }
        }

    fun oppdaterTomtPersongalleri(opplysning: OpplysningDao.GrunnlagHendelse) =
        datasource.connection.use {
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

    fun oppdaterTomtPersongalleriBatch(opplysningr: List<OpplysningDao.GrunnlagHendelse>) =
        datasource.connection.use {
            it
                .prepareStatement(
                    """
                    UPDATE grunnlagshendelse
                    set opplysning = ?
                    where opplysning_id = ?;
                    """.trimIndent(),
                ).apply {
                    opplysningr.forEach {
                        setString(1, it.opplysning.opplysning.serialize())
                        setObject(2, it.opplysning.id)
                        addBatch()
                    }
                }.executeBatch()
        }
}

fun JsonNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
