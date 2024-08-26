package no.nav.etterlatte.sanksjon

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.transaction
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class SanksjonRepository(
    private val dataSource: DataSource,
) {
    fun hentSanksjon(behandlingId: UUID): List<Sanksjon>? =
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    "SELECT * FROM sanksjon WHERE behandling_id = ?",
                behandlingId,
            ).let { query ->
                tx.run(query.map { row -> row.toSanksjon() }.asList).ifEmpty {
                    null
                }
            }
        }

    fun opprettSanksjon(
        behandlingId: UUID,
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        saksbehandlerIdent: String,
        sanksjon: LagreSanksjon,
    ) {
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    INSERT INTO sanksjon(
                        id, behandling_id, sak_id, sanksjon_type, fom, tom, opprettet, endret, beskrivelse
                    ) VALUES (
                        :id, :behandlingId, :sak_id, :type, :fom, :tom, :opprettet, :endret, :beskrivelse
                    )
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to UUID.randomUUID(),
                        "behandlingId" to behandlingId,
                        "sak_id" to sakId,
                        "type" to sanksjon.type.name,
                        "fom" to sanksjon.fom,
                        "tom" to sanksjon.tom,
                        "opprettet" to Grunnlagsopplysning.Saksbehandler.create(saksbehandlerIdent).toJson(),
                        "endret" to null,
                        "beskrivelse" to sanksjon.beskrivelse,
                    ),
            ).let { query -> tx.run(query.asUpdate) }
        }
    }

    fun opprettSanksjonFraKopi(
        behandlingId: UUID,
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        sanksjon: Sanksjon,
    ) {
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    INSERT INTO sanksjon(
                        id, behandling_id, sak_id, sanksjon_type, fom, tom, opprettet, endret, beskrivelse
                    ) VALUES (
                        :id, :behandlingId, :sak_id, :type, :fom, :tom, :opprettet, :endret, :beskrivelse
                    )
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to UUID.randomUUID(),
                        "behandlingId" to behandlingId,
                        "sak_id" to sakId,
                        "type" to sanksjon.type.name,
                        "fom" to sanksjon.fom.atDay(1),
                        "tom" to sanksjon.tom?.atEndOfMonth(),
                        "opprettet" to sanksjon.opprettet.toJson(),
                        "endret" to sanksjon.endret?.toJson(),
                        "beskrivelse" to sanksjon.beskrivelse,
                    ),
            ).let { query -> tx.run(query.asUpdate) }
        }
    }

    fun oppdaterSanksjon(
        sanksjon: LagreSanksjon,
        saksbehandlerIdent: String,
    ) {
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    UPDATE sanksjon
                    SET fom = :fom, 
                        tom = :tom, 
                        sanksjon_type = :type,
                        endret = :endret, 
                        beskrivelse = :beskrivelse
                    WHERE id = :id
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to sanksjon.id,
                        "fom" to sanksjon.fom,
                        "tom" to sanksjon.tom,
                        "type" to sanksjon.type.name,
                        "endret" to Grunnlagsopplysning.Saksbehandler.create(saksbehandlerIdent).toJson(),
                        "beskrivelse" to sanksjon.beskrivelse,
                    ),
            ).let { query -> tx.run(query.asUpdate) }
        }
    }

    fun slettSanksjon(sanksjonId: UUID): Int =
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    "DELETE FROM sanksjon WHERE id = ?",
                sanksjonId,
            ).let { query -> tx.run(query.asUpdate) }
        }

    private fun Row.toSanksjon() =
        Sanksjon(
            id = uuid("id"),
            behandlingId = uuid("behandling_id"),
            sakId = long("sak_id"),
            type = SanksjonType.valueOf(string("sanksjon_type")),
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) },
            opprettet = string("opprettet").let { objectMapper.readValue(it) },
            endret = stringOrNull("endret")?.let { objectMapper.readValue(it) },
            beskrivelse = string("beskrivelse"),
        )

    fun hentSanksjonMedId(sanksjonId: UUID): Sanksjon? =
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    SELECT * FROM sanksjon WHERE id = ?
                    """.trimIndent(),
                sanksjonId,
            ).let { query -> tx.run(query.map { it.toSanksjon() }.asSingle) }
        }
}

data class Sanksjon(
    val id: UUID?,
    val behandlingId: UUID,
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val type: SanksjonType,
    val fom: YearMonth,
    val tom: YearMonth?,
    val opprettet: Grunnlagsopplysning.Saksbehandler,
    val endret: Grunnlagsopplysning.Saksbehandler?,
    val beskrivelse: String,
)

data class LagreSanksjon(
    val id: UUID?,
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val type: SanksjonType,
    val fom: LocalDate,
    val tom: LocalDate?,
    val beskrivelse: String,
)
