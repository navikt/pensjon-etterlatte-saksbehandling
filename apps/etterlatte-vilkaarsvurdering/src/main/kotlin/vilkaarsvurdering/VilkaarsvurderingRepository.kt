package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import java.util.*
import javax.sql.DataSource

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: UUID): VilkaarsvurderingIntern?
    fun lagre(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern
}

class VilkaarsvurderingRepositoryImpl(private val ds: DataSource) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: UUID): VilkaarsvurderingIntern? =
        using(sessionOf(ds)) { session ->
            queryOf(
                statement = Queries.hentVilkaarsvurdering,
                paramMap = mapOf("behandlingId" to behandlingId)
            )
                .let { query -> session.run(query.map(::toVilkaarsvurderingIntern).asSingle) }
        }

    override fun lagre(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern {
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
                ).let { query -> tx.run(query.asUpdate) }
            }
        }
        return checkNotNull(hent(vilkaarsvurdering.behandlingId)) {
            "Fant ikke vilkårsvurdering for behandlingId=${vilkaarsvurdering.behandlingId}"
        }
    }

    private fun toVilkaarsvurderingIntern(row: Row) = with(row) {
        VilkaarsvurderingIntern(
            behandlingId = uuid("behandlingId"),
            payload = string("payload").let { payload -> objectMapper.readValue(payload) },
            vilkaar = string("vilkaar").let { vilkaar -> objectMapper.readValue(vilkaar) },
            resultat = stringOrNull("resultat").let { resultat ->
                resultat?.let { objectMapper.readValue(it) }
            },
            virkningstidspunkt = localDate("virkningstidspunkt")
        )
    }
}

private object Queries {
    val hentVilkaarsvurdering = """
        |SELECT behandlingId, payload, vilkaar, resultat, virkningstidspunkt 
        |FROM vilkaarsvurdering WHERE behandlingId = :behandlingId::UUID
    """.trimMargin()

    val lagreVilkaarsvurdering = """
        |INSERT INTO vilkaarsvurdering(behandlingId, payload, vilkaar, resultat, virkningstidspunkt) 
        |VALUES(:behandlingId::UUID, :payload::JSON, :vilkaar::JSONB, :resultat::JSONB, :virkningstidspunkt::DATE) 
        |ON CONFLICT (behandlingId)  
        |DO UPDATE SET 
        |   payload = EXCLUDED.payload, vilkaar = EXCLUDED.vilkaar, resultat = EXCLUDED.resultat,  
        |   virkningstidspunkt = EXCLUDED.virkningstidspunkt
    """.trimMargin()
}