package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import java.util.*
import javax.sql.DataSource

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: UUID): VilkaarsvurderingDao?
    fun lagre(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao
}

class VilkaarsvurderingRepositoryImpl(private val ds: DataSource) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: UUID): VilkaarsvurderingDao? =
        using(sessionOf(ds)) { session ->
            queryOf(
                statement = Queries.hentVilkaarsvurdering,
                paramMap = mapOf("behandlingId" to behandlingId)
            )
                .let { query ->
                    session.run(
                        query.map { row ->
                            VilkaarsvurderingDao(
                                behandlingId = row.uuid("behandlingId"),
                                payload = row.string("payload").let { payload ->
                                    objectMapper.readValue(payload)
                                },
                                vilkaar = row.string("vilkaar").let { vilkaar ->
                                    objectMapper.readValue(vilkaar)
                                },
                                resultat = row.stringOrNull("resultat").let { resultat ->
                                    resultat?.let { objectMapper.readValue(it) }
                                },
                                virkningstidspunkt = row.localDate("virkningstidspunkt")
                            )
                        }.asSingle
                    )
                }
        }

    override fun lagre(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao {
        using(sessionOf(ds)) {
            it.transaction { tx ->
                queryOf(
                    statement = Queries.lagreVilkaarsvurdering,
                    paramMap = mapOf(
                        "behandlingId" to vilkaarsvurdering.behandlingId,
                        "payload" to vilkaarsvurdering.payload.toJson(),
                        "vilkaar" to vilkaarsvurdering.vilkaar.toJson(),
                        "resultat" to vilkaarsvurdering.resultat?.toJson(),
                        "virkningstidspunkt" to vilkaarsvurdering.virkningstidspunkt
                    )
                ).let { tx.run(it.asUpdate) }
            }
        }
        return vilkaarsvurdering
    }
}

private object Queries {
    val hentVilkaarsvurdering = "SELECT behandlingId, payload, vilkaar, resultat, virkningstidspunkt " +
        "FROM vilkaarsvurdering WHERE behandlingId = :behandlingId::UUID"
    val lagreVilkaarsvurdering = "INSERT INTO vilkaarsvurdering(behandlingId, payload, vilkaar, resultat, virkningstidspunkt) " + // ktlint-disable max-line-length
        "VALUES(:behandlingId::UUID, :payload::JSON, :vilkaar::JSONB, :resultat::JSONB, :virkningstidspunkt::DATE) " +
        "ON CONFLICT (behandlingId) DO UPDATE SET payload = EXCLUDED.payload, vilkaar = EXCLUDED.vilkaar, resultat = EXCLUDED.resultat, virkningstidspunkt = EXCLUDED.virkningstidspunkt" // ktlint-disable max-line-length
}