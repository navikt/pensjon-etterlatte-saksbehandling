package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.behandling.hendelse.setLong
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import no.nav.helse.rapids_rivers.toUUID
import java.sql.ResultSet

class DoedshendelseDao(val connectionAutoclosing: ConnectionAutoclosing) {
    fun oppdaterBrevDistribuertDoedshendelse(doedshendelseBrevDistribuert: DoedshendelseBrevDistribuert) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    UPDATE doedshendelse 
                    SET status = ?, brev_id = ?
                    WHERE sakId = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, Status.FERDIG.name)
                    setLong(2, doedshendelseBrevDistribuert.brevId)
                    setLong(3, doedshendelseBrevDistribuert.sakId)
                }
            }
        }
    }

    fun opprettDoedshendelse(doedshendelse: Doedshendelse) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    INSERT INTO doedshendelse (id, avdoed_fnr, avdoed_doedsdato, beroert_fnr, relasjon, opprettet, endret, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).apply {
                    setString(1, doedshendelse.id.toString())
                    setString(2, doedshendelse.avdoedFnr)
                    setDate(3, java.sql.Date.valueOf(doedshendelse.avdoedDoedsdato))
                    setString(4, doedshendelse.beroertFnr)
                    setString(5, doedshendelse.relasjon.name)
                    setTidspunkt(6, doedshendelse.opprettet)
                    setTidspunkt(7, doedshendelse.endret)
                    setString(8, doedshendelse.status.name)
                }.executeUpdate()
            }
        }
    }

    fun oppdaterDoedshendelse(doedshendelse: Doedshendelse) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    UPDATE doedshendelse 
                    SET sak_id = ?, status = ?, utfall = ?, endret = ?
                    WHERE id = ?
                    """.trimIndent(),
                ).apply {
                    setLong(1, doedshendelse.sakId)
                    setString(2, doedshendelse.status.name)
                    setString(3, doedshendelse.utfall?.name)
                    setTidspunkt(4, doedshendelse.endret)
                    setString(5, doedshendelse.id.toString())
                }.executeUpdate()
            }
        }
    }

    fun hentDoedshendelserMedStatus(status: Status): List<Doedshendelse> {
        return connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT id, avdoed_fnr, avdoed_doedsdato, beroert_fnr, relasjon, opprettet, endret, status, utfall, oppgave_id, brev_id, sak_id
                    FROM doedshendelse
                    WHERE status = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, status.name)
                }.executeQuery().toList { asDoedshendelse() }
            }
        }
    }
}

private fun ResultSet.asDoedshendelse(): Doedshendelse =
    Doedshendelse(
        id = getString("id").toUUID(),
        avdoedFnr = getString("avdoed_fnr"),
        avdoedDoedsdato = getDate("avdoed_doedsdato").toLocalDate(),
        beroertFnr = getString("beroert_fnr"),
        relasjon = getString("relasjon").let { relasjon -> Relasjon.valueOf(relasjon) },
        opprettet = getTidspunkt("opprettet"),
        endret = getTidspunkt("endret"),
        status = Status.valueOf(getString("status")),
        utfall = getString("utfall")?.let { utfall -> Utfall.valueOf(utfall) },
        oppgaveId = getString("oppgave_id")?.toUUID(),
        brevId = getString("brev_id")?.toLong(),
        sakId = getString("sak_id")?.toLong(),
    )
