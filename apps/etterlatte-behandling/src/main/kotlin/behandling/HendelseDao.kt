package no.nav.etterlatte.behandling

import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.ZoneId
import java.util.UUID

class HendelseDao(private val connection: () -> Connection) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(HendelseDao::class.java)
    }

    fun behandlingOpprettet(behandling: Behandling) = lagreHendelse(
        UlagretHendelse(
            "BEHANDLING:OPPRETTET",
            behandling.behandlingOpprettet.toTidspunkt(ZoneId.systemDefault()),
            null,
            behandling.id,
            behandling.sak,
            null,
            null,
            null,
            null
        )
    )

    fun vedtakHendelse(
        behandling: Behandling,
        vedtakId: Long,
        hendelse: String,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) = lagreHendelse(
        UlagretHendelse(
            "VEDTAK:$hendelse",
            inntruffet,
            vedtakId,
            behandling.id,
            behandling.sak,
            saksbehandler,
            "SAKSBEHANDLER".takeIf { saksbehandler != null },
            kommentar,
            begrunnelse
        )
    )

    fun finnHendelserIBehandling(behandling: UUID): List<LagretHendelse> {
        val stmt = connection().prepareStatement(
            """
                |SELECT id, hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse 
                |FROM behandlinghendelse
                |where behandlingid = ?
            """.trimMargin()
        )
        stmt.setObject(1, behandling)
        return stmt.executeQuery().toList {
            LagretHendelse(
                getLong("id"),
                getString("hendelse"),
                requireNotNull(getTidspunkt("opprettet")),
                getTidspunkt("inntruffet"),
                getLongOrNull("vedtakid"),
                getUUID("behandlingid"),
                getLong("sakid"),
                getString("ident"),
                getString("identType"),
                getString("kommentar"),
                getString("valgtBegrunnelse")
            )
        }
    }

    fun lagreHendelse(hendelse: UlagretHendelse) {
        val stmt = connection().prepareStatement(
            """
            |INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) 
            |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimMargin()
        )
        stmt.setString(1, hendelse.hendelse)
        stmt.setTidspunkt(2, hendelse.inntruffet)
        stmt.setLong(3, hendelse.vedtakId)
        stmt.setObject(4, hendelse.behandlingId)
        stmt.setLong(5, hendelse.sakId)
        stmt.setString(6, hendelse.ident)
        stmt.setString(7, hendelse.identType)
        stmt.setString(8, hendelse.kommentar)
        stmt.setString(9, hendelse.valgtBegrunnelse)

        stmt.executeUpdate()

        logger.info("lagret hendelse: $hendelse")
    }
}

fun PreparedStatement.setTidspunkt(index: Int, value: Tidspunkt?) =
    setTimestamp(index, value?.instant?.let(Timestamp::from))

fun PreparedStatement.setLong(index: Int, value: Long?) =
    if (value == null) setNull(index, Types.BIGINT) else setLong(3, value)

fun ResultSet.getTidspunkt(name: String) = getTimestamp(name)?.toInstant()?.toTidspunkt()
fun ResultSet.getUUID(name: String) = getObject(name) as UUID
fun ResultSet.getLongOrNull(name: String) = getLong(name).takeIf { !wasNull() }

data class LagretHendelse(
    val id: Long,
    val hendelse: String,
    val opprettet: Tidspunkt,
    val inntruffet: Tidspunkt?,
    val vedtakId: Long?,
    val behandlingId: UUID,
    val sakId: Long,
    val ident: String?,
    val identType: String?,
    val kommentar: String?,
    val valgtBegrunnelse: String?
)

data class UlagretHendelse(
    val hendelse: String,
    val inntruffet: Tidspunkt?,
    val vedtakId: Long?,
    val behandlingId: UUID,
    val sakId: Long,
    val ident: String?,
    val identType: String?,
    val kommentar: String?,
    val valgtBegrunnelse: String?
)