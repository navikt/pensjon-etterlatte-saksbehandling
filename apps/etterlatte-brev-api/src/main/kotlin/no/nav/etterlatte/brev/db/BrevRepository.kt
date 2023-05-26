package no.nav.etterlatte.brev.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_FOR_BEHANDLING_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_QUERY
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
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.util.*
import javax.sql.DataSource

class BrevRepository(private val ds: DataSource) {

    fun hentBrev(id: BrevID): Brev = using(sessionOf(ds)) {
        it.run(queryOf(HENT_BREV_QUERY, id).map(tilBrev).asSingle)
    }!!

    fun hentBrevInnhold(id: BrevID): BrevInnhold? = using(sessionOf(ds)) {
        it.run(queryOf("SELECT * FROM innhold WHERE brev_id = ?", id).map(tilInnhold).asSingle)
    }

    fun hentBrevPayload(id: BrevID): Slate? = using(sessionOf(ds)) {
        it.run(queryOf("SELECT payload FROM innhold WHERE brev_id = ?", id).map(tilPayload).asSingle)
    }

    fun hentBrevForBehandling(behandlingId: UUID): Brev? = using(sessionOf(ds)) {
        it.run(queryOf(HENT_BREV_FOR_BEHANDLING_QUERY, behandlingId).map(tilBrev).asSingle)
    }

    fun opprettInnholdOgFerdigstill(id: BrevID, innhold: BrevInnhold) {
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    OPPRETT_INNHOLD_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "mal" to "",
                        "spraak" to innhold.spraak?.name,
                        "bytes" to innhold.data
                    )
                ).asUpdate
            ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(id, Status.FERDIGSTILT)
        }
    }

    fun opprettEllerOppdaterPayload(id: BrevID, payload: Slate) = ds.transaction { tx ->
        tx.run(
            queryOf(
                Queries.OPPRETT_ELLER_OPPDATER_INNHOLD_PAYLOAD,
                mapOf(
                    "brev_id" to id,
                    "spraak" to Spraak.NB.name,
                    "payload" to payload.toJson()
                )
            ).asUpdate
        ).also { require(it == 1) }

        tx.lagreHendelse(id, Status.OPPDATERT, payload.toJson())
            .also { require(it == 1) }
    }

    fun ferdigstillManueltBrevInnhold(id: BrevID, pdfBytes: ByteArray) {
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    Queries.LAGRE_PDF_INNHOLD_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "bytes" to pdfBytes
                    )
                ).asUpdate
            ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(id, Status.FERDIGSTILT)
        }
    }

    fun opprettBrev(ulagretBrev: OpprettNyttBrev): Brev = ds.transaction(true) { tx ->
        val id = tx.run(
            queryOf(
                OPPRETT_BREV_QUERY,
                mapOf(
                    "behandling_id" to ulagretBrev.behandlingId,
                    "prosess_type" to ulagretBrev.prosessType.name,
                    "soeker_fnr" to ulagretBrev.soekerFnr,
                    "tittel" to ulagretBrev.tittel
                )
            ).asUpdateAndReturnGeneratedKey
        )

        requireNotNull(id) { "Brev ikke opprettet!" }

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

        tx.lagreHendelse(id, Status.OPPRETTET)
            .also { oppdatert -> require(oppdatert == 1) }

        Brev.fra(id, ulagretBrev)
    }

    fun settBrevJournalfoert(brevId: BrevID, journalpostResponse: JournalpostResponse): Boolean =
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    "UPDATE brev SET journalpost_id = ? WHERE id = ?",
                    journalpostResponse.journalpostId,
                    brevId
                ).asUpdate
            ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(brevId, Status.JOURNALFOERT, journalpostResponse.toJson()) > 0
        }

    fun settBrevDistribuert(brevId: Long, distResponse: DistribuerJournalpostResponse): Boolean =
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    "UPDATE brev SET bestilling_id = ? WHERE id = ?",
                    distResponse.bestillingsId,
                    brevId
                ).asUpdate
            ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(brevId, Status.DISTRIBUERT, distResponse.toJson()) > 0
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
            prosessType = BrevProsessType.valueOf(row.string("prosess_type")),
            tittel = row.string("tittel"),
            status = row.string("status_id").let { Status.valueOf(it) },
            mottaker = Mottaker(
                navn = row.string("navn"),
                foedselsnummer = row.stringOrNull("foedselsnummer")?.let { Foedselsnummer(it) },
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
            )
        )
    }

    private val tilInnhold: (Row) -> BrevInnhold = { row ->
        BrevInnhold(
            row.stringOrNull("spraak")?.let { Spraak.valueOf(it) },
            row.stringOrNull("payload")?.let { deserialize<Slate>(it) },
            row.bytesOrNull("bytes")
        )
    }

    private val tilPayload: (Row) -> Slate? = { row ->
        row.stringOrNull("payload")?.let { deserialize<Slate>(it) }
    }

    // Spesifisere SQL som språk for å sikre formatering/styling i IntelliJ
    // language=SQL
    private object Queries {
        const val HENT_BREV_QUERY = """
            SELECT b.id, b.behandling_id, b.prosess_type, b.soeker_fnr, b.tittel, h.status_id, m.*
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
            SELECT b.id, b.behandling_id, b.prosess_type, b.soeker_fnr, b.tittel, h.status_id, m.*
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
            INSERT INTO brev (behandling_id, prosess_type, soeker_fnr, tittel) 
            VALUES (:behandling_id, :prosess_type, :soeker_fnr, :tittel) 
            ON CONFLICT DO NOTHING;
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
            INSERT INTO innhold (brev_id, mal, spraak, payload, bytes) 
            VALUES (:brev_id, :mal, :spraak, :payload, :bytes)
        """

        const val LAGRE_PDF_INNHOLD_QUERY = """
            UPDATE innhold 
            SET bytes = :bytes
            WHERE brev_id = :brev_id
        """

        const val OPPRETT_ELLER_OPPDATER_INNHOLD_PAYLOAD = """
            INSERT INTO innhold (brev_id, payload) 
            VALUES (:brev_id, :payload)
            ON CONFLICT (brev_id) DO UPDATE  
            SET payload = :payload
        """

        const val OPPRETT_HENDELSE_QUERY = """
            INSERT INTO hendelse (brev_id, status_id, payload) 
            VALUES (:brev_id, :status_id, :payload)
        """
    }
}