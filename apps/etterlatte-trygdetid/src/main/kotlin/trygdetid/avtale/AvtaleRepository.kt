package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import sun.security.util.ResourcesMgr.getString
import java.util.UUID
import javax.sql.DataSource

class AvtaleRepository(private val dataSource: DataSource) {
    fun hentAvtale(behandlingId: UUID): Trygdeavtale? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT id, behandling_id, avtale_kode, avtale_dato_kode, avtale_kriteria_kode, personKrets, arbInntekt1G, arbInntekt1GKommentar, beregArt50, beregArt50Kommentar, nordiskTrygdeAvtale, nordiskTrygdeAvtaleKommentar, kilde
                    FROM trygdeavtale
                    WHERE behandling_id = :behandlingId
                    """.trimIndent(),
                paramMap = mapOf("behandlingId" to behandlingId),
            ).let { query ->
                session.run(
                    query.map { row ->
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
                    personKrets = :personKrets,
                    arbInntekt1G = :arbInntekt1G,
                    arbInntekt1GKommentar = :arbInntekt1GKommentar,
                    beregArt50 = :beregArt50,
                    beregArt50Kommentar = :beregArt50Kommentar,
                    nordiskTrygdeAvtale = :nordiskTrygdeAvtale,
                    nordiskTrygdeAvtaleKommentar = :nordiskTrygdeAvtaleKommentar,
                    kilde = :kilde,
                    WHERE
                    id = :id AND behandling_id = :behandlingId
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to trygdeavtale.id,
                        "behandlingId" to trygdeavtale.behandlingId,
                        "avtaleKode" to trygdeavtale.avtaleKode,
                        "avtaleDatoKode" to trygdeavtale.avtaleDatoKode,
                        "avtaleKriteriaKode" to trygdeavtale.avtaleKriteriaKode,
                        "arbInntekt1G" to trygdeavtale.arbInntekt1G,
                        "arbInntekt1GKommentar" to trygdeavtale.arbInntekt1GKommentar,
                        "beregArt50" to trygdeavtale.beregArt50,
                        "beregArt50Kommentar" to trygdeavtale.beregArt50Kommentar,
                        "nordiskTrygdeAvtale" to trygdeavtale.nordiskTrygdeAvtale,
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
                    INSERT INTO trygdeavtale(id, behandling_id, avtale_kode, avtale_dato_kode, avtale_kriteria_kode, arbInntekt1G, arbInntekt1GKommentar, beregArt50, beregArt50Kommentar, nordiskTrygdeAvtale, nordiskTrygdeAvtaleKommentar, kilde)
                    VALUES(:id, :behandlingId, :avtaleKode, :avtaleDatoKode, :avtaleKriteriaKode, :arbInntekt1G, :arbInntekt1GKommentar, :beregArt50, :beregArt50Kommentar, :nordiskTrygdeAvtale, :nordiskTrygdeAvtaleKommentar, :kilde)
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to trygdeavtale.id,
                        "behandlingId" to trygdeavtale.behandlingId,
                        "avtaleKode" to trygdeavtale.avtaleKode,
                        "avtaleDatoKode" to trygdeavtale.avtaleDatoKode,
                        "avtaleKriteriaKode" to trygdeavtale.avtaleKriteriaKode,
                        "arbInntekt1G" to trygdeavtale.arbInntekt1G,
                        "arbInntekt1GKommentar" to trygdeavtale.arbInntekt1GKommentar,
                        "beregArt50" to trygdeavtale.beregArt50,
                        "beregArt50Kommentar" to trygdeavtale.beregArt50Kommentar,
                        "nordiskTrygdeAvtale" to trygdeavtale.nordiskTrygdeAvtale,
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
            personKrets = getString("personKrets").let { JaNei.valueOf(it) },
            arbInntekt1G = getString("arbInntekt1G").let { JaNei.valueOf(it) },
            arbInntekt1GKommentar = stringOrNull("arbInntekt1GKommentar"),
            beregArt50 = getString("beregArt50").let { JaNei.valueOf(it) },
            beregArt50Kommentar = stringOrNull("beregArt50Kommentar"),
            nordiskTrygdeAvtale = getString("nordiskTrygdeAvtale").let { JaNei.valueOf(it) },
            nordiskTrygdeAvtaleKommentar = stringOrNull("nordiskTrygdeAvtaleKommentar"),
            kilde = string("kilde").let { objectMapper.readValue(it) },
        )
}
