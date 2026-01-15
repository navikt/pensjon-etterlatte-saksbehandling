package no.nav.etterlatte.institusjonsopphold.oppgaver

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.toList

class InstitusjonsoppholdOppgaverDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreKjoering(oppholdId: Long) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                       INSERT INTO institusjonsopphold_oppgaver (opphold_id, status) 
                       VALUES (?, ?)
                    x   ON CONFLICT DO NOTHING
                    """.trimIndent(),
                ).apply {
                    setLong(1, oppholdId)
                    setString(2, "NY")
                }.executeUpdate()
            }
        }
    }

    fun finnOppholdSomTrengerOppgave(): List<Long> {
        val oppholdSomIkkeFinnesIGjenny =
            connectionAutoclosing.hentConnection {
                with(it) {
                    prepareStatement(
                        """
                    SELECT DISTINCT opphold.opphold_id
                    FROM institusjonsopphold_hentet opphold
                    LEFT OUTER JOIN grunnlagsendringshendelse hendelse
                        ON hendelse.type = 'INSTITUSJONSOPPHOLD'
                        AND hendelse.samsvar_mellom_pdl_og_grunnlag -> 'oppholdBeriket' ->> 'oppholdId' = CAST(opphold.opphold_id AS text)
                    WHERE (opphold.faktisk_sluttdato IS NULL OR opphold.faktisk_sluttdato >= '2024-01-01')
                    AND hendelse.id IS NULL
                    """,
                    ).executeQuery()
                        .toList { getLong("opphold_id") }
                }
            }
        val oppholdSomFinnesMenMedForskjelligeDatoer =
            connectionAutoclosing.hentConnection {
                with(it) {
                    prepareStatement(
                        """
                    SELECT opphold_id
                    FROM (SELECT DISTINCT ON (opphold.opphold_id)
                          opphold.opphold_id,
                          opphold.startdato,
                          opphold.faktisk_sluttdato,
                          opphold.forventet_sluttdato,
                          hendelse.samsvar_mellom_pdl_og_grunnlag -> 'oppholdBeriket' ->> 'startdato'          gjennyStartdato,
                          hendelse.samsvar_mellom_pdl_og_grunnlag -> 'oppholdBeriket' ->> 'faktiskSluttdato'   gjennyFaktiskSlutt,
                          hendelse.samsvar_mellom_pdl_og_grunnlag -> 'oppholdBeriket' ->> 'forventetSluttdato' gjennyForventetSlutt
                          FROM institusjonsopphold_hentet opphold
                          JOIN grunnlagsendringshendelse hendelse
                          ON hendelse.type = 'INSTITUSJONSOPPHOLD' AND
                          hendelse.samsvar_mellom_pdl_og_grunnlag -> 'oppholdBeriket' ->> 'oppholdId' = CAST(opphold.opphold_id AS text)
                          ORDER BY opphold.opphold_id, hendelse.opprettet DESC) foo
                    WHERE (
                      coalesce(CAST(foo.faktisk_sluttdato AS text), 'N/A') != coalesce(foo.gjennyFaktiskSlutt, 'N/A')
                      OR coalesce(CAST(foo.forventet_sluttdato AS text), 'N/A') != coalesce(foo.gjennyForventetSlutt, 'N/A')
                      OR coalesce(CAST(foo.startdato AS text), 'N/A') != coalesce(foo.gjennyStartdato, 'N/A'));
                    """,
                    ).executeQuery()
                        .toList { getLong("opphold_id") }
                }
            }
        return oppholdSomIkkeFinnesIGjenny + oppholdSomFinnesMenMedForskjelligeDatoer
    }

    fun markerSomFerdig(oppholdId: Long) =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    UPDATE institusjonsopphold_oppgaver
                    SET status = 'FERDIG'
                    WHERE opphold_id = ?
                    """,
                ).apply {
                    setLong(1, oppholdId)
                }.executeUpdate()
            }
        }

    fun hentUbehandledeOpphold(): List<Long> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT opphold_id
                    FROM institusjonsopphold_oppgaver
                    WHERE status = 'NY'
                    ORDER BY opphold_id
                    """,
                ).executeQuery()
                    .toList { getLong("opphold_id") }
            }
        }
}
