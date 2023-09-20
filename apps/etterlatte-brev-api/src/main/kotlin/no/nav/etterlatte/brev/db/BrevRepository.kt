package no.nav.etterlatte.brev.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_FOR_BEHANDLING_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_FOR_SAK_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_INNHOLD_PAYLOAD
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_INNHOLD_PAYLOAD_VEDLEGG
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_MOTTAKER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_HENDELSE_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_INNHOLD_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_MOTTAKER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_PDF_QUERY
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.util.UUID
import javax.sql.DataSource

class BrevRepository(private val ds: DataSource) {
    fun hentBrev(id: BrevID): Brev =
        using(sessionOf(ds)) {
            it.run(queryOf(HENT_BREV_QUERY, id).map(tilBrev).asSingle)
        }!!

    fun hentBrevInnhold(id: BrevID): BrevInnhold? =
        using(sessionOf(ds)) {
            it.run(queryOf("SELECT * FROM innhold WHERE brev_id = ?", id).map(tilInnhold).asSingle)
        }

    fun hentPdf(id: BrevID): Pdf? =
        using(sessionOf(ds)) {
            it.run(queryOf("SELECT bytes FROM pdf WHERE brev_id = ?", id).map(tilPdf).asSingle)
        }

    fun hentBrevPayload(id: BrevID): Slate? =
        using(sessionOf(ds)) {
            it.run(queryOf("SELECT payload FROM innhold WHERE brev_id = ?", id).map(tilPayload).asSingle)
        }

    fun hentBrevPayloadVedlegg(id: BrevID): List<BrevInnholdVedlegg>? =
        using(sessionOf(ds)) {
            it.run(queryOf("SELECT payload_vedlegg FROM innhold WHERE brev_id = ?", id).map(tilPayloadVedlegg).asSingle)
        }

    fun hentBrevForBehandling(behandlingId: UUID): Brev? =
        using(sessionOf(ds)) {
            it.run(queryOf(HENT_BREV_FOR_BEHANDLING_QUERY, behandlingId).map(tilBrev).asSingle)
        }

    fun hentBrevForSak(sakId: Long): List<Brev> =
        using(sessionOf(ds)) {
            it.run(queryOf(HENT_BREV_FOR_SAK_QUERY, sakId).map(tilBrev).asList)
        }

    fun oppdaterPayload(
        id: BrevID,
        payload: Slate,
    ) = ds.transaction { tx ->
        tx.run(
            queryOf(
                OPPDATER_INNHOLD_PAYLOAD,
                mapOf(
                    "brev_id" to id,
                    "spraak" to Spraak.NB.name,
                    "payload" to payload.toJson(),
                ),
            ).asUpdate,
        ).also { require(it == 1) }

        tx.lagreHendelse(id, Status.OPPDATERT, payload.toJson())
            .also { require(it == 1) }
    }

    fun oppdaterPayloadVedlegg(
        id: BrevID,
        payload: List<BrevInnholdVedlegg>,
    ) = ds.transaction { tx ->
        tx.run(
            queryOf(
                OPPDATER_INNHOLD_PAYLOAD_VEDLEGG,
                mapOf(
                    "brev_id" to id,
                    "spraak" to Spraak.NB.name,
                    "payload_vedlegg" to payload.toJson(),
                ),
            ).asUpdate,
        ).also { require(it == 1) }

        tx.lagreHendelse(id, Status.OPPDATERT, payload.toJson())
            .also { require(it == 1) }
    }

    fun oppdaterMottaker(
        id: BrevID,
        mottaker: Mottaker,
    ) = ds.transaction { tx ->
        tx.run(
            queryOf(
                OPPDATER_MOTTAKER_QUERY,
                mapOf(
                    "brev_id" to id,
                    "foedselsnummer" to mottaker.foedselsnummer?.value,
                    "orgnummer" to mottaker.orgnummer,
                    "navn" to mottaker.navn,
                    "adressetype" to mottaker.adresse.adresseType,
                    "adresselinje1" to mottaker.adresse.adresselinje1,
                    "adresselinje2" to mottaker.adresse.adresselinje2,
                    "adresselinje3" to mottaker.adresse.adresselinje3,
                    "postnummer" to mottaker.adresse.postnummer,
                    "poststed" to mottaker.adresse.poststed,
                    "landkode" to mottaker.adresse.landkode,
                    "land" to mottaker.adresse.land,
                ),
            ).asUpdate,
        ).also { require(it == 1) }

        tx.lagreHendelse(id, Status.OPPDATERT, mottaker.toJson())
            .also { require(it == 1) }
    }

