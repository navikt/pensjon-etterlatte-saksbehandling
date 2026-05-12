package no.nav.etterlatte.tilbakekreving

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.tidspunktOrNull
import java.util.UUID
import javax.sql.DataSource

data class TilbakekrevingHendelse(
    val id: UUID,
    val opprettet: Tidspunkt,
    val sakId: SakId,
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
    KRAVGRUNNLAG_OMGJOERING_FORESPOERSEL_SENDT,
    KRAVGRUNNLAG_OMGJOERING_MOTTATT,
}

class TilbakekrevingHendelseRepository(
    private val dataSource: DataSource,
) {
    fun lagreTilbakekrevingHendelse(
        sakId: SakId,
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
                        "sakId" to sakId.sakId,
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
                    krev(it == 1) {
                        "Feil under lagring av hendelse for sak $sakId"
                    }
                }
                id
            }
        }

    fun hentSisteTilbakekrevingHendelse(sakId: SakId): TilbakekrevingHendelse? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT id, opprettet, sak_id, payload, status, type, jms_timestamp 
                        FROM tilbakekreving_hendelse 
                        WHERE sak_id = :sakId
                        ORDER BY jms_timestamp DESC
                        LIMIT 1
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "sakId" to sakId.sakId,
                    ),
            ).let {
                session.run(
                    it
                        .map(Row::tilTilbakekrevingHendelse)
                        .asSingle,
                )
            }
        }

    fun ferdigstillTilbakekrevingHendelse(
        sakId: SakId,
        id: UUID,
    ): UUID =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    UPDATE tilbakekreving_hendelse 
                    SET status = '${TilbakekrevingHendelseStatus.FERDIGSTILT.name}' 
                    WHERE id = :id
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to id,
                    ),
            ).let { query ->
                session.update(query).also {
                    krev(it == 1) {
                        "Feil under oppdatering av hendelse for sak $sakId"
                    }
                }
                id
            }
        }

    fun finnHendelserForSak(sakId: SakId): List<TilbakekrevingHendelse> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT id, opprettet, sak_id, payload, status, type, jms_timestamp 
                        FROM tilbakekreving_hendelse 
                        WHERE sak_id = :sakId
                        ORDER BY opprettet DESC
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "sakId" to sakId.sakId,
                    ),
            ).let { query ->
                session.run(query.map(Row::tilTilbakekrevingHendelse).asList)
            }
        }
}

private fun Row.tilTilbakekrevingHendelse(): TilbakekrevingHendelse =
    TilbakekrevingHendelse(
        id = UUID.fromString(this.string("id")),
        opprettet = this.tidspunkt("opprettet"),
        sakId = SakId(this.long("sak_id")),
        payload = this.string("payload"),
        status = TilbakekrevingHendelseStatus.valueOf(this.string("status")),
        type = TilbakekrevingHendelseType.valueOf(this.string("type")),
        jmsTimestamp = this.tidspunktOrNull("jms_timestamp"),
    )
