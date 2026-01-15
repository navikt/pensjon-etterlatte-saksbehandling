package no.nav.etterlatte.institusjonsopphold.personer

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.institusjonsopphold.model.Institusjonsopphold
import no.nav.etterlatte.libs.database.setNullableDate
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.toList

class InstitusjonsoppholdPersonerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreKjoering(fnr: String) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    INSERT INTO institusjonsopphold_personer (person_ident, status) 
                    VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                ).apply {
                    setString(1, fnr)
                    setString(2, "NY")
                }.executeUpdate()
            }
        }
    }

    fun hentUbehandledePersoner(batchSize: Int): List<String> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT person_ident
                    FROM institusjonsopphold_personer
                    WHERE status = 'NY'
                    ORDER BY person_ident
                    limit ?
                    """,
                ).apply {
                    setInt(1, batchSize)
                }.executeQuery()
                    .toList { getString("person_ident") }
            }
        }

    fun markerSomFerdig(fnrListe: List<String>) =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    UPDATE institusjonsopphold_personer
                    SET status = 'FERDIG'
                    WHERE person_ident = ANY(?)
                    """,
                ).apply {
                    setArray(1, createArrayOf("text", fnrListe.toTypedArray()))
                }.executeUpdate()
            }
        }

    fun hentInstitusjonsopphold(fnr: String): List<Institusjonsopphold> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT * FROM institusjonsopphold_hentet
                    WHERE person_ident = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, fnr)
                }.executeQuery()
                    .toList {
                        Institusjonsopphold(
                            oppholdId = getLong("opphold_id"),
                            institusjonstype = getString("institusjonstype"),
                            startdato = getDate("startdato").toLocalDate(),
                            faktiskSluttdato = getDate("faktisk_sluttdato")?.toLocalDate(),
                            forventetSluttdato = getDate("forventet_sluttdato")?.toLocalDate(),
                            institusjonsnavn = "",
                            organisasjonsnummer = "",
                        )
                    }
            }
        }

    fun personIdentForInstitusjonsopphold(oppholdId: Long): String =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT person_ident FROM institusjonsopphold_hentet
                    WHERE opphold_id = ?
                    """.trimIndent(),
                ).apply {
                    setLong(1, oppholdId)
                }.executeQuery()
                    .single {
                        getString("person_ident")
                    }
            }
        }

    fun lagreInstitusjonsopphold(
        personIdent: String,
        institusjonsopphold: Institusjonsopphold,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            prepareStatement(
                """
                INSERT INTO institusjonsopphold_hentet (
                    opphold_id,
                    person_ident,
                    institusjonstype,
                    startdato,
                    faktisk_sluttdato,
                    forventet_sluttdato
                )
                VALUES (?,?,?,?,?,?)
                """.trimIndent(),
            ).apply {
                setLong(1, institusjonsopphold.oppholdId)
                setString(2, personIdent)
                setString(3, institusjonsopphold.institusjonstype)
                setNullableDate(4, institusjonsopphold.startdato)
                setNullableDate(5, institusjonsopphold.faktiskSluttdato)
                setNullableDate(6, institusjonsopphold.forventetSluttdato)
            }
        }.executeUpdate()
    }
}
