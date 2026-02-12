package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.FilterVerdi
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
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
    fun hentEtteroppgjoerMedSvarfristUtloept(svarfrist: EtteroppgjoerSvarfrist): List<Etteroppgjoer> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *  
                        FROM etteroppgjoer e INNER JOIN etteroppgjoer_behandling eb on e.siste_ferdigstilte_forbehandling = eb.id
                        WHERE e.status = ?
                        AND eb.varselbrev_sendt IS NOT NULL
                          AND eb.varselbrev_sendt < (now() - interval '${svarfrist.value}')
                          AND eb.status = ?
                        """.trimIndent(),
                    )
                statement.setString(1, EtteroppgjoerStatus.VENTER_PAA_SVAR.name)
                statement.setString(2, EtteroppgjoerStatus.FERDIGSTILT.name)

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
                val results = statement.executeQuery().toList { toEtteroppgjoer() }

                if (results.isNotEmpty()) {
                    krev(results.size == 1) { "Fant flere Etteroppgjør for inntektsår=$inntektsaar og sakId=$sakId" }
                }

                results.singleOrNull()
            }
        }

    fun hentEtteroppgjoerSakerSomVenterPaaSkatteoppgjoer(antall: Int): List<Etteroppgjoer> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer
                        WHERE status = ?
                        LIMIT ?
                        """.trimIndent(),
                    )
                statement.setString(1, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER.name)
                statement.setInt(2, antall)
                statement.executeQuery().toList { toEtteroppgjoer() }
            }
        }

    fun hentEtteroppgjoerSakerIBulk(
        inntektsaar: Int,
        antall: Int,
        status: EtteroppgjoerStatus,
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
                    WHERE e.status = ?
                    AND e.inntektsaar = ?
                    AND (e.har_sanksjon = ? OR e.har_sanksjon = ?)
                    AND (e.har_institusjonsopphold = ? OR e.har_institusjonsopphold = ?)
                    AND (e.har_opphoer = ? OR e.har_opphoer = ?)
                    AND (e.har_bosatt_utland = ? OR e.har_bosatt_utland = ?)
                    AND (e.har_utlandstilsnitt = ? OR e.har_utlandstilsnitt = ?)
                    AND (e.har_adressebeskyttelse_eller_skjermet = ? OR e.har_adressebeskyttelse_eller_skjermet = ?)
                    AND (e.har_aktivitetskrav = ? OR e.har_aktivitetskrav = ?)
                    AND (e.har_overstyrt_beregning = ? OR e.har_overstyrt_beregning = ?)
                     ${if (spesifikkeSaker.isEmpty()) "" else " AND e.sak_id = ANY(?)"}
                     ${if (ekskluderteSaker.isEmpty()) "" else " AND NOT(e.sak_id = ANY(?))"}
                     ${if (spesifikkeEnheter.isEmpty()) "" else " AND s.enhet = ANY(?)"}
                    AND NOT EXISTS (SELECT 1 FROM oppgave o 
                            WHERE o.type = ? AND o.referanse = '' AND status != ? AND e.sak_id = o.sak_id)
                    ORDER BY sak_id
                    LIMIT ?
                    """.trimIndent(),
                ).apply {
                    var paramIndex = 1

                    fun settFilterVerdier(filterVerdi: FilterVerdi) {
                        setBoolean(paramIndex, filterVerdi.filterEn)
                        paramIndex += 1
                        setBoolean(paramIndex, filterVerdi.filterTo)
                        paramIndex += 1
                    }

                    setString(paramIndex, status.name)
                    paramIndex += 1
                    setInt(paramIndex, inntektsaar)
                    paramIndex += 1

                    settFilterVerdier(etteroppgjoerFilter.harSanksjon)
                    settFilterVerdier(etteroppgjoerFilter.harInstitusjonsopphold)
                    settFilterVerdier(etteroppgjoerFilter.harOpphoer)
                    settFilterVerdier(etteroppgjoerFilter.harBosattUtland)
                    settFilterVerdier(etteroppgjoerFilter.harUtlandstilsnitt)
                    settFilterVerdier(etteroppgjoerFilter.harAdressebeskyttelseEllerSkjermet)
                    settFilterVerdier(etteroppgjoerFilter.harAktivitetskrav)
                    settFilterVerdier(etteroppgjoerFilter.harOverstyrtBeregning)

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
                        paramIndex += 1
                    }
                    setString(paramIndex, OppgaveType.ETTEROPPGJOER.name)
                    paramIndex += 1
                    setString(paramIndex, Status.FERDIGSTILT.name)
                    paramIndex += 1
                    setInt(paramIndex, antall)
                }.executeQuery()
                    .toList {
                        SakId(getLong("sak_id"))
                    }
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
                            sak_id, inntektsaar, opprettet, status, har_opphoer, har_institusjonsopphold, har_sanksjon, har_bosatt_utland, har_utlandstilsnitt, har_adressebeskyttelse_eller_skjermet, har_aktivitetskrav, har_overstyrt_beregning, endret, siste_ferdigstilte_forbehandling
                        ) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (sak_id, inntektsaar) DO UPDATE SET
                            har_institusjonsopphold = excluded.har_institusjonsopphold,
                            har_bosatt_utland = excluded.har_bosatt_utland,
                            har_utlandstilsnitt = excluded.har_utlandstilsnitt,
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
                    statement.setBoolean(9, harUtlandstilsnitt)
                    statement.setBoolean(10, harAdressebeskyttelseEllerSkjermet)
                    statement.setBoolean(11, harAktivitetskrav)
                    statement.setBoolean(12, harOverstyrtBeregning)
                    statement.setTidspunkt(13, Tidspunkt.now())
                    statement.setObject(14, sisteFerdigstilteForbehandling)

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
            harUtlandstilsnitt = getBoolean("har_utlandstilsnitt"),
            harAdressebeskyttelseEllerSkjermet = getBoolean("har_adressebeskyttelse_eller_skjermet"),
            harAktivitetskrav = getBoolean("har_aktivitetskrav"),
            harOverstyrtBeregning = getBoolean("har_overstyrt_beregning"),
            sisteFerdigstilteForbehandling = getObject("siste_ferdigstilte_forbehandling")?.let { it as UUID },
        )
}
