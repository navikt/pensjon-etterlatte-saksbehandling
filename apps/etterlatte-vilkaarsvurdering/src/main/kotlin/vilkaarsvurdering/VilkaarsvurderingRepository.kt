package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: UUID): VilkaarsvurderingDao?
    fun lagre(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao
}

class VilkaarsvurderingRepositoryImpl(private val ds: DataSource) : VilkaarsvurderingRepository {
    private val connection get() = ds.connection

    override fun hent(behandlingId: UUID): VilkaarsvurderingDao? = connection.use {
        it.prepareStatement(Queries.hentVilkaarsvurdering)
            .apply { setObject(1, behandlingId) }
            .executeQuery()
            .singleOrNull {
                VilkaarsvurderingDao(
                    behandlingId = getObject("behandlingId") as UUID,
                    payload = getString("payload"),
                    vilkaar = getString("vilkaar").let { vilkaar ->
                        objectMapper.readValue(vilkaar)
                    },
                    resultat = getString("resultat")?.let { resultat ->
                        objectMapper.readValue(resultat)
                    },
                    virkningstidspunkt = getDate("virkningstidspunkt").toLocalDate()
                )
            }
    }

    override fun lagre(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao {
        connection.use {
            it.prepareStatement(Queries.lagreVilkaarsvurdering)
                .apply {
                    setObject(1, vilkaarsvurdering.behandlingId)
                    setObject(2, vilkaarsvurdering.payload.toJson())
                    setObject(3, vilkaarsvurdering.vilkaar.toJson())
                    setObject(4, vilkaarsvurdering.resultat?.toJson())
                    setObject(5, vilkaarsvurdering.virkningstidspunkt)
                }
                .execute()
        }
        return vilkaarsvurdering
    }

    private fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
        return if (next()) {
            block().also {
                require(!next()) { "Skal være unik" }
            }
        } else {
            null
        }
    }
}

private object Queries {
    const val hentVilkaarsvurdering =
        "SELECT behandlingId, payload, vilkaar, resultat, virkningstidspunkt FROM vilkaarsvurdering WHERE behandlingId = ?" // ktlint-disable max-line-length
    const val lagreVilkaarsvurdering = "INSERT INTO vilkaarsvurdering(behandlingId, payload, vilkaar, resultat, virkningstidspunkt) " + // ktlint-disable max-line-length
        "VALUES(?::UUID, ?::JSON, ?::JSONB, ?::JSONB, ?::DATE) ON CONFLICT (behandlingId) DO " +
        "UPDATE SET payload = EXCLUDED.payload, vilkaar = EXCLUDED.vilkaar, resultat = EXCLUDED.resultat, virkningstidspunkt = EXCLUDED.virkningstidspunkt" // ktlint-disable max-line-length
}