    fun lagrePdfOgFerdigstillBrev(
        id: BrevID,
        pdf: Pdf,
    ) {
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    OPPRETT_PDF_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "bytes" to pdf.bytes,
                    ),
                ).asUpdate,
            ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(id, Status.FERDIGSTILT)
        }
    }

    fun opprettBrev(ulagretBrev: OpprettNyttBrev): Brev =
        ds.transaction(true) { tx ->
            val id =
                tx.run(
                    queryOf(
                        OPPRETT_BREV_QUERY,
                        mapOf(
                            "sak_id" to ulagretBrev.sakId,
                            "behandling_id" to ulagretBrev.behandlingId,
                            "prosess_type" to ulagretBrev.prosessType.name,
                            "soeker_fnr" to ulagretBrev.soekerFnr,
                        ),
                    ).asUpdateAndReturnGeneratedKey,
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
                        "land" to ulagretBrev.mottaker.adresse.land,
                    ),
                ).asUpdate,
            ).also { opprettet -> require(opprettet == 1) }

            tx.run(
                queryOf(
                    OPPRETT_INNHOLD_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "tittel" to ulagretBrev.innhold.tittel,
                        "spraak" to ulagretBrev.innhold.spraak.name,
                        "payload" to ulagretBrev.innhold.payload?.toJson(),
                        "payload_vedlegg" to ulagretBrev.innholdVedlegg?.toJson(),
                    ),
                ).asUpdate,
            ).also { opprettet -> require(opprettet == 1) }

            tx.lagreHendelse(id, Status.OPPRETTET)
                .also { oppdatert -> require(oppdatert == 1) }

            Brev.fra(id, ulagretBrev)
        }

    fun settBrevJournalfoert(
        brevId: BrevID,
        journalpostResponse: JournalpostResponse,
    ): Boolean =
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    "UPDATE brev SET journalpost_id = ? WHERE id = ?",
                    journalpostResponse.journalpostId,
                    brevId,
                ).asUpdate,
            ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(brevId, Status.JOURNALFOERT, journalpostResponse.toJson()) > 0
        }

    fun hentJournalpostId(brevId: BrevID): String? =
        using(sessionOf(ds)) {
            it.run(
                queryOf("SELECT journalpost_id FROM brev WHERE id = ?", brevId)
                    .map { row -> row.string("journalpost_id") }.asSingle,
            )
        }

    fun settBrevDistribuert(
        brevId: Long,
        distResponse: DistribuerJournalpostResponse,
    ): Boolean =
        ds.transaction { tx ->
            tx.run(
                queryOf(
                    "UPDATE brev SET bestilling_id = ? WHERE id = ?",
                    distResponse.bestillingsId,
                    brevId,
                ).asUpdate,
            ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(brevId, Status.DISTRIBUERT, distResponse.toJson()) > 0
        }

    fun slett(id: BrevID): Boolean =
        using(sessionOf(ds)) {
            it.run(queryOf("DELETE FROM brev WHERE id = ?", id).asUpdate) > 0
        }

    private fun Session.lagreHendelse(
        brevId: BrevID,
        status: Status,
        payload: String = "{}",
    ) = run(
        queryOf(
            OPPRETT_HENDELSE_QUERY,
            mapOf(
                "brev_id" to brevId,
                "status_id" to status.name,
                "payload" to payload,
            ),
        ).asUpdate,
    )

    private val tilBrev: (Row) -> Brev = { row ->
        Brev(
            id = row.long("id"),
            sakId = row.long("sak_id"),
            behandlingId = row.uuidOrNull("behandling_id"),
            soekerFnr = row.string("soeker_fnr"),
            prosessType = BrevProsessType.valueOf(row.string("prosess_type")),
            status = row.string("status_id").let { Status.valueOf(it) },
            mottaker =
                Mottaker(
                    navn = row.string("navn"),
                    foedselsnummer = row.stringOrNull("foedselsnummer")?.let { Foedselsnummer(it) },
                    orgnummer = row.stringOrNull("orgnummer"),
                    adresse =
                        Adresse(
                            adresseType = row.string("adressetype"),
                            adresselinje1 = row.stringOrNull("adresselinje1"),
                            adresselinje2 = row.stringOrNull("adresselinje2"),
                            adresselinje3 = row.stringOrNull("adresselinje3"),
                            postnummer = row.stringOrNull("postnummer"),
                            poststed = row.stringOrNull("poststed"),
                            landkode = row.string("landkode"),
                            land = row.string("land"),
                        ),
                ),
        )
    }

    private val tilInnhold: (Row) -> BrevInnhold = { row ->
        BrevInnhold(
            row.stringOrNull("tittel") ?: "Tittel mangler",
            row.string("spraak").let { Spraak.valueOf(it) },
            row.stringOrNull("payload")?.let { deserialize<Slate>(it) },
        )
    }

    private val tilPdf: (Row) -> Pdf? = { row -> Pdf(row.bytes("bytes")) }

    private val tilPayload: (Row) -> Slate? = { row ->
        row.stringOrNull("payload")?.let { deserialize<Slate>(it) }
    }

    private val tilPayloadVedlegg: (Row) -> List<BrevInnholdVedlegg>? = { row ->
        row.stringOrNull("payload_vedlegg")?.let { deserialize<List<BrevInnholdVedlegg>>(it) }
    }

    // Spesifisere SQL som språk for å sikre formatering/styling i IntelliJ
    // language=SQL
    private object Queries {
        const val HENT_BREV_QUERY = """
            SELECT b.id, b.sak_id, b.behandling_id, b.prosess_type, b.soeker_fnr, h.status_id, m.*
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
            SELECT b.id, b.sak_id, b.behandling_id, b.prosess_type, b.soeker_fnr, h.status_id, m.*
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

        const val HENT_BREV_FOR_SAK_QUERY = """
            SELECT b.id, b.sak_id, b.behandling_id, b.prosess_type, b.soeker_fnr, h.status_id, m.*
            FROM brev b
            INNER JOIN mottaker m on b.id = m.brev_id
            INNER JOIN hendelse h on b.id = h.brev_id
            WHERE b.sak_id = ?
            AND h.id IN (
                SELECT DISTINCT ON (brev_id) id
                FROM hendelse
                ORDER BY brev_id, opprettet DESC
            )
        """

        const val OPPRETT_BREV_QUERY = """
            INSERT INTO brev (sak_id, behandling_id, prosess_type, soeker_fnr) 
            VALUES (:sak_id, :behandling_id, :prosess_type, :soeker_fnr) 
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
            WHERE brev_id = :brev_id
        """

        const val OPPRETT_INNHOLD_QUERY = """
            INSERT INTO innhold (brev_id, tittel, spraak, payload, payload_vedlegg) 
            VALUES (:brev_id, :tittel, :spraak, :payload, :payload_vedlegg)
        """

        const val OPPRETT_PDF_QUERY = """
            INSERT INTO pdf (brev_id, bytes) 
            VALUES (:brev_id, :bytes)
        """

        const val OPPDATER_INNHOLD_PAYLOAD = """
            UPDATE innhold
            SET payload = :payload
            WHERE brev_id = :brev_id
        """

        const val OPPDATER_INNHOLD_PAYLOAD_VEDLEGG = """
            UPDATE innhold
            SET payload_vedlegg = :payload_vedlegg
            WHERE brev_id = :brev_id
        """

        const val OPPRETT_HENDELSE_QUERY = """
            INSERT INTO hendelse (brev_id, status_id, payload) 
            VALUES (:brev_id, :status_id, :payload)
        """
    }
}
