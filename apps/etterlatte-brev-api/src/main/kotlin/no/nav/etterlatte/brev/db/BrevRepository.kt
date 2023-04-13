package no.nav.etterlatte.brev.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_FOR_BEHANDLING_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_INNHOLD_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_MOTTAKER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_HENDELSE_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_INNHOLD_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_MOTTAKER_QUERY
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.toJson
import java.util.*
import javax.sql.DataSource

class BrevRepository(private val ds: DataSource) {

    fun hentBrev(id: BrevID): Brev = using(sessionOf(ds)) {
        it.run(queryOf(HENT_BREV_QUERY, id).map(tilBrev).asSingle)
    }!!

    fun hentBrevInnhold(id: BrevID): BrevInnhold = using(sessionOf(ds)) {
        it.run(queryOf("SELECT * FROM innhold WHERE brev_id = ?", id).map(tilInnhold).asSingle)
    }!!

    fun hentBrevForBehandling(behandlingId: UUID): List<Brev> = using(sessionOf(ds)) {
        it.run(queryOf(HENT_BREV_FOR_BEHANDLING_QUERY, behandlingId).map(tilBrev).asList)
    }

    fun oppdaterBrev(brevId: Long, brev: UlagretBrev) {
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        OPPDATER_BREV_QUERY,
                        mapOf(
                            "id" to brevId,
                            "tittel" to brev.tittel,
                            "vedtaksbrev" to brev.erVedtaksbrev
                        )
                    ).asUpdate
                ).also { oppdatert -> require(oppdatert == 1) }

                tx.run(
                    queryOf(
                        OPPDATER_INNHOLD_QUERY,
                        mapOf(
                            "brev_id" to brevId,
                            "mal" to "",
                            "spraak" to brev.spraak.name,
                            "bytes" to brev.pdf
                        )
                    ).asUpdate
                ).also { oppdatert -> require(oppdatert == 1) }

                tx.run(
                    queryOf(
                        OPPDATER_MOTTAKER_QUERY,
                        mapOf(
                            "id" to brevId,
                            "foedselsnummer" to brev.mottaker.foedselsnummer?.value,
                            "orgnummer" to brev.mottaker.orgnummer,
                            "navn" to brev.mottaker.navn,
                            "adresselinje1" to brev.mottaker.adresse.adresselinje1,
                            "adresselinje2" to brev.mottaker.adresse.adresselinje2,
                            "adresselinje3" to brev.mottaker.adresse.adresselinje3,
                            "postnummer" to brev.mottaker.adresse.postnummer,
                            "poststed" to brev.mottaker.adresse.poststed,
                            "landkode" to brev.mottaker.adresse.landkode,
                            "land" to brev.mottaker.adresse.land
                        )
                    ).asUpdate
                ).also { oppdatert -> require(oppdatert == 1) }

                tx.lagreHendelse(brevId, Status.OPPDATERT)
            }
        }
    }

    fun opprettBrev(ulagretBrev: UlagretBrev): Brev = using(sessionOf(ds, returnGeneratedKey = true)) { session ->
        session.transaction { tx ->
            val id = tx.run(
                queryOf(
                    OPPRETT_BREV_QUERY,
                    mapOf(
                        "behandling_id" to ulagretBrev.behandlingId,
                        "soeker_fnr" to ulagretBrev.soekerFnr,
                        "tittel" to ulagretBrev.tittel,
                        "vedtaksbrev" to ulagretBrev.erVedtaksbrev
                    )
                ).asUpdateAndReturnGeneratedKey
            )!!

            tx.run(
                queryOf(
                    OPPRETT_MOTTAKER_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "foedselsnummer" to ulagretBrev.mottaker.foedselsnummer?.value,
                        "orgnummer" to ulagretBrev.mottaker.orgnummer,
                        "navn" to ulagretBrev.mottaker.navn,
                        "adressetype" to ulagretBrev.mottaker.adresse.adresseType,
                        "adresselinje1" to ulagretBrev.mottaker.adresse.adresselinje1,
                        "adresselinje2" to ulagretBrev.mottaker.adresse.adresselinje2,
                        "adresselinje3" to ulagretBrev.mottaker.adresse.adresselinje3,
                        "postnummer" to ulagretBrev.mottaker.adresse.postnummer,
                        "poststed" to ulagretBrev.mottaker.adresse.poststed,
                        "landkode" to ulagretBrev.mottaker.adresse.landkode,
                        "land" to ulagretBrev.mottaker.adresse.land
                    )
                ).asUpdate
            )

            tx.run(
                queryOf(
                    OPPRETT_INNHOLD_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "mal" to "",
                        "spraak" to ulagretBrev.spraak.name,
                        "bytes" to ulagretBrev.pdf
                    )
                ).asUpdate
            )

            tx.lagreHendelse(id, Status.OPPRETTET)
                .also { oppdatert -> require(oppdatert == 1) }

            Brev.fraUlagretBrev(id, ulagretBrev)
        }
    }

    fun settBrevJournalfoert(brevId: BrevID, journalpostResponse: JournalpostResponse): Boolean =
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "UPDATE brev SET journalpost_id = ? WHERE id = ?",
                        journalpostResponse.journalpostId,
                        brevId
                    ).asUpdate
                ).also { oppdatert -> require(oppdatert == 1) }

                tx.lagreHendelse(brevId, Status.JOURNALFOERT, journalpostResponse.toJson()) > 0
            }
        }

    fun settBrevDistribuert(brevId: Long, distResponse: DistribuerJournalpostResponse): Boolean =
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "UPDATE brev SET bestilling_id = ? WHERE id = ?",
                        distResponse.bestillingsId,
                        brevId
                    ).asUpdate
                ).also { oppdatert -> require(oppdatert == 1) }

                tx.lagreHendelse(brevId, Status.DISTRIBUERT, distResponse.toJson()) > 0
            }
        }

    fun settBrevFerdigstilt(id: BrevID): Boolean = using(sessionOf(ds)) {
        it.lagreHendelse(id, Status.FERDIGSTILT) > 0
    }

    fun slett(id: BrevID): Boolean = using(sessionOf(ds)) {
        it.run(queryOf("DELETE FROM brev WHERE id = ?", id).asUpdate) > 0
    }

    private fun Session.lagreHendelse(brevId: BrevID, status: Status, payload: String = "{}") = run(
        queryOf(
            OPPRETT_HENDELSE_QUERY,
            mapOf(
                "brev_id" to brevId,
                "status_id" to status.name,
                "payload" to payload
            )
        ).asUpdate
    )

    private val tilBrev: (Row) -> Brev = { row ->
        Brev(
            id = row.long("id"),
            behandlingId = row.uuid("behandling_id"),
            soekerFnr = row.string("soeker_fnr"),
            tittel = row.string("tittel"),
            status = row.string("status_id").let { Status.valueOf(it) },
            mottaker = Mottaker(
                navn = row.string("navn"),
                foedselsnummer = row.stringOrNull("foedselsnummer")?.let { Folkeregisteridentifikator.of(it) },
                orgnummer = row.stringOrNull("orgnummer"),
                adresse = Adresse(
                    adresseType = row.string("adressetype"),
                    adresselinje1 = row.stringOrNull("adresselinje1"),
                    adresselinje2 = row.stringOrNull("adresselinje2"),
                    adresselinje3 = row.stringOrNull("adresselinje3"),
                    postnummer = row.stringOrNull("postnummer"),
                    poststed = row.stringOrNull("poststed"),
                    landkode = row.string("landkode"),
                    land = row.string("land")
                )
            ),
            erVedtaksbrev = row.boolean("vedtaksbrev")
        )
    }

    private val tilInnhold: (Row) -> BrevInnhold = { row ->
        BrevInnhold(
            row.string("mal"),
            row.string("spraak").let { Spraak.valueOf(it) },
            row.bytes("bytes")
        )
    }

    private object Queries {
        const val HENT_BREV_QUERY = """
            SELECT b.id, b.behandling_id, b.soeker_fnr, b.tittel, b.vedtaksbrev, h.status_id, m.*
            FROM brev b
            INNER JOIN mottaker m on b.id = m.brev_id
            INNER JOIN hendelse h on b.id = h.brev_id
            WHERE b.id = ?
            AND h.id IN (
                SELECT DISTINCT ON (brev_id) id
                FROM hendelse
                ORDER  BY brev_id, opprettet DESC
            )
        """

        const val HENT_BREV_FOR_BEHANDLING_QUERY = """
            SELECT b.id, b.behandling_id, b.soeker_fnr, b.tittel, b.vedtaksbrev, h.status_id, m.*
            FROM brev b
            INNER JOIN mottaker m on b.id = m.brev_id
            INNER JOIN hendelse h on b.id = h.brev_id
            WHERE b.behandling_id = ?
            AND h.id IN (
                SELECT DISTINCT ON (brev_id) id
                FROM hendelse
                ORDER BY brev_id, opprettet DESC
            )
        """

        const val OPPRETT_BREV_QUERY = """
            INSERT INTO brev (behandling_id, soeker_fnr, tittel, vedtaksbrev) 
            VALUES (:behandling_id, :soeker_fnr, :tittel, :vedtaksbrev) 
        """

        const val OPPRETT_MOTTAKER_QUERY = """
            INSERT INTO mottaker (
                brev_id, foedselsnummer, orgnummer, navn, 
                adressetype, adresselinje1, adresselinje2, adresselinje3, 
                postnummer, poststed, landkode, land
            ) VALUES (:brev_id, :foedselsnummer, :orgnummer, :navn,
                :adressetype, :adresselinje1, :adresselinje2, :adresselinje3,
                :postnummer, :poststed, :landkode, :land
            )
        """

        const val OPPRETT_INNHOLD_QUERY = """
            INSERT INTO innhold (brev_id, mal, spraak, bytes) 
            VALUES (:brev_id, :mal, :spraak, :bytes)
        """

        const val OPPDATER_INNHOLD_QUERY = """
            UPDATE innhold 
            SET mal = :mal, 
                spraak = :spraak, 
                bytes = :bytes
            WHERE brev_id = :brev_id
        """

        const val OPPDATER_MOTTAKER_QUERY = """
            UPDATE mottaker 
            SET foedselsnummer = :foedselsnummer, 
                orgnummer = :orgnummer, 
                navn = :navn, 
                adressetype = :adressetype, 
                adresselinje1 = :adresselinje1, 
                adresselinje2 = :adresselinje2, 
                adresselinje3 = :adresselinje3,
                postnummer = :postnummer, 
                poststed = :poststed, 
                landkode = :landkode, 
                land = :land
            WHERE brev_id = :id
        """

        const val OPPDATER_BREV_QUERY = """
            UPDATE brev 
            SET tittel = :tittel, 
                vedtaksbrev = :vedtaksbrev 
            WHERE id = :id
        """

        const val OPPRETT_HENDELSE_QUERY = """
            INSERT INTO hendelse (brev_id, status_id, payload) 
            VALUES (:brev_id, :status_id, :payload)
        """
    }
}