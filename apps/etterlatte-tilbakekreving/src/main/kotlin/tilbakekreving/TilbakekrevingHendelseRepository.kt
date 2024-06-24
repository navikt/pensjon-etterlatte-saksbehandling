package no.nav.etterlatte.tilbakekreving

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.database.singleOrNull
import java.util.UUID
import javax.sql.DataSource

data class TilbakekrevingHendelse(
    val id: UUID,
    val opprettet: Tidspunkt,
    val sakId: Long,
    val payload: String,
    val status: TilbakekrevingHendelseStatus,
    val type: TilbakekrevingHendelseType,
    val jmsTimestamp: Tidspunkt? = null,
)

enum class TilbakekrevingHendelseStatus {
    NY,
    FERDIGSTILT,
}

enum class TilbakekrevingHendelseType(
    val mqHendelse: Boolean = false,
) {
    KRAVGRUNNLAG_MOTTATT(true),
    KRAV_VEDTAK_STATUS_MOTTATT(true),
    KRAVGRUNNLAG_FORESPOERSEL_SENDT,
    KRAVGRUNNLAG_FORESPOERSEL_KVITTERING,
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
        jmsTimestamp: Tidspunkt? = null,
    ): UUID =
        using(sessionOf(dataSource)) { session ->
            val id = UUID.randomUUID()
            queryOf(
                statement =
                    """
                    INSERT INTO tilbakekreving_hendelse(id, opprettet, sak_id, payload, type, jms_timestamp, status)
                    VALUES(:id, now(), :sakId, :payload, :type, :jmsTimestamp, :status)
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to id,
                        "sakId" to sakId,
                        "payload" to payload,
                        "type" to type.name,
                        "jmsTimestamp" to jmsTimestamp?.toTimestamp(),
                        "status" to
                            if (type.mqHendelse) {
                                TilbakekrevingHendelseStatus.NY.name
                            } else {
                                TilbakekrevingHendelseStatus.FERDIGSTILT.name
                            },
                    ),
            ).let { query ->
                session.update(query).also {
                    require(it == 1) {
                        "Feil under lagring av hendelse for sak $sakId"
                    }
                }
                id
            }
        }

    fun hentSisteTilbakekrevingHendelse(
        sakId: Long,
        type: TilbakekrevingHendelseType,
    ): TilbakekrevingHendelse? {
        dataSource.connection.use {
            val stmt =
                it
                    .prepareStatement(
                        """
                        SELECT id, opprettet, sak_id, payload, status, type, jms_timestamp 
                        FROM tilbakekreving_hendelse 
                        WHERE sak_id = ? AND type = ?
                        ORDER BY jms_timestamp DESC
                        """.trimIndent(),
                    ).apply {
                        setLong(1, sakId)
                        setString(2, type.name)
                    }

            return stmt.executeQuery().singleOrNull {
                TilbakekrevingHendelse(
                    id = UUID.fromString(getString("id")),
                    opprettet = getTidspunkt("opprettet"),
                    sakId = getLong("sak_id"),
                    payload = getString("payload"),
                    status = TilbakekrevingHendelseStatus.valueOf(getString("status")),
                    type = TilbakekrevingHendelseType.valueOf(getString("type")),
                    jmsTimestamp = getTidspunktOrNull("jms_timestamp"),
                )
            }
        }
    }

    fun ferdigstillTilbakekrevingHendelse(
        sakId: Long,
        id: UUID,
    ) = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement =
                """
                UPDATE tilbakekreving_hendelse 
                SET status = '${TilbakekrevingHendelseStatus.FERDIGSTILT.name}' 
                WHERE id = ?
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to id,
                ),
        ).let { query ->
            session.update(query).also {
                require(it == 1) {
                    "Feil under oppdatering av hendelse for sak $sakId"
                }
            }
            id
        }
    }
}
