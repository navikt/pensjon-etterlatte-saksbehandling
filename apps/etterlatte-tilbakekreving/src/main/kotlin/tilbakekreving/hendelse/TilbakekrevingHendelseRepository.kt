package no.nav.etterlatte.tilbakekreving.hendelse

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.UUID
import javax.sql.DataSource

enum class TilbakekrevingHendelseType {
    KRAVGRUNNLAG_MOTTATT,
    KRAV_VEDTAK_STATUS_MOTTATT,
    TILBAKEKREVINGSVEDTAK_SENDT,
    TILBAKEKREVINGSVEDTAK_KVITTERING,
}

class TilbakekrevingHendelseRepository(
    private val dataSource: DataSource,
) {
    fun lagreTilbakekrevingHendelse(
        sakId: Long,
        payload: String,
        type: TilbakekrevingHendelseType,
    ) = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement =
                """
                INSERT INTO tilbakekreving_hendelse(id, opprettet, sak_id, payload, type)
                VALUES(:id, now(), :sakId, :payload, :type)
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to UUID.randomUUID(),
                    "sakId" to sakId,
                    "payload" to payload,
                    "type" to type.name,
                ),
        ).let { query ->
            session.update(query).also {
                require(it == 1) {
                    "Feil under lagring av hendelse for sak $sakId"
                }
            }
        }
    }
}
