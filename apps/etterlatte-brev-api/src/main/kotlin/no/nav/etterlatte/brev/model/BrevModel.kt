package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.util.*

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
    val adresseType: String,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String,
    val land: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val navn: String,
    val foedselsnummer: Folkeregisteridentifikator? = null,
    val orgnummer: String? = null,
    val adresse: Adresse
) {
    init {
        if (foedselsnummer == null && orgnummer == null) {
            throw IllegalArgumentException("Enten fødselsnummer, orgnummer eller adresse må være spesifisert")
        }
    }

    companion object {
        fun fra(fnr: Folkeregisteridentifikator, regoppslag: RegoppslagResponseDTO) = Mottaker(
            navn = regoppslag.navn,
            foedselsnummer = fnr,
            adresse = Adresse(
                adresseType = regoppslag.adresse.type.name,
                adresselinje1 = regoppslag.adresse.adresselinje1,
                adresselinje2 = regoppslag.adresse.adresselinje2,
                adresselinje3 = regoppslag.adresse.adresselinje3,
                postnummer = regoppslag.adresse.postnummer,
                poststed = regoppslag.adresse.poststed,
                landkode = regoppslag.adresse.landkode,
                land = regoppslag.adresse.land
            )
        )
    }
}

class Brev(
    val id: BrevID,
    val behandlingId: UUID,
    val soekerFnr: String,
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
                soekerFnr = ulagretBrev.soekerFnr,
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
    val soekerFnr: String,
    val tittel: String,
    val spraak: Spraak,
    val mottaker: Mottaker,
    val erVedtaksbrev: Boolean,
    val pdf: ByteArray
) {
    val status: Status = Status.OPPRETTET
}

enum class Spraak(@get:JsonValue val verdi: String) { NB("nb"), NN("nn"), EN("en") }