package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import java.time.Period
import java.util.*
import javax.sql.DataSource

class TrygdetidRepository(private val dataSource: DataSource) {

    fun hentTrygdetid(behandlingId: UUID): Trygdetid? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, sak_id, behandling_id, trygdetid_nasjonal, trygdetid_fremtidig, tidspunkt, 
                     trygdetid_total, trygdetid_total_tidspunkt, trygdetid_total_regelresultat 
                    FROM trygdetid 
                    WHERE behandling_id = :behandlingId
                """.trimIndent(),
                paramMap = mapOf("behandlingId" to behandlingId)
            ).let { query ->
                session.run(
                    query.map { row ->
                        val trygdetidId = row.uuid("id")
                        val trygdetidGrunnlag = hentTrygdetidGrunnlag(trygdetidId)
                        val opplysninger = hentGrunnlagOpplysninger(trygdetidId)
                        row.toTrygdetid(trygdetidGrunnlag, opplysninger)
                    }.asSingle
                )
            }
        }

    fun opprettTrygdetid(trygdetid: Trygdetid): Trygdetid =
        dataSource.transaction { tx ->
            opprettTrygdetid(trygdetid, tx)
            opprettOpplysningsgrunnlag(trygdetid.id, trygdetid.opplysninger, tx)
            trygdetid.trygdetidGrunnlag.forEach { opprettTrygdetidGrunnlag(trygdetid.id, it, tx) }

            if (trygdetid.beregnetTrygdetid != null) {
                oppdaterBeregnetTrygdetid(trygdetid.behandlingId, trygdetid.beregnetTrygdetid, tx)
            }
        }.let { hentTrygdtidNotNull(trygdetid.behandlingId) }

    fun oppdaterTrygdetid(oppdatertTrygdetid: Trygdetid): Trygdetid =
        dataSource.transaction { tx ->
            val gjeldendeTrygdetid = hentTrygdtidNotNull(oppdatertTrygdetid.behandlingId)

            // opprett grunnlag
            oppdatertTrygdetid.trygdetidGrunnlag
                .filter { gjeldendeTrygdetid.trygdetidGrunnlag.find { tg -> tg.id == it.id } == null }
                .forEach { opprettTrygdetidGrunnlag(oppdatertTrygdetid.id, it, tx) }

            // oppdater grunnlag
            oppdatertTrygdetid.trygdetidGrunnlag.forEach { trygdetidGrunnlag ->
                gjeldendeTrygdetid.trygdetidGrunnlag
                    .find { it.id == trygdetidGrunnlag.id && it != trygdetidGrunnlag }
                    ?.let { oppdaterTrygdetidGrunnlag(trygdetidGrunnlag, tx) }
            }

            // slett grunnlag
            gjeldendeTrygdetid.trygdetidGrunnlag
                .filter { oppdatertTrygdetid.trygdetidGrunnlag.find { tg -> tg.id == it.id } == null }
                .forEach { slettTrygdetidGrunnlag(it.id, tx) }

            if (oppdatertTrygdetid.beregnetTrygdetid != null) {
                oppdaterBeregnetTrygdetid(oppdatertTrygdetid.behandlingId, oppdatertTrygdetid.beregnetTrygdetid, tx)
            } else {
                nullstillBeregnetTrygdetid(oppdatertTrygdetid.behandlingId, tx)
            }
        }.let { hentTrygdtidNotNull(oppdatertTrygdetid.behandlingId) }

    private fun opprettTrygdetid(trygdetid: Trygdetid, tx: TransactionalSession) =
        queryOf(
            statement = """
                INSERT INTO trygdetid(id, behandling_id, sak_id) VALUES(:id, :behandlingId, :sakId)
            """.trimIndent(),
            paramMap = mapOf(
                "id" to trygdetid.id,
                "behandlingId" to trygdetid.behandlingId,
                "sakId" to trygdetid.sakId
            )
        ).let { query -> tx.update(query) }

    private fun opprettOpplysningsgrunnlag(
        trygdetidId: UUID?,
        opplysninger: List<Opplysningsgrunnlag>,
        tx: TransactionalSession
    ) = opplysninger.forEach { opplysningsgrunnlag ->
        queryOf(
            statement = """
                INSERT INTO opplysningsgrunnlag(id, trygdetid_id, type, opplysning, kilde)
                 VALUES(:id, :trygdetidId, :type, :opplysning::JSONB, :kilde::JSONB)
            """.trimIndent(),
            paramMap = mapOf(
                "id" to UUID.randomUUID(),
                "trygdetidId" to trygdetidId,
                "type" to opplysningsgrunnlag.type.name,
                "opplysning" to opplysningsgrunnlag.opplysning.toJson(),
                "kilde" to opplysningsgrunnlag.kilde.toJson()
            )
        ).let { query -> tx.update(query) }
    }

    private fun opprettTrygdetidGrunnlag(
        trygdetidId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
                INSERT INTO trygdetid_grunnlag(
                    id, 
                    trygdetid_id, 
                    type, bosted, 
                    periode_fra, 
                    periode_til,
                    kilde,
                    beregnet_verdi, 
                    beregnet_tidspunkt, 
                    beregnet_regelresultat,
                    begrunnelse,
                    poeng_inn_aar,
                    poeng_ut_aar
                ) 
                VALUES(:id, :trygdetidId, :type, :bosted, :periodeFra, :periodeTil, :kilde, 
                    :beregnetVerdi, :beregnetTidspunkt, :beregnetRegelresultat, :begrunnelse, :poengInnAar, :poengUtAar)
            """.trimIndent(),
            paramMap = mapOf(
                "id" to trygdetidGrunnlag.id,
                "trygdetidId" to trygdetidId,
                "type" to trygdetidGrunnlag.type.name,
                "bosted" to trygdetidGrunnlag.bosted,
                "periodeFra" to trygdetidGrunnlag.periode.fra,
                "periodeTil" to trygdetidGrunnlag.periode.til,
                "kilde" to trygdetidGrunnlag.kilde.toJson(),
                "beregnetVerdi" to trygdetidGrunnlag.beregnetTrygdetid?.verdi?.toString(),
                "beregnetTidspunkt" to trygdetidGrunnlag.beregnetTrygdetid?.tidspunkt?.toTimestamp(),
                "beregnetRegelresultat" to trygdetidGrunnlag.beregnetTrygdetid?.regelResultat?.toJson(),
                "begrunnelse" to trygdetidGrunnlag.begrunnelse,
                "poengInnAar" to trygdetidGrunnlag.poengInnAar,
                "poengUtAar" to trygdetidGrunnlag.poengUtAar
            )
        ).let { query -> tx.update(query) }
    }

    private fun oppdaterTrygdetidGrunnlag(
        trygdetidGrunnlag: TrygdetidGrunnlag,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
                UPDATE trygdetid_grunnlag
                SET bosted = :bosted,
                 periode_fra = :periodeFra,
                 periode_til = :periodeTil,
                 kilde = :kilde,
                 beregnet_verdi = :beregnetVerdi, 
                 beregnet_tidspunkt = :beregnetTidspunkt, 
                 beregnet_regelresultat = :beregnetRegelresultat,
                 begrunnelse = :begrunnelse,
                 poeng_inn_aar = :poengInnAar,
                 poeng_ut_aar = :poengUtAar
                WHERE id = :trygdetidGrunnlagId
            """.trimIndent(),
            paramMap = mapOf(
                "trygdetidGrunnlagId" to trygdetidGrunnlag.id,
                "bosted" to trygdetidGrunnlag.bosted,
                "periodeFra" to trygdetidGrunnlag.periode.fra,
                "periodeTil" to trygdetidGrunnlag.periode.til,
                "kilde" to trygdetidGrunnlag.kilde.toJson(),
                "beregnetVerdi" to trygdetidGrunnlag.beregnetTrygdetid?.verdi?.toString(),
                "beregnetTidspunkt" to trygdetidGrunnlag.beregnetTrygdetid?.tidspunkt?.toTimestamp(),
                "beregnetRegelresultat" to trygdetidGrunnlag.beregnetTrygdetid?.regelResultat?.toJson(),
                "begrunnelse" to trygdetidGrunnlag.begrunnelse,
                "poengInnAar" to trygdetidGrunnlag.poengInnAar,
                "poengUtAar" to trygdetidGrunnlag.poengUtAar
            )
        ).let { query -> tx.update(query) }
    }

    private fun slettTrygdetidGrunnlag(
        trygdetidGrunnlagId: UUID,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = "DELETE FROM trygdetid_grunnlag WHERE id = :id",
            paramMap = mapOf(
                "id" to trygdetidGrunnlagId
            )
        ).let { query -> tx.update(query) }
    }

    private fun oppdaterBeregnetTrygdetid(
        behandlingId: UUID,
        beregnetTrygdetid: BeregnetTrygdetid,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
                UPDATE trygdetid 
                SET trygdetid_nasjonal = :trygdetidNasjonal, trygdetid_fremtidig = :trygdetidFremtidig, 
                    trygdetid_total = :trygdetidTotal, trygdetid_total_tidspunkt = :tidspunkt, 
                    trygdetid_total_regelresultat = :regelResultat
                WHERE behandling_id = :behandlingId
            """.trimIndent(),
            paramMap = mapOf(
                "behandlingId" to behandlingId,
                "trygdetidTotal" to beregnetTrygdetid.verdi,
                "tidspunkt" to beregnetTrygdetid.tidspunkt.toTimestamp(),
                "regelResultat" to beregnetTrygdetid.regelResultat.toJson()
            )
        ).let { query ->
            tx.update(query)
        }
    }

    private fun nullstillBeregnetTrygdetid(behandlingId: UUID, tx: TransactionalSession) = queryOf(
        statement = """
                UPDATE trygdetid 
                SET trygdetid_total = null, 
                    trygdetid_total_regelresultat = null,
                    trygdetid_total_tidspunkt = null
                WHERE behandling_id = :behandlingId
        """.trimIndent(),
        paramMap = mapOf("behandlingId" to behandlingId)
    ).let { query -> tx.update(query) }

    private fun hentTrygdetidGrunnlag(trygdetidId: UUID): List<TrygdetidGrunnlag> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, trygdetid_id, type, bosted, periode_fra, periode_til, kilde, beregnet_verdi,
                    beregnet_tidspunkt, beregnet_regelresultat , begrunnelse, poeng_inn_aar, poeng_ut_aar
                    FROM trygdetid_grunnlag
                    WHERE trygdetid_id = :trygdetidId
                """.trimIndent(),
                paramMap = mapOf("trygdetidId" to trygdetidId)
            ).let { query ->
                session.run(
                    query.map { row -> row.toTrygdetidGrunnlag() }.asList
                )
            }
        }

    private fun hentGrunnlagOpplysninger(trygdetidId: UUID): List<Opplysningsgrunnlag> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                SELECT id, trygdetid_id, type, opplysning, kilde
                FROM opplysningsgrunnlag
                WHERE trygdetid_id = :trygdetidId
                """.trimIndent(),
                paramMap = mapOf("trygdetidId" to trygdetidId)
            ).let { query ->
                session.run(
                    query.map { row -> row.toOpplysningsgrunnlag() }.asList
                )
            }
        }

    private fun hentTrygdtidNotNull(behandlingsId: UUID) =
        hentTrygdetid(behandlingsId)
            ?: throw Exception("Fant ikke trygdetid for $behandlingsId")

    private fun Row.toTrygdetid(
        trygdetidGrunnlag: List<TrygdetidGrunnlag>,
        opplysninger: List<Opplysningsgrunnlag>
    ) =
        Trygdetid(
            id = uuid("id"),
            sakId = long("sak_id"),
            behandlingId = uuid("behandling_id"),
            beregnetTrygdetid = intOrNull("trygdetid_total")?.let {
                BeregnetTrygdetid(
                    verdi = it,
                    tidspunkt = tidspunkt("trygdetid_total_tidspunkt"),
                    regelResultat = string("trygdetid_total_regelresultat").let { regelResultat ->
                        objectMapper.readTree(regelResultat)
                    }
                )
            },
            trygdetidGrunnlag = trygdetidGrunnlag,
            opplysninger = opplysninger
        )

    private fun Row.toTrygdetidGrunnlag() =
        TrygdetidGrunnlag(
            id = uuid("id"),
            type = string("type").let { TrygdetidType.valueOf(it) },
            bosted = string("bosted"),
            periode = TrygdetidPeriode(
                fra = localDate("periode_fra"),
                til = localDate("periode_til")
            ),
            kilde = string("kilde").let { objectMapper.readValue(it) },
            beregnetTrygdetid = stringOrNull("beregnet_verdi")?.let { verdi ->
                BeregnetTrygdetidGrunnlag(
                    verdi = Period.parse(verdi),
                    tidspunkt = sqlTimestamp("beregnet_tidspunkt").toTidspunkt(),
                    regelResultat = string("beregnet_regelresultat").let {
                        objectMapper.readTree(it)
                    }
                )
            },
            begrunnelse = stringOrNull("begrunnelse"),
            poengInnAar = boolean("poeng_inn_aar"),
            poengUtAar = boolean("poeng_ut_aar")
        )

    private fun Row.toOpplysningsgrunnlag(): Opplysningsgrunnlag {
        return Opplysningsgrunnlag(
            id = uuid("id"),
            type = string("type").let { TrygdetidOpplysningType.valueOf(it) },
            opplysning = string("opplysning").let { objectMapper.readValue(it) },
            kilde = string("kilde").let { objectMapper.readValue(it) }
        )
    }
}