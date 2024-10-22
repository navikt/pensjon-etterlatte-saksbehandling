package no.nav.etterlatte.brev.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREVKODER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_FOR_BEHANDLING_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_FOR_SAK_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_BREVKODER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_INNHOLD_PAYLOAD
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_INNHOLD_PAYLOAD_VEDLEGG
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_MOTTAKER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_SPRAAK_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_TITTEL_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_ELLER_OPPDATER_PDF_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_HENDELSE_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_INNHOLD_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_MOTTAKER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_PDF_QUERY
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.opprettBrevFra
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID
import javax.sql.DataSource

class BrevRepository(
    private val ds: DataSource,
) {
    fun hentBrev(id: BrevID): Brev =
        using(sessionOf(ds)) { session ->
            session.single(queryOf(HENT_BREV_QUERY, id)) { brevRow ->
                val mottakere =
                    session.list(
                        queryOf("SELECT * FROM mottaker WHERE brev_id = ?", brevRow.long("id")),
                        tilMottaker,
                    )

                brevRow.tilBrev(mottakere)
            }
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

    fun hentBrevkoder(id: BrevID) = ds.hent(HENT_BREVKODER_QUERY, id) { it.stringOrNull("brevkoder")?.let { Brevkoder.valueOf(it) } }

    fun hentBrevForBehandling(
        behandlingId: UUID,
        type: Brevtype,
    ): List<Brev> =
        using(sessionOf(ds)) { session ->
            session.list(queryOf(HENT_BREV_FOR_BEHANDLING_QUERY, behandlingId, type.name)) { brevRow ->
                val mottakere =
                    session.list(
                        queryOf("SELECT * FROM mottaker WHERE brev_id = ?", brevRow.long("id")),
                        tilMottaker,
                    )

                brevRow.tilBrev(mottakere)
            }
        }

    fun hentBrevForSak(sakId: SakId): List<Brev> =
        using(sessionOf(ds)) { session ->
            session.list(queryOf(HENT_BREV_FOR_SAK_QUERY, sakId.sakId)) { brevRow ->
                val mottakere =
                    session.list(
                        queryOf("SELECT * FROM mottaker WHERE brev_id = ?", brevRow.long("id")),
                        tilMottaker,
                    )

                brevRow.tilBrev(mottakere)
            }
        }

    fun oppdaterPayload(
        id: BrevID,
        payload: Slate,
    ) = ds.transaction { tx ->
        tx
            .run(
                queryOf(
                    OPPDATER_INNHOLD_PAYLOAD,
                    mapOf(
                        "brev_id" to id,
                        "spraak" to Spraak.NB.name,
                        "payload" to payload.toJson(),
                    ),
                ).asUpdate,
            ).also { require(it == 1) }

        tx
            .lagreHendelse(id, Status.OPPDATERT, payload.toJson())
            .also { require(it == 1) }
    }

    fun oppdaterPayloadVedlegg(
        id: BrevID,
        payload: List<BrevInnholdVedlegg>,
    ) = ds.transaction { tx ->
        tx
            .run(
                queryOf(
                    OPPDATER_INNHOLD_PAYLOAD_VEDLEGG,
                    mapOf(
                        "brev_id" to id,
                        "spraak" to Spraak.NB.name,
                        "payload_vedlegg" to payload.toJson(),
                    ),
                ).asUpdate,
            ).also { require(it == 1) }

        tx
            .lagreHendelse(id, Status.OPPDATERT, payload.toJson())
            .also { require(it == 1) }
    }

    fun oppdaterBrevkoder(
        id: BrevID,
        brevkoder: Brevkoder,
    ) = ds.transaction { tx ->
        tx.oppdater(
            OPPDATER_BREVKODER_QUERY,
            mapOf("id" to id, "brevkoder" to brevkoder.name),
            "Oppdaterer brevkoder for brev $id til $brevkoder",
        )
    }

    fun opprettMottaker(
        id: BrevID,
        mottaker: Mottaker,
    ) = using(sessionOf(ds)) {
        it.run(
            queryOf(
                OPPRETT_MOTTAKER_QUERY,
                mapOf(
                    "id" to mottaker.id,
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
                    "type" to mottaker.type.name,
                ),
            ).asExecute,
        )
    }

    fun oppdaterMottaker(
        id: BrevID,
        mottaker: Mottaker,
    ) = ds.transaction { tx ->
        tx
            .run(
                queryOf(
                    OPPDATER_MOTTAKER_QUERY,
                    mapOf(
                        "id" to mottaker.id,
                        "brev_id" to id,
                        "foedselsnummer" to mottaker.foedselsnummer?.value?.let { it.ifBlank { null } },
                        "orgnummer" to mottaker.orgnummer?.let { it.ifBlank { null } },
                        "navn" to mottaker.navn,
                        "adressetype" to mottaker.adresse.adresseType,
                        "adresselinje1" to mottaker.adresse.adresselinje1,
                        "adresselinje2" to mottaker.adresse.adresselinje2,
                        "adresselinje3" to mottaker.adresse.adresselinje3,
                        "postnummer" to mottaker.adresse.postnummer,
                        "poststed" to mottaker.adresse.poststed,
                        "landkode" to mottaker.adresse.landkode,
                        "land" to mottaker.adresse.land,
                        "tving_sentral_print" to mottaker.tvingSentralPrint,
                        "type" to mottaker.type.name,
                    ),
                ).asUpdate,
            ).also { require(it == 1) }

        tx
            .lagreHendelse(id, Status.OPPDATERT, mottaker.toJson())
            .also { require(it == 1) }
    }

    fun slettMottaker(
        brevId: BrevID,
        mottakerId: UUID,
    ) = ds.transaction { tx ->
        tx
            .run(queryOf("DELETE FROM mottaker WHERE id = ? AND brev_id = ?", mottakerId, brevId).asUpdate)
            .also { require(it == 1) }

        tx
            .lagreHendelse(brevId, Status.OPPDATERT, "mottaker med id=$mottakerId fjernet fra brevet ")
            .also { require(it == 1) }
    }

    fun oppdaterTittel(
        id: BrevID,
        tittel: String,
    ) = ds.transaction { tx ->
        tx
            .run(
                queryOf(
                    OPPDATER_TITTEL_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "tittel" to tittel,
                    ),
                ).asUpdate,
            ).also { require(it == 1) }

        tx
            .lagreHendelse(id, Status.OPPDATERT, tittel.toJson())
            .also { require(it == 1) }
    }

    fun oppdaterSpraak(
        id: BrevID,
        spraak: Spraak,
    ) = ds.transaction { tx ->
        tx
            .run(
                queryOf(
                    OPPDATER_SPRAAK_QUERY,
                    mapOf(
                        "brev_id" to id,
                        "spraak" to spraak.name,
                    ),
                ).asUpdate,
            ).also { require(it == 1) }

        tx
            .lagreHendelse(id, Status.OPPDATERT, spraak.toJson())
            .also { require(it == 1) }
    }

    fun lagrePdfOgFerdigstillBrev(
        id: BrevID,
        pdf: Pdf,
    ) {
        ds.transaction { tx ->
            tx
                .run(
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

    fun lagrePdf(
        id: BrevID,
        pdf: Pdf,
    ) {
        using(sessionOf(ds)) {
            it
                .run(
                    queryOf(
                        OPPRETT_ELLER_OPPDATER_PDF_QUERY,
                        mapOf(
                            "brev_id" to id,
                            "bytes" to pdf.bytes,
                        ),
                    ).asUpdate,
                ).also { oppdatert -> require(oppdatert == 1) }
        }
    }

    fun settBrevFerdigstilt(id: BrevID) {
        using(sessionOf(ds)) {
            it.lagreHendelse(id, Status.FERDIGSTILT)
        }
    }

    fun settBrevUtgaatt(
        id: BrevID,
        kommentar: String,
        bruker: BrukerTokenInfo,
    ) {
        using(sessionOf(ds)) {
            it.lagreHendelse(id, Status.UTGAATT, "${bruker.ident()}: $kommentar".toJson())
        }
    }

    fun opprettBrev(ulagretBrev: OpprettNyttBrev): Brev =
        ds.transaction(true) { tx ->
            val id =
                tx.run(
                    queryOf(
                        OPPRETT_BREV_QUERY,
                        mapOf(
                            "sak_id" to ulagretBrev.sakId.sakId,
                            "behandling_id" to ulagretBrev.behandlingId,
                            "prosess_type" to ulagretBrev.prosessType.name,
                            "soeker_fnr" to ulagretBrev.soekerFnr,
                            "opprettet" to ulagretBrev.opprettet.toTimestamp(),
                            "brevtype" to ulagretBrev.brevtype.name,
                            "brevkoder" to ulagretBrev.brevkoder.name,
                        ),
                    ).asUpdateAndReturnGeneratedKey,
                )

            requireNotNull(id) { "Brev ikke opprettet!" }

            tx
                .run(
                    queryOf(
                        OPPRETT_MOTTAKER_QUERY,
                        mapOf(
                            "id" to ulagretBrev.mottaker.id,
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
                            "type" to ulagretBrev.mottaker.type.name,
                        ),
                    ).asUpdate,
                ).also { opprettet -> require(opprettet == 1) }

            tx
                .run(
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

            tx
                .lagreHendelse(id, Status.OPPRETTET, ulagretBrev.opprettet)
                .also { oppdatert -> require(oppdatert == 1) }

            opprettBrevFra(id, ulagretBrev)
        }

    fun settBrevJournalfoert(
        brevId: BrevID,
        journalpostResponse: OpprettJournalpostResponse,
    ): Boolean =
        ds.transaction { tx ->
            tx
                .run(
                    // TODO EY-3627:
                    //  Må settes med mottaker_id
                    queryOf(
                        "UPDATE mottaker SET journalpost_id = ? WHERE brev_id = ?",
                        journalpostResponse.journalpostId,
                        brevId,
                    ).asUpdate,
                ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(brevId, Status.JOURNALFOERT, journalpostResponse.toJson()) > 0
        }

    fun hentJournalpostId(brevId: BrevID): String? =
        using(sessionOf(ds)) {
            it.run(
                // TODO EY-3627:
                //  Må hentes med mottaker_id
                queryOf("SELECT journalpost_id FROM mottaker WHERE brev_id = ?", brevId)
                    .map { row -> row.string("journalpost_id") }
                    .asSingle,
            )
        }

    fun settBrevDistribuert(
        brevId: Long,
        distResponse: DistribuerJournalpostResponse,
    ): Boolean =
        ds.transaction { tx ->
            tx
                .run(
                    // TODO EY-3627:
                    //  Må settes med mottaker_id
                    queryOf(
                        "UPDATE mottaker SET bestilling_id = ? WHERE brev_id = ?",
                        distResponse.bestillingsId,
                        brevId,
                    ).asUpdate,
                ).also { oppdatert -> require(oppdatert == 1) }

            tx.lagreHendelse(brevId, Status.DISTRIBUERT, distResponse.toJson()) > 0
        }

    fun settBrevSlettet(
        brevId: BrevID,
        bruker: BrukerTokenInfo,
    ): Boolean =
        ds.transaction { tx ->
            tx.lagreHendelse(brevId, Status.SLETTET, bruker.ident()) > 0
        }

    private fun Session.lagreHendelse(
        brevId: BrevID,
        status: Status,
        payload: String = "{}",
    ) = lagreHendelse(brevId, status, Tidspunkt.now(), payload)

    private fun Session.lagreHendelse(
        brevId: BrevID,
        status: Status,
        opprettet: Tidspunkt = Tidspunkt.now(),
        payload: String = "{}",
    ) = run(
        queryOf(
            OPPRETT_HENDELSE_QUERY,
            mapOf(
                "brev_id" to brevId,
                "status_id" to status.name,
                "payload" to payload,
                "opprettet" to opprettet.toTimestamp(),
            ),
        ).asUpdate,
    )

    private val tilBrev: Row.(mottakere: List<Mottaker>) -> Brev = { mottakere ->
        Brev(
            id = long("id"),
            sakId = SakId(long("sak_id")),
            behandlingId = uuidOrNull("behandling_id"),
            soekerFnr = string("soeker_fnr"),
            tittel = stringOrNull("tittel"),
            spraak = Spraak.valueOf(string("spraak")),
            prosessType = BrevProsessType.valueOf(string("prosess_type")),
            status = string("status_id").let { Status.valueOf(it) },
            statusEndret = tidspunkt("hendelse_opprettet"),
            opprettet = tidspunkt("opprettet"),
            mottakere = mottakere,
            brevtype = string("brevtype").let { Brevtype.valueOf(it) },
            brevkoder = stringOrNull("brevkoder")?.let { Brevkoder.valueOf(it) },
        )
    }

    private val tilMottaker: (Row) -> Mottaker = { row ->
        Mottaker(
            id = row.uuid("id"),
            navn = row.string("navn"),
            foedselsnummer = row.stringOrNull("foedselsnummer")?.let { MottakerFoedselsnummer(it) },
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
            tvingSentralPrint = row.boolean("tving_sentral_print"),
            type = MottakerType.valueOf(row.string("type")),
            journalpostId = row.stringOrNull("journalpost_id"),
            bestillingId = row.stringOrNull("bestilling_id"),
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
            SELECT 
                b.id, b.sak_id, b.behandling_id, b.prosess_type, b.soeker_fnr, b.opprettet, h.status_id, 
                h.opprettet as hendelse_opprettet, i.tittel, i.spraak, b.brevtype, b.brevkoder
            FROM brev b
            INNER JOIN hendelse h on b.id = h.brev_id
            INNER JOIN innhold i on b.id = i.brev_id
            WHERE b.id = ?
            AND h.id IN (
                SELECT DISTINCT ON (h2.brev_id) h2.id
                FROM hendelse h2
                WHERE h2.brev_id = b.id
                ORDER BY h2.brev_id, h2.opprettet DESC
            )
        """

        const val HENT_BREVKODER_QUERY = "SELECT brevkoder FROM brev WHERE id = ?"

        const val HENT_BREV_FOR_BEHANDLING_QUERY = """
            SELECT 
                b.id, b.sak_id, b.behandling_id, b.prosess_type, b.soeker_fnr, h.status_id, b.opprettet, 
                h.opprettet as hendelse_opprettet, i.tittel, i.spraak, b.brevtype, b.brevkoder
            FROM brev b
            INNER JOIN hendelse h on b.id = h.brev_id
            INNER JOIN innhold i on b.id = i.brev_id
            WHERE b.behandling_id = ?
            AND b.brevtype = ?
            AND h.status_id != 'SLETTET'
            AND h.id IN (
                SELECT DISTINCT ON (h2.brev_id) h2.id
                FROM hendelse h2
                WHERE h2.brev_id = b.id
                ORDER BY h2.brev_id, h2.opprettet DESC
            )
        """

        const val HENT_BREV_FOR_SAK_QUERY = """
            SELECT 
                b.id, b.sak_id, b.behandling_id, b.prosess_type, b.soeker_fnr, h.status_id, b.opprettet, 
                h.opprettet as hendelse_opprettet, i.tittel, i.spraak, b.brevtype, b.brevkoder
            FROM brev b
            INNER JOIN hendelse h on b.id = h.brev_id
            INNER JOIN innhold i on b.id = i.brev_id
            WHERE b.sak_id = ?
            AND h.status_id != 'SLETTET'
            AND h.id IN (
                SELECT DISTINCT ON (h2.brev_id) h2.id
                FROM hendelse h2
                WHERE h2.brev_id = b.id
                ORDER BY h2.brev_id, h2.opprettet DESC
            )
        """

        const val OPPRETT_BREV_QUERY = """
            INSERT INTO brev (sak_id, behandling_id, prosess_type, soeker_fnr, opprettet, brevtype, brevkoder) 
            VALUES (:sak_id, :behandling_id, :prosess_type, :soeker_fnr, :opprettet, :brevtype, :brevkoder) 
            ON CONFLICT DO NOTHING;
        """

        const val OPPRETT_MOTTAKER_QUERY = """
            INSERT INTO mottaker (
                id, brev_id, foedselsnummer, orgnummer, navn, 
                adressetype, adresselinje1, adresselinje2, adresselinje3, 
                postnummer, poststed, landkode, land, type
            ) VALUES (:id, :brev_id, :foedselsnummer, :orgnummer, :navn,
                :adressetype, :adresselinje1, :adresselinje2, :adresselinje3,
                :postnummer, :poststed, :landkode, :land, :type
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
                land = :land,
                tving_sentral_print = :tving_sentral_print,
                type = :type 
            WHERE id = :id AND brev_id = :brev_id
        """

        const val OPPDATER_BREVKODER_QUERY = """
            UPDATE brev 
            SET brevkoder = :brevkoder
            WHERE id = :id
        """

        const val OPPDATER_TITTEL_QUERY = """
            UPDATE innhold 
            SET tittel = :tittel
            WHERE brev_id = :brev_id
        """

        const val OPPDATER_SPRAAK_QUERY = """
            UPDATE innhold 
            SET spraak = :spraak
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

        const val OPPRETT_ELLER_OPPDATER_PDF_QUERY = """
            INSERT INTO pdf (brev_id, bytes) 
            VALUES (:brev_id, :bytes)
            ON CONFLICT (brev_id) DO UPDATE SET bytes = :bytes
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
            INSERT INTO hendelse (brev_id, status_id, payload, opprettet) 
            VALUES (:brev_id, :status_id, :payload, :opprettet)
        """
    }
}
