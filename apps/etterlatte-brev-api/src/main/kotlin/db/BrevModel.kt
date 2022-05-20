package no.nav.etterlatte.db

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.person.Foedselsnummer

typealias BrevID = Long

enum class Status {
    OPPRETTET,
    OPPDATERT,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    SLETTET
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Adresse(
    val adresse: String,
    val postnummer: String,
    val poststed: String,
    val land: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer? = null,
    val adresse: Adresse
)

class Brev(
    val id: BrevID,
    val behandlingId: Long,
    val tittel: String,
    val status: Status,
    val mottaker: Mottaker,
    // TODO: Burde dette fjernes fra brevet og heller være helt adskilt?
    @JsonIgnore
    val data: ByteArray? = null
) {
    companion object {
        fun fraNyttBrev(id: BrevID, nyttBrev: NyttBrev) =
            Brev(id, nyttBrev.behandlingId, nyttBrev.tittel, nyttBrev.status, nyttBrev.mottaker)
    }
}

class BrevInnhold(
    val mal: String,
    val spraak: String,
    val data: ByteArray
)

class NyttBrev(
    val behandlingId: Long,
    val tittel: String,
    val mottaker: Mottaker,
    val pdf: ByteArray
) {
    val status: Status = Status.OPPRETTET
}
