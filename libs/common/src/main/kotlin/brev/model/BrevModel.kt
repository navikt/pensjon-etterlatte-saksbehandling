package no.nav.etterlatte.libs.common.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import java.util.UUID

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
    val fornavn: String? = null,
    val etternavn: String? = null,
    val adresse: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val foedselsnummer: Foedselsnummer? = null,
    val orgnummer: String? = null,
    val adresse: Adresse? = null
) {
    init {
        if (foedselsnummer == null && orgnummer == null && adresse == null) {
            throw IllegalArgumentException("Enten fødselsnummer, orgnummer eller adresse må være spesifisert")
        }
    }
}

class Brev(
    val id: BrevID,
    val behandlingId: UUID,
    val tittel: String,
    val status: Status,
    val mottaker: Mottaker,
    val erVedtaksbrev: Boolean
) {
    companion object {
        fun fraUlagretBrev(id: BrevID, ulagretBrev: UlagretBrev) =
            Brev(
                id = id,
                behandlingId = ulagretBrev.behandlingId,
                tittel = ulagretBrev.tittel,
                status = ulagretBrev.status,
                mottaker = ulagretBrev.mottaker,
                erVedtaksbrev = ulagretBrev.erVedtaksbrev
            )
    }
}

class BrevInnhold(
    val mal: String,
    val spraak: Spraak,
    val data: ByteArray
)

class UlagretBrev(
    val behandlingId: UUID,
    val tittel: String,
    val spraak: Spraak,
    val mottaker: Mottaker,
    val erVedtaksbrev: Boolean,
    val pdf: ByteArray
) {
    val status: Status = Status.OPPRETTET
}