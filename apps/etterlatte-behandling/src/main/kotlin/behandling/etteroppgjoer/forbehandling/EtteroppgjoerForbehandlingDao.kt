package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
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
                        "Kunne ikke lagre forbehandling etteroppgjør for sakid ${forbehandling.sak.id}"
                    }
                }
            }
        }

    fun lagrePensjonsgivendeInntektFraSkatt(
        inntekterFraSkatt: PensjonsgivendeInntektFraSkatt,
        forbehandlingsId: UUID,
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
                statement.setObject(2, forbehandlingsId)
                statement.setInt(3, inntekt.inntektsaar)
                statement.setString(4, inntekt.skatteordning)
                statement.setInt(5, inntekt.loensinntekt)
                statement.setInt(6, inntekt.naeringsinntekt)
                statement.setInt(7, inntekt.fiskeFangstFamiliebarnehage)

                statement.addBatch()
            }

            val result = statement.executeBatch()
            krev(result.size == inntekterFraSkatt.inntekter.size) {
                "Kunne ikke lagre alle inntekter for forbehandlingsId $forbehandlingsId"
            }
        }
    }

    fun hentPensjonsgivendeInntektFraSkatt(forbehandlingId: UUID): PensjonsgivendeInntektFraSkatt? =
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

    fun lagreOpplysningerAInntekt(aInntekt: AInntekt) {
        // TODO("Not yet implemented")
    }

    fun hentOpplysningerAInntekt(behandlingId: UUID): AInntekt = AInntekt.stub()

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
}
