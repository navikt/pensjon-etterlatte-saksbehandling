package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.AInntektMaaned
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
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
                        SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, t.opprettet, t.status
                        FROM etteroppgjoer_behandling t INNER JOIN sak s on t.sak_id = s.id
                        WHERE t.id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull { toForbehandling() }
            }
        }

    fun lagreForbehandling(forbehandling: EtteroppgjoerForbehandling) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO etteroppgjoer_behandling(
                            id, status, sak_id, opprettet
                        ) 
                        VALUES (?, ?, ?, ?) 
                        ON CONFLICT (id) DO UPDATE SET
                            status = excluded.status
                        """.trimIndent(),
                    )
                statement.setObject(1, forbehandling.id)
                statement.setString(2, forbehandling.status)
                statement.setSakId(3, forbehandling.sak.id)
                statement.setTidspunkt(4, forbehandling.opprettet)
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
                        id, forbehandling_id, maaned, inntekter, summert_beloep
                    ) 
                    VALUES (?, ?, ?, ?, ?) 
                    """.trimIndent(),
                )

            for (inntektsmaaned in aInntekt.inntektsmaaneder) {
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, behandlingId)
                statement.setString(3, inntektsmaaned.maaned.toString())
                statement.setString(4, objectMapper.writeValueAsString(inntektsmaaned.inntekter))
                statement.setBigDecimal(5, inntektsmaaned.summertBeloep)

                statement.addBatch()
            }

            val result = statement.executeBatch()
            krev(result.size == aInntekt.inntektsmaaneder.size) {
                "Kunne ikke lagre alle inntekter fra aInntekt for behandlingId=$behandlingId"
            }
        }
    }

    fun hentAInntekt(forbehandlingId: UUID): AInntekt? =
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
                statement.setObject(1, forbehandlingId)
                val inntekterFraAInntekt =
                    statement.executeQuery().toList {
                        toAInntektMaaned()
                    }

                if (inntekterFraAInntekt.isEmpty()) {
                    null
                } else {
                    AInntekt(
                        aar = inntekterFraAInntekt.first().maaned.year,
                        inntektsmaaneder = inntekterFraAInntekt,
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
            aar = 2024,
        )

    private fun ResultSet.toPensjonsgivendeInntekt(): PensjonsgivendeInntekt =
        PensjonsgivendeInntekt(
            skatteordning = getString("skatteordning"),
            loensinntekt = getInt("loensinntekt"),
            naeringsinntekt = getInt("naeringsinntekt"),
            fiskeFangstFamiliebarnehage = getInt("fiske_fangst_familiebarnehage"),
            inntektsaar = getInt("inntektsaar"),
        )

    private fun ResultSet.toAInntektMaaned(): AInntektMaaned =
        AInntektMaaned(
            maaned = YearMonth.parse(getString("maaned")),
            inntekter = objectMapper.readValue(getString("inntekter")),
            summertBeloep = getBigDecimal("summert_beloep"),
        )
}
