package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import java.util.UUID
import javax.sql.DataSource

class AvtaleRepository(
    private val dataSource: DataSource,
) {
    fun hentAvtale(behandlingId: UUID): Trygdeavtale? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT id, behandling_id, avtale_kode, avtale_dato_kode, avtale_kriteria_kode, person_krets, arb_inntekt, arb_inntekt_kommentar, bereg_art, bereg_art_kommentar, nordisk_trygdeAvtale, nordisk_trygdeavtale_kommentar, kilde
                    FROM trygdeavtale
                    WHERE behandling_id = :behandlingId
                    """.trimIndent(),
                paramMap = mapOf("behandlingId" to behandlingId),
            ).let { query ->
                session.run(
                    query
                        .map { row ->
                            row.toTrygdeavtale()
                        }.asSingle,
                )
            }
        }

    fun lagreAvtale(trygdeavtale: Trygdeavtale) {
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    UPDATE trygdeavtale
                    SET
                    avtale_kode = :avtaleKode,
                    avtale_dato_kode = :avtaleDatoKode,
                    avtale_kriteria_kode = :avtaleKriteriaKode,
                    person_krets = :personKrets,
                    arb_inntekt = :arbInntekt1G,
                    arb_inntekt_kommentar = :arbInntekt1GKommentar,
                    bereg_art = :beregArt50,
                    bereg_art_kommentar = :beregArt50Kommentar,
                    nordisk_trygdeavtale = :nordiskTrygdeAvtale,
                    nordisk_trygdeavtale_kommentar = :nordiskTrygdeAvtaleKommentar,
                    kilde = :kilde
                    WHERE id = :id AND behandling_id = :behandlingId
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to trygdeavtale.id,
                        "behandlingId" to trygdeavtale.behandlingId,
                        "avtaleKode" to trygdeavtale.avtaleKode,
                        "avtaleDatoKode" to trygdeavtale.avtaleDatoKode,
                        "avtaleKriteriaKode" to trygdeavtale.avtaleKriteriaKode,
                        "personKrets" to trygdeavtale.personKrets?.name,
                        "arbInntekt1G" to trygdeavtale.arbInntekt1G?.name,
                        "arbInntekt1GKommentar" to trygdeavtale.arbInntekt1GKommentar,
                        "beregArt50" to trygdeavtale.beregArt50?.name,
                        "beregArt50Kommentar" to trygdeavtale.beregArt50Kommentar,
                        "nordiskTrygdeAvtale" to trygdeavtale.nordiskTrygdeAvtale?.name,
                        "nordiskTrygdeAvtaleKommentar" to trygdeavtale.nordiskTrygdeAvtaleKommentar,
                        "kilde" to trygdeavtale.kilde.toJson(),
                    ),
            ).let { query ->
                session.execute(query)
            }
        }
    }

    fun opprettAvtale(trygdeavtale: Trygdeavtale) {
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    INSERT INTO trygdeavtale(id, behandling_id, avtale_kode, avtale_dato_kode, avtale_kriteria_kode, arb_inntekt, arb_inntekt_kommentar, bereg_art, bereg_art_kommentar, nordisk_trygdeavtale, nordisk_trygdeavtale_kommentar, kilde)
                    VALUES(:id, :behandlingId, :avtaleKode, :avtaleDatoKode, :avtaleKriteriaKode, :arbInntekt1G, :arbInntekt1GKommentar, :beregArt50, :beregArt50Kommentar, :nordiskTrygdeAvtale, :nordiskTrygdeAvtaleKommentar, :kilde)
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to trygdeavtale.id,
                        "behandlingId" to trygdeavtale.behandlingId,
                        "avtaleKode" to trygdeavtale.avtaleKode,
                        "avtaleDatoKode" to trygdeavtale.avtaleDatoKode,
                        "avtaleKriteriaKode" to trygdeavtale.avtaleKriteriaKode,
                        "personKrets" to trygdeavtale.personKrets?.name,
                        "arbInntekt1G" to trygdeavtale.arbInntekt1G?.name,
                        "arbInntekt1GKommentar" to trygdeavtale.arbInntekt1GKommentar,
                        "beregArt50" to trygdeavtale.beregArt50?.name,
                        "beregArt50Kommentar" to trygdeavtale.beregArt50Kommentar,
                        "nordiskTrygdeAvtale" to trygdeavtale.nordiskTrygdeAvtale?.name,
                        "nordiskTrygdeAvtaleKommentarto" to trygdeavtale.nordiskTrygdeAvtaleKommentar,
                        "kilde" to trygdeavtale.kilde.toJson(),
                    ),
            ).let { query ->
                session.execute(query)
            }
        }
    }

    private fun Row.toTrygdeavtale() =
        Trygdeavtale(
            id = uuid("id"),
            behandlingId = uuid("behandling_id"),
            avtaleKode = string("avtale_kode"),
            avtaleDatoKode = stringOrNull("avtale_dato_kode"),
            avtaleKriteriaKode = stringOrNull("avtale_kriteria_kode"),
            personKrets = stringOrNull("person_krets")?.let { JaNei.valueOf(it) },
            arbInntekt1G = stringOrNull("arb_inntekt")?.let { JaNei.valueOf(it) },
            arbInntekt1GKommentar = stringOrNull("arb_inntekt_kommentar"),
            beregArt50 = stringOrNull("bereg_art")?.let { JaNei.valueOf(it) },
            beregArt50Kommentar = stringOrNull("bereg_art_kommentar"),
            nordiskTrygdeAvtale = stringOrNull("nordisk_trygdeavtale")?.let { JaNei.valueOf(it) },
            nordiskTrygdeAvtaleKommentar = stringOrNull("nordisk_trygdeavtale_kommentar"),
            kilde = string("kilde").let { objectMapper.readValue(it) },
        )
}
