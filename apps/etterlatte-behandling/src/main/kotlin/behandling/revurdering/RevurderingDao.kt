package no.nav.etterlatte.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.behandling.somLocalDateTimeUTC
import no.nav.etterlatte.grunnlagsendring.setJsonb
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

class RevurderingDao(private val connection: () -> Connection) {
    fun asRevurdering(
        rs: ResultSet,
        sak: Sak,
        persongalleri: Persongalleri,
        kommerBarnetTilGode: (UUID) -> KommerBarnetTilgode?
    ): Revurdering {
        val id = rs.getUUID("id")
        val revurderingInfo = hentRevurderingInfoForBehandling(id)

        return Revurdering.opprett(
            id = id,
            sak = sak,
            behandlingOpprettet = rs.somLocalDateTimeUTC("behandling_opprettet"),
            sistEndret = rs.somLocalDateTimeUTC("sist_endret"),
            persongalleri = persongalleri,
            status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
            revurderingsaarsak = rs.getString("revurdering_aarsak").let { RevurderingAarsak.valueOf(it) },
            kommerBarnetTilgode = kommerBarnetTilGode.invoke(id),
            virkningstidspunkt = rs.getString("virkningstidspunkt")?.let { objectMapper.readValue(it) },
            utenlandstilsnitt = rs.getString("utenlandstilsnitt")?.let { objectMapper.readValue(it) },
            boddEllerArbeidetUtlandet = rs.getString("bodd_eller_arbeidet_utlandet")?.let {
                objectMapper.readValue(it)
            },
            prosesstype = rs.getString("prosesstype").let { Prosesstype.valueOf(it) },
            kilde = rs.getString("kilde").let { Vedtaksloesning.valueOf(it) },
            revurderingInfo = revurderingInfo
        )
    }

    fun lagreRevurderingInfo(id: UUID, revurderingInfo: RevurderingInfo, kilde: Grunnlagsopplysning.Kilde) {
        connection().prepareStatement(
            """
                INSERT INTO revurdering_info(behandling_id, info, kilde)
                VALUES(?, ?, ?) ON CONFLICT(behandling_id) DO UPDATE SET info = excluded.info, kilde = excluded.kilde
            """.trimIndent()
        ).let { statement ->
            statement.setObject(1, id)
            statement.setJsonb(2, revurderingInfo)
            statement.setJsonb(3, kilde)
            statement.executeUpdate()
        }
    }

    private fun hentRevurderingInfoForBehandling(id: UUID): RevurderingInfo? {
        return connection().prepareStatement(
            """
                SELECT info FROM revurdering_info 
                WHERE behandling_id = ?
            """.trimIndent()
        ).let { statement ->
            statement.setObject(1, id)
            statement.executeQuery()
                .singleOrNull { getString("info")?.let { objectMapper.readValue(it) } }
        }
    }
}