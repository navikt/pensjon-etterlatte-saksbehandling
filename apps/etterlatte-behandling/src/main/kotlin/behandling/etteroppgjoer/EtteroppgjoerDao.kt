package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
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
                        WHERE e.status = 'VENTER_PAA_SVAR'
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

    fun hentAktivtEtteroppgjoerForSak(sakId: SakId): Etteroppgjoer? =
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

                val etteroppgjoer = statement.executeQuery().toList { toEtteroppgjoer() }

                // TODO: håndtere flere aktive etteroppgjoer for sak
                krev(etteroppgjoer.size < 2) { "Fant ${etteroppgjoer.size} aktive etteroppgjoer for sak $sakId, forventet 1" }
                etteroppgjoer.firstOrNull()
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
        spesifikkeEnheter: List<String>,
    ): List<SakId> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT sak_id FROM etteroppgjoer e
                    INNER JOIN sak s on s.id = e.sak_id
                    WHERE e.status = 'MOTTATT_SKATTEOPPGJOER'
                    AND e.inntektsaar = ?
                    AND e.har_sanksjon = ?
                    AND e.har_institusjonsopphold = ?
                    AND e.har_opphoer = ?
                    AND e.har_bosatt_utland = ?
                    AND e.har_adressebeskyttelse_eller_skjermet = ?
                    AND e.har_aktivitetskrav = ?
                    AND e.har_overstyrt_beregning = ?
                     ${if (spesifikkeSaker.isEmpty()) "" else " AND e.sak_id = ANY(?)"}
                     ${if (ekskluderteSaker.isEmpty()) "" else " AND NOT(e.sak_id = ANY(?))"}
                     ${if (spesifikkeEnheter.isEmpty()) "" else " AND s.enhet = ANY(?)"}
                    ORDER BY sak_id
                    LIMIT $antall
                    """.trimIndent(),
                ).apply {
                    var paramIndex = 1
                    setInt(paramIndex, inntektsaar)
                    paramIndex += 1
                    setBoolean(paramIndex, etteroppgjoerFilter.harSanksjon)
                    paramIndex += 1
                    setBoolean(paramIndex, etteroppgjoerFilter.harInsitusjonsopphold)
                    paramIndex += 1
                    setBoolean(paramIndex, etteroppgjoerFilter.harOpphoer)
                    paramIndex += 1
                    setBoolean(paramIndex, etteroppgjoerFilter.harBosattUtland)
                    paramIndex += 1
                    setBoolean(paramIndex, etteroppgjoerFilter.harAdressebeskyttelseEllerSkjermet)
                    paramIndex += 1
                    setBoolean(paramIndex, etteroppgjoerFilter.harAktivitetskrav)
                    paramIndex += 1
                    setBoolean(paramIndex, etteroppgjoerFilter.harOverstyrtBeregning)
                    paramIndex += 1

                    if (spesifikkeSaker.isNotEmpty()) {
                        setArray(paramIndex, createArrayOf("bigint", spesifikkeSaker.toTypedArray()))
                        paramIndex += 1
                    }

                    if (ekskluderteSaker.isNotEmpty()) {
                        setArray(paramIndex, createArrayOf("bigint", ekskluderteSaker.toTypedArray()))
                        paramIndex += 1
                    }

                    if (spesifikkeEnheter.isNotEmpty()) {
                        setArray(paramIndex, createArrayOf("text", spesifikkeEnheter.toTypedArray()))
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
                      AND har_overstyrt_beregning = ?
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
                        setBoolean(8, filter.harOverstyrtBeregning)
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
                            sak_id, inntektsaar, opprettet, status, har_opphoer, har_institusjonsopphold, har_sanksjon, har_bosatt_utland, har_adressebeskyttelse_eller_skjermet, har_aktivitetskrav, har_overstyrt_beregning, endret, siste_ferdigstilte_forbehandling
                        ) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (sak_id, inntektsaar) DO UPDATE SET
                            har_institusjonsopphold = excluded.har_institusjonsopphold,
                            har_bosatt_utland = excluded.har_bosatt_utland,
                            har_sanksjon = excluded.har_sanksjon,
                            har_opphoer = excluded.har_opphoer,
                            har_adressebeskyttelse_eller_skjermet = excluded.har_adressebeskyttelse_eller_skjermet,
                            har_aktivitetskrav = excluded.har_aktivitetskrav,
                            har_overstyrt_beregning = excluded.har_overstyrt_beregning,
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
                    statement.setBoolean(11, harOverstyrtBeregning)
                    statement.setTidspunkt(12, Tidspunkt.now())
                    statement.setObject(13, sisteFerdigstilteForbehandling)

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
            harOverstyrtBeregning = getBoolean("har_overstyrt_beregning"),
            sisteFerdigstilteForbehandling = getObject("siste_ferdigstilte_forbehandling")?.let { it as UUID },
        )
}
