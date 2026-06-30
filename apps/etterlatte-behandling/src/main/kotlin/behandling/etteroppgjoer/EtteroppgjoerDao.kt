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

    fun hentEtteroppgjoerForSak(sakId: SakId): List<Etteroppgjoer> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer
                        WHERE sak_id = ?
                        """.trimIndent(),
                    )
                statement.setLong(1, sakId.sakId)
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
                val results = statement.executeQuery().toList { toEtteroppgjoer() }

                if (results.isNotEmpty()) {
                    krev(results.size == 1) { "Fant flere Etteroppgjør for inntektsår=$inntektsaar og sakId=$sakId" }
                }

                results.singleOrNull()
            }
        }

    fun hentEtteroppgjoerSomVenterPaaSkatteoppgjoer(antall: Int): List<Etteroppgjoer> =
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
