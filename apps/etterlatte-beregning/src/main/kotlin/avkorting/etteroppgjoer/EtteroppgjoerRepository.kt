package no.nav.etterlatte.avkorting.etteroppgjoer

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import java.util.UUID
import javax.sql.DataSource

class EtteroppgjoerRepository(
    private val dataSource: DataSource,
) {
    fun lagreEtteroppgjoerResultat(resultat: BeregnetEtteroppgjoerResultat) =
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    INSERT INTO etteroppgjoer_beregnet_resultat(id, aar, siste_iverksatte_behandling_id, forbehandling_id, utbetalt_stoenad, ny_brutto_stoenad, differanse, grense, resultat_type,
                            tidspunkt, regel_resultat, kilde, referanse_avkorting_sist_iverksatte, har_ingen_inntekt,
                            referanse_avkorting_forbehandling)
                    VALUES (:id, :aar, :siste_iverksatte_behandling_id, :forbehandling_id, :utbetalt_stoenad, :ny_brutto_stoenad, :differanse, :grense, :resultat_type, :tidspunkt, :regel_resultat,
                            :kilde, :referanse_avkorting_sist_iverksatte, :har_ingen_inntekt, :referanse_avkorting_forbehandling)
                    ON CONFLICT (
                        aar, siste_iverksatte_behandling_id, forbehandling_id
                        ) DO UPDATE SET utbetalt_stoenad = excluded.utbetalt_stoenad,
                                ny_brutto_stoenad = excluded.ny_brutto_stoenad,
                                differanse = excluded.differanse,
                                grense = excluded.grense,
                                resultat_type = excluded.resultat_type,
                                tidspunkt = excluded.tidspunkt,
                                har_ingen_inntekt = excluded.har_ingen_inntekt,
                                regel_resultat = excluded.regel_resultat,
                                kilde = excluded.kilde
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to resultat.id,
                        "aar" to resultat.aar,
                        "siste_iverksatte_behandling_id" to resultat.sisteIverksatteBehandlingId,
                        "forbehandling_id" to resultat.forbehandlingId,
                        "utbetalt_stoenad" to resultat.utbetaltStoenad,
                        "ny_brutto_stoenad" to resultat.nyBruttoStoenad,
                        "differanse" to resultat.differanse,
                        "grense" to resultat.grense.toJson(),
                        "resultat_type" to resultat.resultatType.name,
                        "tidspunkt" to resultat.tidspunkt.toTimestamp(),
                        "regel_resultat" to resultat.regelResultat.toJson(),
                        "kilde" to resultat.kilde.toJson(),
                        "har_ingen_inntekt" to resultat.harIngenInntekt,
                        "referanse_avkorting_sist_iverksatte" to resultat.referanseAvkorting.avkortingSisteIverksatte,
                        "referanse_avkorting_forbehandling" to resultat.referanseAvkorting.avkortingForbehandling,
                    ),
            ).let { query -> tx.run(query.asUpdate) }
                .let { antallOppdatert ->
                    krev(
                        antallOppdatert == 1,
                    ) {
                        "Kunne ikke lagre etteroppgjør resultat, referanse avkorting=${resultat.referanseAvkorting}"
                    }
                }
        }

    fun hentEtteroppgjoerResultat(
        aar: Int,
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
    ): BeregnetEtteroppgjoerResultat? =
        dataSource.transaction { tx ->
            queryOf(
                """
                SELECT * FROM etteroppgjoer_beregnet_resultat 
                WHERE aar = :aar 
                    AND siste_iverksatte_behandling_id = :siste_iverksatte_behandling_id 
                    AND forbehandling_id = :forbehandling_id
                """.trimIndent(),
                paramMap =
                    mapOf(
                        "aar" to aar,
                        "siste_iverksatte_behandling_id" to sisteIverksatteBehandlingId,
                        "forbehandling_id" to forbehandlingId,
                    ),
            ).let { query ->
                val results = tx.run(query.map { row -> row.toBeregnetEtteroppgjoerResultat() }.asList)
                when {
                    results.isEmpty() -> null
                    results.size == 1 -> results.first()
                    else -> throw IllegalStateException(
                        "Forventet maks én rad, men fikk ${results.size} for forbehandling med id $forbehandlingId",
                    )
                }
            }
        }

    private fun Row.toBeregnetEtteroppgjoerResultat(): BeregnetEtteroppgjoerResultat =
        BeregnetEtteroppgjoerResultat(
            id = uuid("id"),
            aar = int("aar"),
            forbehandlingId = uuid("forbehandling_id"),
            sisteIverksatteBehandlingId = uuid("siste_iverksatte_behandling_id"),
            utbetaltStoenad = long("utbetalt_stoenad"),
            nyBruttoStoenad = long("ny_brutto_stoenad"),
            differanse = long("differanse"),
            grense = objectMapper.readValue(this.string("grense")),
            resultatType = EtteroppgjoerResultatType.valueOf(string("resultat_type")),
            tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
            regelResultat = objectMapper.readValue(this.string("regel_resultat")),
            kilde = objectMapper.readValue(this.string("kilde")),
            harIngenInntekt = boolean("har_ingen_inntekt"),
            referanseAvkorting =
                ReferanseEtteroppgjoer(
                    avkortingForbehandling = uuid("referanse_avkorting_forbehandling"),
                    avkortingSisteIverksatte = uuid("referanse_avkorting_sist_iverksatte"),
                ), // TODO: introdusere nytt felt for å lagre referanse til vedtak brukt i utbetalt stoenad
        )
}
