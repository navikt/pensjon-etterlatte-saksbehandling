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
    fun hent(behandlingId: UUID): Vilkaarsvurdering?
    fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering
}

class VilkaarsvurderingRepositoryImpl(private val ds: DataSource) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: UUID): Vilkaarsvurdering? =
        using(sessionOf(ds)) { session ->
            queryOf(
                statement = Queries.hentVilkaarsvurdering,
                paramMap = mapOf("behandlingId" to behandlingId)
            )
                .let { query ->
                    session.run(
                        query.map { row ->
                            Vilkaarsvurdering(
                                behandlingId = row.uuid("behandlingId"),
                                payload = row.string("payload").let { payload ->
                                    objectMapper.readValue(payload)
                                },
                                vilkaar = row.string("vilkaar").let { vilkaar ->
                                    objectMapper.readValue(vilkaar)
                                },
                                resultat = row.stringOrNull("resultat").let { resultat ->
                                    resultat?.let { objectMapper.readValue(it) }
                                }
                            )
                        }.asSingle
                    )
                }
        }

    override fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        using(sessionOf(ds)) {
            it.transaction { tx ->
                queryOf(
                    statement = Queries.lagreVilkaarsvurdering,
                    paramMap = mapOf(
                        "behandlingId" to vilkaarsvurdering.behandlingId,
                        "payload" to vilkaarsvurdering.payload.toJson(),
                        "vilkaar" to vilkaarsvurdering.vilkaar.toJson(),
                        "resultat" to vilkaarsvurdering.resultat?.toJson()
                    )
                ).let { tx.run(it.asUpdate) }
            }
        }
        return vilkaarsvurdering
    }
}

private object Queries {
    val hentVilkaarsvurdering = "SELECT behandlingId, payload, vilkaar, resultat " +
        "FROM vilkaarsvurdering WHERE behandlingId = :behandlingId::UUID"
    val lagreVilkaarsvurdering = "INSERT INTO vilkaarsvurdering(behandlingId, payload, vilkaar, resultat) " +
        "VALUES(:behandlingId::UUID, :payload::JSON, :vilkaar::JSONB, :resultat::JSONB) ON CONFLICT (behandlingId) " +
        "DO UPDATE SET payload = EXCLUDED.payload, vilkaar = EXCLUDED.vilkaar, resultat = EXCLUDED.resultat"
}