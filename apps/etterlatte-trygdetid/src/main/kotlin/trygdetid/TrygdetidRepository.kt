package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import java.time.Period
import java.util.*
import javax.sql.DataSource

class TrygdetidRepository(private val dataSource: DataSource) {

    fun <T> transaction(block: (tx: TransactionalSession) -> T): T = dataSource.transaction(block)

    private fun <T> retrieveTransaction(txIn: TransactionalSession?, block: (tx: TransactionalSession) -> T): T =
        txIn?.let(block) ?: transaction { block(it) }

    fun hentTrygdetid(behandlingId: UUID): Trygdetid? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, behandling_id, sak_id, trygdetid_nasjonal, trygdetid_fremtidig, tidspunkt, 
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

    fun hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlagId: UUID): TrygdetidGrunnlag? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, trygdetid_id, type, bosted, periode_fra, periode_til, trygdetid, kilde, 
                        beregnet_verdi, beregnet_tidspunkt, beregnet_regelresultat 
                    FROM trygdetid_grunnlag
                    WHERE id = :trygdetidGrunnlagId
                """.trimIndent(),
                paramMap = mapOf("trygdetidGrunnlagId" to trygdetidGrunnlagId)
            ).let { query ->
                session.run(
                    query.map { row -> row.toTrygdetidGrunnlag() }.asSingle
                )
            }
        }

    fun opprettTrygdetid(behandling: DetaljertBehandling, opplysninger: List<Opplysningsgrunnlag>): Trygdetid =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                val trygdetidId = UUID.randomUUID()
                opprettTrygdetid(behandling, trygdetidId, tx)
                lagreOpplysningsgrunnlag(trygdetidId, opplysninger, tx)
            }
        }.let { hentTrygdtidNotNull(behandling.id) }

    private fun opprettTrygdetid(behandling: DetaljertBehandling, trygdetidId: UUID, tx: TransactionalSession) =
        queryOf(
            statement = """
                        INSERT INTO trygdetid(id, behandling_id, sak_id) VALUES(:id, :behandlingId, :sakId)
            """.trimIndent(),
            paramMap = mapOf(
                "id" to trygdetidId,
                "behandlingId" to behandling.id,
                "sakId" to behandling.sak
            )
        ).let { query -> tx.update(query) }

    private fun lagreOpplysningsgrunnlag(
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
                "id" to opplysningsgrunnlag.id,
                "trygdetidId" to trygdetidId,
                "type" to opplysningsgrunnlag.type.name,
                "opplysning" to opplysningsgrunnlag.opplysning.toJson(),
                "kilde" to opplysningsgrunnlag.kilde.toJson()
            )
        ).let { query -> tx.update(query) }
    }

    fun opprettTrygdetidGrunnlag(
        behandlingId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        txIn: TransactionalSession? = null
    ): Trygdetid =
        retrieveTransaction(txIn) { tx ->
            val trygdetid = hentTrygdtidNotNull(behandlingId)

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
                            beregnet_regelresultat
                        ) 
                        VALUES(:id, :trygdetidId, :type, :bosted, :periodeFra, :periodeTil, :kilde, 
                            :beregnetVerdi, :beregnetTidspunkt, :beregnetRegelresultat)
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to trygdetidGrunnlag.id,
                    "trygdetidId" to trygdetid.id,
                    "type" to trygdetidGrunnlag.type.name,
                    "bosted" to trygdetidGrunnlag.bosted,
                    "periodeFra" to trygdetidGrunnlag.periode.fra,
                    "periodeTil" to trygdetidGrunnlag.periode.til,
                    "kilde" to trygdetidGrunnlag.kilde?.toJson(),
                    "beregnetVerdi" to trygdetidGrunnlag.beregnetTrygdetid?.verdi.toString(),
                    "beregnetTidspunkt" to trygdetidGrunnlag.beregnetTrygdetid?.tidspunkt?.toTimestamp(),
                    "beregnetRegelresultat" to trygdetidGrunnlag.beregnetTrygdetid?.regelResultat?.toJson()
                )
            ).let { query -> tx.update(query) }
        }.let { hentTrygdtidNotNull(behandlingId) }

    fun oppdaterTrygdetidGrunnlag(
        behandlingId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        txIn: TransactionalSession? = null
    ): Trygdetid =
        retrieveTransaction(txIn) { tx ->
            queryOf(
                statement = """
                        UPDATE trygdetid_grunnlag
                        SET bosted = :bosted,
                         periode_fra = :periodeFra,
                         periode_til = :periodeTil,
                         kilde = :kilde,
                         beregnet_verdi = :beregnetVerdi, 
                         beregnet_tidspunkt = :beregnetTidspunkt, 
                         beregnet_regelresultat = :beregnetRegelresultat 
                        WHERE id = :trygdetidGrunnlagId
                """.trimIndent(),
                paramMap = mapOf(
                    "trygdetidGrunnlagId" to trygdetidGrunnlag.id,
                    "bosted" to trygdetidGrunnlag.bosted,
                    "periodeFra" to trygdetidGrunnlag.periode.fra,
                    "periodeTil" to trygdetidGrunnlag.periode.til,
                    "kilde" to trygdetidGrunnlag.kilde?.toJson(),
                    "beregnetVerdi" to trygdetidGrunnlag.beregnetTrygdetid?.verdi.toString(),
                    "beregnetTidspunkt" to trygdetidGrunnlag.beregnetTrygdetid?.tidspunkt?.toTimestamp(),
                    "beregnetRegelresultat" to trygdetidGrunnlag.beregnetTrygdetid?.regelResultat?.toJson()
                )
            ).let { query -> tx.update(query) }
        }.let { hentTrygdtidNotNull(behandlingId) }

    fun oppdaterBeregnetTrygdetid(
        behandlingId: UUID,
        beregnetTrygdetid: BeregnetTrygdetid,
        txIn: TransactionalSession? = null
    ): Trygdetid =
        retrieveTransaction(txIn) { tx ->
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
            ).let { query -> tx.update(query) }
        }.let { hentTrygdtidNotNull(behandlingId) }

    private fun hentTrygdetidGrunnlag(trygdetidId: UUID): List<TrygdetidGrunnlag> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, trygdetid_id, type, bosted, periode_fra, periode_til, kilde, 
                        beregnet_verdi, beregnet_tidspunkt, beregnet_regelresultat 
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

    private fun Row.toTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>, opplysninger: List<Opplysningsgrunnlag>) =
        Trygdetid(
            id = uuid("id"),
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
            beregnetTrygdetid = BeregnetTrygdetidGrunnlag(
                verdi = string("beregnet_verdi").let { Period.parse(it) },
                tidspunkt = sqlTimestamp("beregnet_tidspunkt").toTidspunkt(),
                regelResultat = string("beregnet_regelresultat").let {
                    objectMapper.readTree(it)
                }
            )
        )

    private fun Row.toOpplysningsgrunnlag(): Opplysningsgrunnlag {
        return Opplysningsgrunnlag(
            id = uuid("id"),
            type = string("type").let { TrygdetidOpplysningType.valueOf(it) },
            opplysning = string("opplysning").toJsonNode(),
            kilde = string("kilde").let { objectMapper.readValue(it) }
        )
    }
}