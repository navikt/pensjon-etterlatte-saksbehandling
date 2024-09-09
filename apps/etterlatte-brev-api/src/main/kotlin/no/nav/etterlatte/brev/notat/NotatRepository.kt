package no.nav.etterlatte.brev.notat

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import javax.sql.DataSource

class NotatRepository(
    private val ds: DataSource,
) {
    fun hent(id: NotatID): Notat =
        using(sessionOf(ds)) {
            it.run(
                queryOf("SELECT id, sak_id, journalpost_id, tittel, opprettet, referanse FROM notat WHERE id = ?", id)
                    .map(tilNotat)
                    .asSingle,
            )
        }!!

    fun hentForSak(sakId: SakId): List<Notat> =
        using(sessionOf(ds)) {
            it.run(
                queryOf(
                    "SELECT id, sak_id, journalpost_id, tittel, opprettet, referanse FROM notat WHERE sak_id = ?",
                    sakId,
                ).map(tilNotat)
                    .asList,
            )
        }

    fun hentForReferanse(referanse: String): List<Notat> =
        using(sessionOf(ds)) {
            it.run(
                queryOf(
                    "SELECT id, sak_id, journalpost_id, tittel, opprettet, referanse FROM notat WHERE referanse = ?",
                    referanse,
                ).map(tilNotat)
                    .asList,
            )
        }

    fun hentPayload(id: NotatID): Slate =
        using(sessionOf(ds)) {
            it.run(
                queryOf("SELECT payload FROM notat WHERE id = ?", id)
                    .map { row -> deserialize<Slate>(row.string("payload")) }
                    .asSingle,
            )!!
        }

    fun hentPdf(id: NotatID): ByteArray =
        using(sessionOf(ds)) {
            it.run(
                queryOf("SELECT bytes FROM notat WHERE id = ?", id)
                    .map { row -> row.bytes("bytes") }
                    .asSingle,
            )!!
        }

    fun oppdaterPayload(
        id: NotatID,
        payload: Slate,
        bruker: BrukerTokenInfo,
    ) = ds.transaction { tx ->
        tx.run(
            queryOf(
                "UPDATE notat SET payload = :payload WHERE id = :id",
                mapOf(
                    "id" to id,
                    "payload" to payload.toJson(),
                ),
            ).asUpdate,
        )

        tx.lagreHendelse(id, "PAYLOAD_OPPDATERT".toJson(), bruker)
    }

    fun lagreInnhold(
        id: NotatID,
        bytes: ByteArray,
    ) = using(sessionOf(ds)) {
        it.run(
            queryOf(
                "UPDATE notat SET bytes = :bytes WHERE id = :id",
                mapOf(
                    "id" to id,
                    "bytes" to bytes,
                ),
            ).asUpdate,
        )
    }

    fun settJournalfoert(
        id: NotatID,
        journalpostResponse: OpprettJournalpostResponse,
        bruker: BrukerTokenInfo,
    ) = ds.transaction { tx ->
        tx.run(
            queryOf(
                """
                        UPDATE notat 
                        SET journalpost_id = :journalpost_id
                        WHERE id = :id
                    """,
                mapOf(
                    "id" to id,
                    "journalpost_id" to journalpostResponse.journalpostId,
                ),
            ).asUpdate,
        )

        tx.lagreHendelse(id, journalpostResponse.toJson(), bruker)
    }

    fun opprett(
        notat: NyttNotat,
        bruker: BrukerTokenInfo,
    ): NotatID =
        ds.transaction(returnGeneratedKey = true) { tx ->
            val id =
                tx.run(
                    queryOf(
                        """
                        INSERT INTO notat (sak_id, tittel, payload, opprettet, referanse)
                        VALUES (:sak_id, :tittel, :payload, :opprettet, :referanse)
                    """,
                        mapOf(
                            "sak_id" to notat.sakId,
                            "tittel" to notat.tittel,
                            "payload" to notat.payload.toJson(),
                            "opprettet" to Tidspunkt.now().toTimestamp(),
                            "referanse" to notat.referanse,
                        ),
                    ).asUpdateAndReturnGeneratedKey,
                )

            checkNotNull(id) { "Kunne ikke lagre notat i databasen!" }

            tx.lagreHendelse(id, notat.toJson(), bruker)

            return@transaction id
        }

    fun oppdaterTittel(
        id: NotatID,
        tittel: String,
        bruker: BrukerTokenInfo,
    ) = ds.transaction { tx ->
        tx
            .run(
                queryOf(
                    "UPDATE notat SET tittel = :tittel WHERE id = :id",
                    mapOf(
                        "id" to id,
                        "tittel" to tittel,
                    ),
                ).asUpdate,
            ).also { oppdatert ->
                require(oppdatert == 1)
            }

        tx.lagreHendelse(id, tittel.toJson(), bruker)
    }

    fun slett(id: NotatID) {
        using(sessionOf(ds)) {
            it.run(
                queryOf("DELETE FROM notat WHERE id = ?", id).asUpdate,
            )
        }
    }

    private fun Session.lagreHendelse(
        notatId: NotatID,
        payload: String = "{}",
        bruker: BrukerTokenInfo,
    ) = run(
        queryOf(
            """
            INSERT INTO notat_hendelse (notat_id, saksbehandler, payload, tidspunkt) 
            VALUES (:notat_id, :saksbehandler, :payload, :tidspunkt)
            """,
            mapOf(
                "notat_id" to notatId,
                "saksbehandler" to bruker.ident(),
                "payload" to payload,
                "tidspunkt" to Tidspunkt.now().toTimestamp(),
            ),
        ).asUpdate,
    )

    private val tilNotat: Row.() -> Notat = {
        Notat(
            id = long("id"),
            sakId = long("sak_id"),
            referanse = stringOrNull("referanse"),
            journalpostId = stringOrNull("journalpost_id"),
            tittel = string("tittel"),
            opprettet = tidspunkt("opprettet"),
        )
    }
}
