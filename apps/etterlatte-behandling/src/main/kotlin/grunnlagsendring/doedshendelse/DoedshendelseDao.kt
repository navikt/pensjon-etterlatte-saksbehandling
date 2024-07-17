package no.nav.etterlatte.grunnlagsendring.doedshendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminder
import no.nav.etterlatte.behandling.hendelse.setLong
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.toList
import no.nav.helse.rapids_rivers.toUUID
import java.sql.ResultSet

class DoedshendelseDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun oppdaterBrevDistribuertDoedshendelse(doedshendelseBrevDistribuert: DoedshendelseBrevDistribuert) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    UPDATE doedshendelse 
                    SET status = ?, brev_id = ?
                    WHERE sak_id = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, Status.FERDIG.name)
                    setLong(2, doedshendelseBrevDistribuert.brevId)
                    setLong(3, doedshendelseBrevDistribuert.sakId)
                }.executeUpdate()
            }
        }
    }

    fun opprettDoedshendelse(doedshendelseInternal: DoedshendelseInternal) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    INSERT INTO doedshendelse (id, avdoed_fnr, avdoed_doedsdato, beroert_fnr, relasjon, opprettet, endret, status, endringstype)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).apply {
                    setString(1, doedshendelseInternal.id.toString())
                    setString(2, doedshendelseInternal.avdoedFnr)
                    setDate(3, java.sql.Date.valueOf(doedshendelseInternal.avdoedDoedsdato))
                    setString(4, doedshendelseInternal.beroertFnr)
                    setString(5, doedshendelseInternal.relasjon.name)
                    setTidspunkt(6, doedshendelseInternal.opprettet)
                    setTidspunkt(7, doedshendelseInternal.endret)
                    setString(8, doedshendelseInternal.status.name)
                    setString(9, doedshendelseInternal.endringstype?.name)
                }.executeUpdate()
            }
        }
    }

    fun oppdaterDoedshendelse(doedshendelseInternal: DoedshendelseInternal) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    UPDATE doedshendelse 
                    SET sak_id = ?, status = ?, utfall = ?, endret = ?, oppgave_id = ?, brev_id = ?, kontrollpunkter = ?
                    WHERE id = ?
                    """.trimIndent(),
                ).apply {
                    setLong(1, doedshendelseInternal.sakId)
                    setString(2, doedshendelseInternal.status.name)
                    setString(3, doedshendelseInternal.utfall?.name)
                    setTidspunkt(4, doedshendelseInternal.endret)
                    setString(5, doedshendelseInternal.oppgaveId?.toString())
                    setLong(6, doedshendelseInternal.brevId)
                    setJsonb(7, doedshendelseInternal.kontrollpunkter)
                    setString(8, doedshendelseInternal.id.toString())
                }.executeUpdate()
            }
        }
    }

    fun hentDoedshendelserMedSakider(sakider: List<Long>): List<DoedshendelseInternal> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT id, avdoed_fnr, avdoed_doedsdato, beroert_fnr, relasjon, opprettet, endret, status, utfall, oppgave_id, brev_id, sak_id, endringstype, kontrollpunkter
                    FROM doedshendelse
                    WHERE status = any(?)
                    """.trimIndent(),
                ).apply {
                    setArray(1, createArrayOf("bigint", sakider.toTypedArray()))
                }.executeQuery()
                    .toList { asDoedshendelse() }
            }
        }

    fun hentDoedshendelserMedStatus(status: List<Status>): List<DoedshendelseInternal> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT id, avdoed_fnr, avdoed_doedsdato, beroert_fnr, relasjon, opprettet, endret, status, utfall, oppgave_id, brev_id, sak_id, endringstype, kontrollpunkter
                    FROM doedshendelse
                    WHERE status = any(?)
                    """.trimIndent(),
                ).apply {
                    setArray(1, createArrayOf("text", status.toTypedArray()))
                }.executeQuery()
                    .toList { asDoedshendelse() }
            }
        }

    fun hentDoedshendelserMedStatusFerdigOgUtFallBrevBp(): List<DoedshendelseReminder> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT id, endret, beroert_fnr, sak_id, relasjon
                    FROM doedshendelse
                    WHERE status = ?
                    AND relasjon = ?
                    AND utfall = ANY (?)
                    """.trimIndent(),
                ).apply {
                    setString(1, Status.FERDIG.name)
                    setString(2, Relasjon.BARN.name)
                    setArray(
                        3,
                        createArrayOf(
                            "text",
                            listOf(
                                Utfall.BREV,
                                Utfall.BREV_OG_OPPGAVE,
                            ).toTypedArray(),
                        ),
                    )
                }.executeQuery()
                    .toList { asDoedshendelseReminder() }
            }
        }

    fun hentDoedshendelserForPerson(avdoedFnr: String): List<DoedshendelseInternal> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT id, avdoed_fnr, avdoed_doedsdato, beroert_fnr, relasjon, opprettet, endret, status, utfall, oppgave_id, brev_id, sak_id, endringstype, kontrollpunkter
                    FROM doedshendelse
                    WHERE avdoed_fnr = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, avdoedFnr)
                }.executeQuery()
                    .toList { asDoedshendelse() }
            }
        }
}

private fun ResultSet.asDoedshendelseReminder(): DoedshendelseReminder =
    DoedshendelseReminder(
        id = getString("id").toUUID(),
        beroertFnr = getString("beroert_fnr"),
        relasjon = getString("relasjon").let { relasjon -> Relasjon.valueOf(relasjon) },
        endret = getTidspunkt("endret"),
        sakId = getString("sak_id")?.toLong(),
    )

private fun ResultSet.asDoedshendelse(): DoedshendelseInternal =
    DoedshendelseInternal(
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
        endringstype = getString("endringstype")?.let { Endringstype.valueOf(it) },
        kontrollpunkter =
            getString("kontrollpunkter")?.let { objectMapper.readValue(it) },
    )
