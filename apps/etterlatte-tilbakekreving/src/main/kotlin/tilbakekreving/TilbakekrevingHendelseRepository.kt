package no.nav.etterlatte.tilbakekreving

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
                        .map { row ->
                            TilbakekrevingHendelse(
                                id = UUID.fromString(row.string("id")),
                                opprettet = row.tidspunkt("opprettet"),
                                sakId = SakId(row.long("sak_id")),
                                payload = row.string("payload"),
                                status = TilbakekrevingHendelseStatus.valueOf(row.string("status")),
                                type = TilbakekrevingHendelseType.valueOf(row.string("type")),
                                jmsTimestamp = row.tidspunktOrNull("jms_timestamp"),
                            )
                        }.asSingle,
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
}
