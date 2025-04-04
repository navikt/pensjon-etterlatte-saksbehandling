package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.hendelse.getLongOrNull
import no.nav.etterlatte.behandling.hendelse.setLong
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Date
import java.sql.ResultSet
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerForbehandlingDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, t.opprettet, t.status, t.aar, t.fom, t.tom, t.brev_id
                        FROM etteroppgjoer_behandling t INNER JOIN sak s on t.sak_id = s.id
                        WHERE t.id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull { toForbehandling() }
            }
        }

    fun hentForbehandlinger(sakId: SakId): List<EtteroppgjoerForbehandling> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, t.opprettet, t.status, t.aar, t.fom, t.tom, t.brev_id
                        FROM etteroppgjoer_behandling t INNER JOIN sak s on t.sak_id = s.id
                        WHERE t.sak_id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, sakId.sakId)
                statement.executeQuery().toList { toForbehandling() }
            }
        }

    fun lagreForbehandling(forbehandling: EtteroppgjoerForbehandling) =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO etteroppgjoer_behandling(
                            id, status, sak_id, opprettet, aar, fom, tom, brev_id
                        ) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (id) DO UPDATE SET
                            status = excluded.status,
                            brev_id = excluded.brev_id
                        """.trimIndent(),
                    )
                statement.setObject(1, forbehandling.id)
                statement.setString(2, forbehandling.status)
                statement.setSakId(3, forbehandling.sak.id)
                statement.setTidspunkt(4, forbehandling.opprettet)
                statement.setInt(5, forbehandling.aar)
                statement.setDate(6, Date.valueOf(forbehandling.innvilgetPeriode.fom.atDay(1)))
                statement.setDate(
                    7,
                    Date.valueOf(
                        forbehandling.innvilgetPeriode.tom?.atEndOfMonth()
                            ?: throw InternfeilException("Etteroppgjoer forbehandling mangler periode"),
                    ),
                )
                statement.setLong(8, forbehandling.brevId)
                statement.executeUpdate().also {
                    krev(it == 1) {
                        "Kunne ikke lagre forbehandling etteroppgjør for sakId=${forbehandling.sak.id}"
                    }
                }
            }
        }

    fun lagrePensjonsgivendeInntekt(
        inntekterFraSkatt: PensjonsgivendeInntektFraSkatt,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO pensjonsgivendeinntekt_fra_skatt(
                        id, forbehandling_id, inntektsaar, skatteordning, loensinntekt, naeringsinntekt,fiske_fangst_familiebarnehage
                    ) 
                    VALUES (?, ?, ?, ?, ?, ?, ?) 
                    """.trimIndent(),
                )

            for (inntekt in inntekterFraSkatt.inntekter) {
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, behandlingId)
                statement.setInt(3, inntekt.inntektsaar)
                statement.setString(4, inntekt.skatteordning)
                statement.setInt(5, inntekt.loensinntekt)
                statement.setInt(6, inntekt.naeringsinntekt)
                statement.setInt(7, inntekt.fiskeFangstFamiliebarnehage)

                statement.addBatch()
            }

            val result = statement.executeBatch()
            krev(result.size == inntekterFraSkatt.inntekter.size) {
                "Kunne ikke lagre alle pensjonsgivendeInntekter for behandlingId=$behandlingId"
            }
        }
    }

    fun hentPensjonsgivendeInntekt(forbehandlingId: UUID): PensjonsgivendeInntektFraSkatt? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM pensjonsgivendeinntekt_fra_skatt
                        WHERE forbehandling_id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, forbehandlingId)
                val pensjonsgivendeInntekter =
                    statement.executeQuery().toList {
                        toPensjonsgivendeInntekt()
                    }

                if (pensjonsgivendeInntekter.isEmpty()) {
                    null
                } else {
                    PensjonsgivendeInntektFraSkatt(
                        inntektsaar = pensjonsgivendeInntekter.first().inntektsaar,
                        inntekter = pensjonsgivendeInntekter,
                    )
                }
            }
        }

    fun lagreAInntekt(
        aInntekt: AInntekt,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO inntekt_fra_ainntekt(
                        id, forbehandling_id, aar, inntektsmaaneder
                    ) 
                    VALUES (?, ?, ?, ?) 
                    """.trimIndent(),
                )

            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, behandlingId)
            statement.setInt(3, aInntekt.aar)
            statement.setJsonb(4, aInntekt.inntektsmaaneder)

            statement.executeUpdate().also {
                krev(it == 1) {
                    "Kunne ikke lagre aInntekt for behandling=$behandlingId"
                }
            }
        }
    }

    fun hentAInntekt(behandlingId: UUID): AInntekt? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM inntekt_fra_ainntekt
                        WHERE forbehandling_id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull {
                    AInntekt(
                        aar = getInt("aar"),
                        inntektsmaaneder = getString("inntektsmaaneder").let { objectMapper.readValue(it) },
                    )
                }
            }
        }

    private fun ResultSet.toForbehandling(): EtteroppgjoerForbehandling =
        EtteroppgjoerForbehandling(
            id = getString("id").let { UUID.fromString(it) },
            hendelseId = UUID.randomUUID(), // TODO
            sak =
                Sak(
                    id = SakId(getLong("sak_id")),
                    sakType = enumValueOf(getString("saktype")),
                    ident = getString("fnr"),
                    enhet = Enhetsnummer(getString("enhet")),
                ),
            // sekvensnummerSkatt = "123", // TODO
            opprettet = getTidspunkt("opprettet"),
            status = getString("status"),
            aar = getInt("aar"),
            innvilgetPeriode =
                Periode(
                    fom = getDate("fom").let { YearMonth.from(it.toLocalDate()) },
                    tom = getDate("tom").let { YearMonth.from(it.toLocalDate()) },
                ),
            brevId = getLongOrNull("brev_id"),
        )

    private fun ResultSet.toPensjonsgivendeInntekt(): PensjonsgivendeInntekt =
        PensjonsgivendeInntekt(
            skatteordning = getString("skatteordning"),
            loensinntekt = getInt("loensinntekt"),
            naeringsinntekt = getInt("naeringsinntekt"),
            fiskeFangstFamiliebarnehage = getInt("fiske_fangst_familiebarnehage"),
            inntektsaar = getInt("inntektsaar"),
        )
}
