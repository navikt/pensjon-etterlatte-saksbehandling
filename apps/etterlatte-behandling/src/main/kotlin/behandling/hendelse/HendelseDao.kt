package no.nav.etterlatte.behandling.hendelse

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.BehandlingOpprettet
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandlingHendelseType
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.UUID

class HendelseDao(private val connectionAutoclosing: ConnectionAutoclosing) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(HendelseDao::class.java)
    }

    fun behandlingAvbrutt(
        behandling: Behandling,
        saksbehandler: String,
        kommentar: String? = null,
        valgtBegrunnelse: String? = null,
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
            valgtBegrunnelse = valgtBegrunnelse,
        ),
    )

    fun behandlingOpprettet(behandling: BehandlingOpprettet) =
        behandlingOpprettet(
            behandling,
            "BEHANDLING:OPPRETTET",
        )

    fun behandlingOpprettet(
        behandling: BehandlingOpprettet,
        hendelseType: String,
    ) = lagreHendelse(
        UlagretHendelse(
            hendelseType,
            behandling.timestamp,
            null,
            behandling.id,
            behandling.sak,
            null,
            null,
            null,
            null,
        ),
    )

    fun behandlingHendelse(
        behandlingId: UUID,
        sakId: Long,
        saksbehandler: String,
        status: BehandlingStatus,
    ) = lagreHendelse(
        UlagretHendelse(
            hendelse = "BEHANDLING:${status.name}",
            inntruffet = Tidspunkt.now(),
            vedtakId = null,
            behandlingId = behandlingId,
            sakId = sakId,
            ident = saksbehandler,
            identType = "SAKSBEHANDLER".takeIf { saksbehandler != null },
            kommentar = null,
            valgtBegrunnelse = null,
        ),
    )

    fun klageHendelse(
        klageId: UUID,
        sakId: Long,
        hendelse: KlageHendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?,
    ) = lagreHendelse(
        UlagretHendelse(
            hendelse = "KLAGE:${hendelse.name}",
            inntruffet = inntruffet,
            vedtakId = null,
            behandlingId = klageId,
            sakId = sakId,
            ident = saksbehandler,
            identType = "SAKSBEHANDLER".takeIf { saksbehandler != null },
            kommentar = kommentar,
            valgtBegrunnelse = begrunnelse,
        ),
    )

    fun generellBehandlingHendelse(
        behandlingId: UUID,
        sakId: Long,
        hendelse: GenerellBehandlingHendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String? = null,
        begrunnelse: String? = null,
    ) = lagreHendelse(
        UlagretHendelse(
            hendelse = "GENERELL_BEHANDLING:${hendelse.name}",
            inntruffet = inntruffet,
            vedtakId = null,
            behandlingId = behandlingId,
            sakId = sakId,
            ident = saksbehandler,
            identType = "SAKSBEHANDLER".takeIf { saksbehandler != null },
            kommentar = kommentar,
            valgtBegrunnelse = begrunnelse,
        ),
    )

    fun tilbakekrevingOpprettet(
        tilbakekrevingId: UUID,
        sakId: Long,
    ) = behandlingOpprettet(
        hendelseType = "TILBAKEKREVING:OPPRETTET",
        behandling =
            BehandlingOpprettet(
                Tidspunkt.now(),
                tilbakekrevingId,
                sakId,
            ),
    )

    fun vedtakHendelse(
        behandlingId: UUID,
        sakId: Long,
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?,
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
            begrunnelse,
        ),
    )

    fun opppdatertGrunnlagHendelse(
        behandlingId: UUID,
        sakId: Long,
        saksbehandler: String?,
    ) = lagreHendelse(
        UlagretHendelse(
            hendelse = "BEHANDLING:OPPDATERT_GRUNNLAG",
            inntruffet = Tidspunkt.now(),
            vedtakId = null,
            behandlingId = behandlingId,
            sakId = sakId,
            ident = saksbehandler,
            identType = "SAKSBEHANDLER".takeIf { saksbehandler != null },
            kommentar = null,
            valgtBegrunnelse = null,
        ),
    )

    fun hentHendelserISak(sakId: Long): List<LagretHendelse> {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse
                        FROM behandlinghendelse
                        where sakid = ?
                        """.trimIndent(),
                    )

                statement.setLong(1, sakId)
                statement.executeQuery().toList {
                    asHendelse()
                }
            }
        }
    }

    fun finnHendelserIBehandling(behandling: UUID): List<LagretHendelse> {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                |SELECT id, hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse 
                |FROM behandlinghendelse
                |where behandlingid = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandling)
                stmt.executeQuery().toList {
                    asHendelse()
                }
            }
        }
    }

    private fun ResultSet.asHendelse(): LagretHendelse =
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
            getString("valgtBegrunnelse"),
        )

    private fun lagreHendelse(hendelse: UlagretHendelse) {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
            |INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) 
            |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimMargin(),
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
    }
}

fun PreparedStatement.setLong(
    index: Int,
    value: Long?,
) = if (value == null) setNull(index, Types.BIGINT) else setLong(index, value)

fun ResultSet.getUUID(name: String) = getObject(name) as UUID

fun ResultSet.getUUIDOrNull(name: String): UUID? =
    getObject(name)
        .let {
            if (wasNull()) {
                null
            } else {
                it as UUID
            }
        }

fun ResultSet.getLongOrNull(name: String) = getLong(name).takeUnless { wasNull() }
