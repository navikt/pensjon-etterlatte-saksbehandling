package no.nav.etterlatte.libs.common.brev.model

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
    val fornavn: String,
    val etternavn: String,
    val adresse: String,
    val postnummer: String,
    val poststed: String,
    val land: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val foedselsnummer: Foedselsnummer? = null,
    val orgnummer: String? = null,
    val adresse: Adresse? = null,
) {
    init {
        if (foedselsnummer == null && orgnummer == null && adresse == null) {
            throw IllegalArgumentException("Enten fødselsnummer, orgnummer eller adresse må være spesifisert")
        }
    }
}

class Brev(
    val id: BrevID,
    val behandlingId: String,
    val tittel: String,
    val status: Status,
    val mottaker: Mottaker,
    val erVedtaksbrev: Boolean,
) {
    companion object {
        fun fraNyttBrev(id: BrevID, nyttBrev: NyttBrev) =
            Brev(
                id = id,
                behandlingId = nyttBrev.behandlingId,
                tittel = nyttBrev.tittel,
                status = nyttBrev.status,
                mottaker = nyttBrev.mottaker,
                erVedtaksbrev = nyttBrev.erVedtaksbrev
            )
    }
}

class BrevInnhold(
    val mal: String,
    val spraak: String,
    val data: ByteArray
)

class NyttBrev(
    val behandlingId: String,
    val tittel: String,
    val mottaker: Mottaker,
    val erVedtaksbrev: Boolean,
    val pdf: ByteArray
) {
    val status: Status = Status.OPPRETTET
}
