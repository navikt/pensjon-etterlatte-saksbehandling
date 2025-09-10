package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
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
import java.util.UUID

class EtteroppgjoerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentEtteroppgjoerMedSvarfristUtloept(
        inntektsaar: Int,
        svarfrist: EtteroppgjoerSvarfrist,
    ): List<Etteroppgjoer>? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *  
                        FROM etteroppgjoer e INNER JOIN etteroppgjoer_behandling eb on e.siste_ferdigstilte_forbehandling = eb.id
                        WHERE e.status = 'FERDIGSTILT_FORBEHANDLING'
                        AND eb.varselbrev_sendt IS NOT NULL
                          AND eb.varselbrev_sendt < (now() - interval '${svarfrist.value}')
                          AND eb.status = 'FERDIGSTILT'
                          AND eb.aar = ?
                        """.trimIndent(),
                    )
                statement.setInt(1, inntektsaar)

                statement.executeQuery().toList { toEtteroppgjoer() }
            }
        }

    fun oppdaterFerdigstiltForbehandlingId(
        sakId: SakId,
        inntektsaar: Int,
        forbehandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    UPDATE etteroppgjoer
                    SET siste_ferdigstilte_forbehandling = ?
                    WHERE sak_id = ? AND inntektsaar = ?
                    """.trimIndent(),
                )

            statement.setObject(1, forbehandlingId)
            statement.setSakId(2, sakId)
            statement.setInt(3, inntektsaar)

            val updated = statement.executeUpdate()
            krev(updated == 1) { "Kunne ikke oppdatere siste ferdigstilte forbehandling etteroppgjør for sakid $sakId" }
        }
    }

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

    fun hentEtteroppgjoerSakerIBulk(
        inntektsaar: Int,
        antall: Int,
        etteroppgjoerFilter: EtteroppgjoerFilter,
        spesifikkeSaker: List<SakId>,
        ekskluderteSaker: List<SakId>,
    ): List<SakId> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT sak_id FROM etteroppgjoer e 
                    WHERE e.status = 'MOTTATT_SKATTEOPPGJOER'
                    AND e.inntektsaar = ?
                    AND har_sanksjon = ?
                    AND har_institusjonsopphold = ?
                    AND har_opphoer = ?
                    AND har_bosatt_utland = ?
                    AND har_adressebeskyttelse_eller_skjermet = ?
                    AND har_aktivitetskrav = ?
                     ${if (spesifikkeSaker.isEmpty()) "" else " AND sak_id = ANY(?)"}
                     ${if (ekskluderteSaker.isEmpty()) "" else " AND sak_id = ANY(?)"}
                    ORDER BY sak_id
                    LIMIT $antall
                    """.trimIndent(),
                ).apply {
                    setInt(1, inntektsaar)
                    setBoolean(2, etteroppgjoerFilter.harSanksjon)
                    setBoolean(3, etteroppgjoerFilter.harInsitusjonsopphold)
                    setBoolean(4, etteroppgjoerFilter.harOpphoer)
                    setBoolean(5, etteroppgjoerFilter.harBosattUtland)
                    setBoolean(6, etteroppgjoerFilter.harAdressebeskyttelseEllerSkjermet)
                    setBoolean(7, etteroppgjoerFilter.harAktivitetskrav)

                    if (spesifikkeSaker.isNotEmpty()) {
                        setArray(8, createArrayOf("bigint", spesifikkeSaker.toTypedArray()))
                    }
                    if (ekskluderteSaker.isNotEmpty()) {
                        setArray(9, createArrayOf("bigint", spesifikkeSaker.toTypedArray()))
                    }
                }.executeQuery()
                    .toList {
                        SakId(getLong("sak_id"))
                    }
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
                      AND har_bosatt_utland = ?
                      AND har_adressebeskyttelse_eller_skjermet = ?
                      AND har_aktivitetskrav = ?
                    """.trimIndent()

                prepareStatement(sql)
                    .apply {
                        setInt(1, inntektsaar)
                        setBoolean(2, filter.harSanksjon)
                        setBoolean(3, filter.harInsitusjonsopphold)
                        setBoolean(4, filter.harOpphoer)
                        setBoolean(5, filter.harBosattUtland)
                        setBoolean(6, filter.harAdressebeskyttelseEllerSkjermet)
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
                            sak_id, inntektsaar, opprettet, status, har_opphoer, har_institusjonsopphold, har_sanksjon, har_bosatt_utland, har_adressebeskyttelse_eller_skjermet, har_aktivitetskrav, endret, siste_ferdigstilte_forbehandling
                        ) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (sak_id, inntektsaar) DO UPDATE SET
                            har_institusjonsopphold = excluded.har_institusjonsopphold,
                            har_bosatt_utland = excluded.har_bosatt_utland,
                            har_sanksjon = excluded.har_sanksjon,
                            har_opphoer = excluded.har_opphoer,
                            har_adressebeskyttelse_eller_skjermet = excluded.har_adressebeskyttelse_eller_skjermet,
                            har_aktivitetskrav = excluded.har_aktivitetskrav,
                            status = excluded.status,
                            endret = excluded.endret,
                            siste_ferdigstilte_forbehandling  = excluded.siste_ferdigstilte_forbehandling
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
                    statement.setBoolean(9, harAdressebeskyttelseEllerSkjermet)
                    statement.setBoolean(10, harAktivitetskrav)
                    statement.setTidspunkt(11, Tidspunkt.now())
                    statement.setObject(12, sisteFerdigstilteForbehandling)

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
            harBosattUtland = getBoolean("har_bosatt_utland"),
            harAdressebeskyttelseEllerSkjermet = getBoolean("har_adressebeskyttelse_eller_skjermet"),
            harAktivitetskrav = getBoolean("har_aktivitetskrav"),
            sisteFerdigstilteForbehandling = getObject("siste_ferdigstilte_forbehandling")?.let { it as UUID },
        )
}
