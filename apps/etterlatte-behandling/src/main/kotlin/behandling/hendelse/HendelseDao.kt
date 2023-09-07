package no.nav.etterlatte.behandling.hendelse

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.BehandlingOpprettet
import no.nav.etterlatte.libs.common.behandling.KlageHendelseType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.*

class HendelseDao(private val connection: () -> Connection) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(HendelseDao::class.java)
    }

    fun behandlingAvbrutt(
        behandling: Behandling,
        saksbehandler: String,
        kommentar: String? = null,
        valgtBegrunnelse: String? = null
    ) = lagreHendelse(
        UlagretHendelse(
            hendelse = "BEHANDLING:AVBRUTT",
            inntruffet = Tidspunkt.now(),
            vedtakId = null,
            behandlingId = behandling.id,
            sakId = behandling.sak.id,
            ident = saksbehandler,
            identType = "SAKSBEHANDLER",
            kommentar = kommentar,
            valgtBegrunnelse = valgtBegrunnelse
        )
    )

    fun behandlingOpprettet(behandling: BehandlingOpprettet) = lagreHendelse(
        UlagretHendelse(
            "BEHANDLING:OPPRETTET",
            behandling.timestamp,
            null,
            behandling.id,
            behandling.sak,
            null,
            null,
            null,
            null
        )
    )

    fun klageHendelse(
        klageId: UUID,
        sakId: Long,
        hendelse: KlageHendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) = lagreHendelse(
        UlagretHendelse(
            hendelse = "KLAGE:${hendelse.name}",
            inntruffet = inntruffet,
            vedtakId = null,
            behandlingId = klageId,
            sakId = sakId,
            ident = saksbehandler,
            identType = "SAKSBEHANLDER".takeIf { saksbehandler != null },
            kommentar = kommentar,
            valgtBegrunnelse = begrunnelse
        )
    )

    fun vedtakHendelse(
        behandlingId: UUID,
        sakId: Long,
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) = lagreHendelse(
        UlagretHendelse(
            "VEDTAK:${hendelse.name}",
            inntruffet,
            vedtakId,
            behandlingId,
            sakId,
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
                getTidspunkt("opprettet"),
                getTidspunktOrNull("inntruffet"),
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

    private fun lagreHendelse(hendelse: UlagretHendelse) {
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

fun PreparedStatement.setLong(index: Int, value: Long?) =
    if (value == null) setNull(index, Types.BIGINT) else setLong(3, value)
fun ResultSet.getUUID(name: String) = getObject(name) as UUID
fun ResultSet.getLongOrNull(name: String) = getLong(name).takeUnless { wasNull() }