package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.ResultSet
import java.util.UUID

class EtteroppgjoerDao(
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
                statement.executeQuery().singleOrNull { toEtteroppgjoer() }
            }
        }

    fun lagreForbehandling(etteroppgjoer: EtteroppgjoerForbehandling) =
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
                statement.setObject(1, etteroppgjoer.id)
                statement.setString(2, etteroppgjoer.status)
                statement.setSakId(3, etteroppgjoer.sak.id)
                statement.setTidspunkt(4, etteroppgjoer.opprettet)
                statement.executeUpdate().also {
                    krev(it == 1) {
                        "Kunne ikke lagre forbehandling etteroppgjør for sakid ${etteroppgjoer.sak.id}"
                    }
                }
            }
        }

    private fun ResultSet.toEtteroppgjoer(): EtteroppgjoerForbehandling =
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

    fun lagreOpplysningerSkatt(skatt: PensjonsgivendeInntektFraSkatt) {
        // TODO("Not yet implemented")
    }

    fun hentOpplysningerSkatt(behandlingId: UUID): PensjonsgivendeInntektFraSkatt = PensjonsgivendeInntektFraSkatt.stub()

    fun lagreOpplysningerAInntekt(aInntekt: AInntekt) {
        // TODO("Not yet implemented")
    }

    fun hentOpplysningerAInntekt(behandlingId: UUID): AInntekt = AInntekt.stub()

    fun hentEtteroppgjoer(ident: String): Etteroppgjoer? {
        // TODO
        return null
    }

    fun oppdaterEtteroppgjoerStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        TODO("Not yet implemented")
    }
}
