package no.nav.etterlatte.brev.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.InformasjonsbrevUtsending
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus
import no.nav.etterlatte.brev.StrukturertInformasjonsbrevRequest
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.security.MessageDigest
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class InformasjonsbrevRepository(
    private val ds: DataSource,
    private val brevRepository: BrevRepository,
) {
    fun hent(id: UUID): LagretInformasjonsbrev? = using(sessionOf(ds)) { tx -> hent(tx, id) }

    private fun hent(
        tx: Session,
        id: UUID,
    ): LagretInformasjonsbrev? =
        tx.run(queryOf("SELECT * FROM informasjonsbrev_utsending WHERE utsendings_id = ?", id).map(::fraRad).asSingle)

    fun opprett(
        id: UUID,
        request: StrukturertInformasjonsbrevRequest,
        nyttBrev: OpprettNyttBrev,
        bruker: BrukerTokenInfo,
    ): LagretInformasjonsbrev {
        val fingerprint = fingerprint(request)
        repeat(5) {
            hent(id)?.let { return kontroller(it, request.sak.id, bruker.ident(), fingerprint) }
            try {
                return ds.transaction(true) { tx ->
                    val brev = brevRepository.opprettBrev(tx, nyttBrev, bruker, true)
                    tx.run(
                        queryOf(
                            """
                            INSERT INTO informasjonsbrev_utsending
                                (utsendings_id, sak_id, innsender, fingerprint, request, brev_id, status)
                            VALUES (?, ?, ?, ?, ?::jsonb, ?, 'OPPRETTET')
                            """.trimIndent(),
                            id,
                            request.sak.id.sakId,
                            bruker.ident(),
                            fingerprint,
                            request.toJson(),
                            brev.id,
                        ).asUpdate,
                    )
                    hent(tx, id)!!
                }
            } catch (e: SQLException) {
                if (e.sqlState !in setOf("23505", "40001")) throw e
            }
        }
        return kontroller(requireNotNull(hent(id)), request.sak.id, bruker.ident(), fingerprint)
    }

    fun kontroller(
        lagret: LagretInformasjonsbrev,
        sakId: SakId,
        innsender: String,
        fingerprint: String? = null,
    ): LagretInformasjonsbrev {
        if (lagret.resultat.sakId != sakId || lagret.innsender != innsender) throw InformasjonsbrevKonflikt()
        if (fingerprint != null && lagret.fingerprint != fingerprint) throw InformasjonsbrevKonflikt()
        return lagret
    }

    fun overta(
        id: UUID,
        eier: UUID,
    ): Boolean =
        try {
            ds.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                        UPDATE informasjonsbrev_utsending SET eier = ?, laast_til = now() + interval '5 minutes'
                        WHERE utsendings_id = ?
                          AND status IN ('OPPRETTET', 'PDF_LAGRET', 'FERDIGSTILT', 'JOURNALFOERT')
                          AND (eier IS NULL OR laast_til < now())
                        """.trimIndent(),
                        eier,
                        id,
                    ).asUpdate,
                ) == 1
            }
        } catch (e: SQLException) {
            if (e.sqlState == "40001") false else throw e
        }

    fun steg(
        id: UUID,
        eier: UUID,
        status: InformasjonsbrevUtsendingStatus,
        feilkode: String? = null,
        frigi: Boolean = false,
    ): Boolean =
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    """
                    UPDATE informasjonsbrev_utsending SET status = ?, feilkode = ?,
                        eier = CASE WHEN ? THEN NULL ELSE eier END,
                        laast_til = CASE WHEN ? THEN NULL ELSE now() + interval '5 minutes' END
                    WHERE utsendings_id = ? AND eier = ?
                    """.trimIndent(),
                    status.name,
                    feilkode,
                    frigi,
                    frigi,
                    id,
                    eier,
                ).asUpdate,
            ) == 1
        }

    fun lagrePdf(
        id: UUID,
        eier: UUID,
        pdf: Pdf,
    ) = medEier(id, eier) { tx, lagret ->
        tx.run(
            queryOf(
                "INSERT INTO pdf (brev_id, bytes) VALUES (?, ?) ON CONFLICT (brev_id) DO NOTHING",
                lagret.resultat.brevId,
                pdf.bytes,
            ).asUpdate,
        )
        tx.run(
            queryOf("UPDATE informasjonsbrev_utsending SET status = 'PDF_LAGRET' WHERE utsendings_id = ?", id).asUpdate,
        )
    }

    fun ferdigstill(
        id: UUID,
        eier: UUID,
        bruker: BrukerTokenInfo,
    ) = medEier(id, eier) { tx, lagret ->
        tx.run(
            queryOf(
                "INSERT INTO hendelse (brev_id, status_id, payload, saksbehandler) VALUES (?, 'FERDIGSTILT', '{}', ?)",
                lagret.resultat.brevId,
                bruker.ident(),
            ).asUpdate,
        )
        tx.run(queryOf("UPDATE informasjonsbrev_utsending SET status = 'FERDIGSTILT' WHERE utsendings_id = ?", id).asUpdate)
    }

    private fun medEier(
        id: UUID,
        eier: UUID,
        operation: (Session, LagretInformasjonsbrev) -> Unit,
    ) = ds.transaction { tx ->
        tx.run(
            queryOf("SELECT utsendings_id FROM informasjonsbrev_utsending WHERE utsendings_id = ? FOR UPDATE", id)
                .map {
                    it.uuid("utsendings_id")
                }.asSingle,
        )
        val lagret = requireNotNull(hent(tx, id))
        check(lagret.eier == eier) { "UTSENDINGS_EIER_ENDRET" }
        tx.run(
            queryOf("SELECT set_config('etterlatte.batch_eier', ?, true) AS eier", eier.toString()).map { it.string("eier") }.asSingle,
        )
        operation(tx, lagret)
    }

    private fun fraRad(row: Row) =
        LagretInformasjonsbrev(
            resultat =
                InformasjonsbrevUtsending(
                    row.uuid("utsendings_id"),
                    SakId(row.long("sak_id")),
                    row.long("brev_id"),
                    InformasjonsbrevUtsendingStatus.valueOf(row.string("status")),
                    row.stringOrNull("feilkode"),
                ),
            request = deserialize(row.string("request")),
            innsender = row.string("innsender"),
            fingerprint = row.string("fingerprint"),
            eier = row.uuidOrNull("eier"),
        )

    companion object {
        fun fingerprint(request: StrukturertInformasjonsbrevRequest): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(request.toJson().toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}

data class LagretInformasjonsbrev(
    val resultat: InformasjonsbrevUtsending,
    val request: StrukturertInformasjonsbrevRequest,
    val innsender: String,
    val fingerprint: String,
    val eier: UUID?,
)

class InformasjonsbrevKonflikt :
    ForespoerselException(
        code = "INFORMASJONSBREV_KONFLIKT",
        detail = "Utsendings-ID er allerede knyttet til et annet datagrunnlag, en annen sak eller innsender.",
        status = 409,
    )
