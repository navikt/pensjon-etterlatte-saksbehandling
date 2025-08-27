package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet

class EtteroppgjoerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentAlleAktiveEtteroppgjoerForSak(sakId: SakId): List<Etteroppgjoer> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer 
                        WHERE sak_id = ?
                        AND status != ?
                        """.trimIndent(),
                    )
                statement.setLong(1, sakId.sakId)
                statement.setString(2, EtteroppgjoerStatus.FERDIGSTILT.name)
                statement.executeQuery().toList { toEtteroppgjoer() }
            }
        }

    fun hentEtteroppgjoerForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer
                        WHERE sak_id = ?
                        AND inntektsaar = ?
                        """.trimIndent(),
                    )
                statement.setLong(1, sakId.sakId)
                statement.setInt(2, inntektsaar)
                statement.executeQuery().singleOrNull { toEtteroppgjoer() }
            }
        }

    fun hentEtteroppgjoerForFilter(
        filter: EtteroppgjoerFilter,
        inntektsaar: Int,
    ): List<Etteroppgjoer> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val sql =
                    """
                    SELECT *
                    FROM etteroppgjoer
                    WHERE inntektsaar = ?
                      AND har_sanksjon = ?
                      AND har_institusjonsopphold = ?
                      AND har_opphoer = ?
                      AND har_bosattutland = ?
                      AND har_adressebeskyttelse = ?
                      AND har_aktivitetskrav = ?
                    """.trimIndent()

                prepareStatement(sql)
                    .apply {
                        setInt(1, inntektsaar)
                        setBoolean(2, filter.harSanksjon)
                        setBoolean(3, filter.harInsitusjonsopphold)
                        setBoolean(4, filter.harOpphoer)
                        setBoolean(5, filter.harBosattUtland)
                        setBoolean(6, filter.harAdressebeskyttelse)
                        setBoolean(7, filter.harAktivitetskrav)
                    }.executeQuery()
                    .toList { toEtteroppgjoer() }
            }
        }

    fun hentEtteroppgjoerForStatus(
        status: EtteroppgjoerStatus,
        inntektsaar: Int,
    ): List<Etteroppgjoer> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer
                        WHERE status = ?
                        AND inntektsaar = ?
                        """.trimIndent(),
                    )
                statement.setString(1, status.name)
                statement.setInt(2, inntektsaar)
                statement.executeQuery().toList { toEtteroppgjoer() }
            }
        }

    fun oppdaterEtteroppgjoerStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    UPDATE etteroppgjoer
                    SET status = ?
                    WHERE sak_id = ? AND inntektsaar = ?
                    """.trimIndent(),
                )

            statement.setString(1, status.name)
            statement.setSakId(2, sakId)
            statement.setInt(3, inntektsaar)

            val updated = statement.executeUpdate()
            krev(updated == 1) { "Kunne ikke lagre etteroppgjør for sakid $sakId" }
        }
    }

    fun lagreEtteroppgjoer(etteroppgjoer: Etteroppgjoer) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO etteroppgjoer(
                            sak_id, inntektsaar, opprettet, status, har_opphoer, har_institusjonsopphold, har_sanksjon, har_bosattutland, har_adressebeskyttelse, har_aktivitetskrav
                        ) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (sak_id, inntektsaar) DO UPDATE SET
                            har_institusjonsopphold = excluded.har_institusjonsopphold,
                            har_bosattutland = excluded.har_bosattutland,
                            har_sanksjon = excluded.har_sanksjon,
                            har_opphoer = excluded.har_opphoer,
                            har_adressebeskyttelse = excluded.har_adressebeskyttelse,
                            har_aktivitetskrav = excluded.har_aktivitetskrav,
                            status = excluded.status
                        """.trimIndent(),
                    )

                with(etteroppgjoer) {
                    statement.setSakId(1, sakId)
                    statement.setInt(2, inntektsaar)
                    statement.setTidspunkt(3, Tidspunkt(java.time.Instant.now()))
                    statement.setString(4, status.name)
                    statement.setBoolean(5, harOpphoer)
                    statement.setBoolean(6, harInstitusjonsopphold)
                    statement.setBoolean(7, harSanksjon)
                    statement.setBoolean(8, harBosattUtland)
                    statement.setBoolean(9, harAdressebeskyttelse)
                    statement.setBoolean(10, harAktivitetskrav)
                    statement.executeUpdate().also {
                        krev(it == 1) {
                            "Kunne ikke lagre etteroppgjør for sakId=$sakId"
                        }
                    }
                }
            }
        }

    private fun ResultSet.toEtteroppgjoer() =
        Etteroppgjoer(
            sakId = SakId(getLong("sak_id")),
            inntektsaar = getInt("inntektsaar"),
            status = EtteroppgjoerStatus.valueOf(getString("status")),
            harOpphoer = getBoolean("har_opphoer"),
            harInstitusjonsopphold = getBoolean("har_institusjonsopphold"),
            harSanksjon = getBoolean("har_sanksjon"),
            harBosattUtland = getBoolean("har_bosattutland"),
            harAdressebeskyttelse = getBoolean("har_adressebeskyttelse"),
            harAktivitetskrav = getBoolean("har_aktivitetskrav"),
        )
}
