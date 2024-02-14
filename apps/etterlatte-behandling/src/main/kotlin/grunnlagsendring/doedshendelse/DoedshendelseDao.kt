package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import no.nav.helse.rapids_rivers.toUUID
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet

class DoedshendelseDao(val connection: () -> Connection) {
    fun opprettDoedshendelse(doedshendelse: Doedshendelse) =
        with(connection()) {
            prepareStatement(
                """
                INSERT INTO doedshendelse (id, avdoed_fnr, avdoed_doedsdato, beroert_fnr, relasjon, opprettet, endret, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            ).apply {
                setString(1, doedshendelse.id.toString())
                setString(2, doedshendelse.avdoedFnr)
                setDate(3, Date.valueOf(doedshendelse.avdoedDoedsdato))
                setString(4, doedshendelse.beroertFnr)
                setString(5, doedshendelse.relasjon.name)
                setTidspunkt(6, doedshendelse.opprettet)
                setTidspunkt(7, doedshendelse.endret)
                setString(8, doedshendelse.status.name)
            }.executeUpdate()
        }

    fun hentDoedshendelserMedStatus(status: DoedshendelseStatus): List<Doedshendelse> =
        with(connection()) {
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

private fun ResultSet.asDoedshendelse(): Doedshendelse =
    Doedshendelse(
        id = getString("id").toUUID(),
        avdoedFnr = getString("avdoed_fnr"),
        avdoedDoedsdato = getDate("avdoed_doedsdato").toLocalDate(),
        beroertFnr = getString("beroert_fnr"),
        relasjon = getString("relasjon").let { relasjon -> Relasjon.valueOf(relasjon) },
        opprettet = getTidspunkt("opprettet"),
        endret = getTidspunkt("endret"),
        status = DoedshendelseStatus.valueOf(getString("status")),
        utfall = getString("utfall")?.let { utfall -> Utfall.valueOf(utfall) },
        oppgaveId = getString("oppgave_id")?.toUUID(),
        brevId = getString("brev_id")?.toLong(),
        sakId = getString("sak_id")?.toLong(),
    )
