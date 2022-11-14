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
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = Queries.lagreVilkaarsvurdering,
                    paramMap = mapOf(
                        "behandlingId" to vilkaarsvurdering.behandlingId,
                        "vilkaar" to vilkaarsvurdering.vilkaar.toJson(),
                        "resultat" to vilkaarsvurdering.resultat?.toJson(),
                        "virkningstidspunkt" to vilkaarsvurdering.virkningstidspunkt.toJson(),
                        "metadata" to vilkaarsvurdering.grunnlagsmetadata.toJson()
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
            vilkaar = string("vilkaar").let { vilkaar -> objectMapper.readValue(vilkaar) },
            resultat = stringOrNull("resultat").let { resultat ->
                resultat?.let { objectMapper.readValue(it) }
            },
            virkningstidspunkt = string("virkningstidspunkt").let { virk -> objectMapper.readValue(virk) },
            grunnlagsmetadata = string("metadata").let { metadata -> objectMapper.readValue(metadata) }
        )
    }
}

private object Queries {
    val hentVilkaarsvurdering = """
        |SELECT behandlingId, vilkaar, resultat, virkningstidspunkt, metadata 
        |FROM vilkaarsvurdering WHERE behandlingId = :behandlingId::UUID
    """.trimMargin()

    val lagreVilkaarsvurdering = """
        |INSERT INTO vilkaarsvurdering(behandlingId, vilkaar, resultat, virkningstidspunkt, metadata) 
        |VALUES(:behandlingId::UUID, :vilkaar::JSONB, :resultat::JSONB, :virkningstidspunkt::JSONB, :metadata::JSONB) 
        |ON CONFLICT (behandlingId)  
        |DO UPDATE SET 
        |   vilkaar = EXCLUDED.vilkaar, resultat = EXCLUDED.resultat,  
        |   virkningstidspunkt = EXCLUDED.virkningstidspunkt, metadata = EXCLUDED.metadata
    """.trimMargin()
}