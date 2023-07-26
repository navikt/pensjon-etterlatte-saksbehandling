package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
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
    val foedselsnummer: Foedselsnummer? = null,
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
            foedselsnummer = Foedselsnummer(fnr.value),
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

data class Brev(
    val id: BrevID,
    val sakId: Long,
    val behandlingId: UUID?,
    val prosessType: BrevProsessType,
    val soekerFnr: String,
    val status: Status,
    val mottaker: Mottaker
) {

    fun kanEndres() = status in listOf(Status.OPPRETTET, Status.OPPDATERT)

    companion object {
        fun fra(id: BrevID, opprettNyttBrev: OpprettNyttBrev) =
            Brev(
                id = id,
                sakId = opprettNyttBrev.sakId,
                behandlingId = opprettNyttBrev.behandlingId,
                prosessType = opprettNyttBrev.prosessType,
                soekerFnr = opprettNyttBrev.soekerFnr,
                status = opprettNyttBrev.status,
                mottaker = opprettNyttBrev.mottaker
            )
    }
}

class Pdf(val bytes: ByteArray)

data class BrevInnhold(
    val tittel: String,
    val spraak: Spraak,
    val payload: Slate? = null
)

data class OpprettNyttBrev(
    val sakId: Long,
    val behandlingId: UUID?,
    val soekerFnr: String,
    val prosessType: BrevProsessType,
    val mottaker: Mottaker,
    val innhold: BrevInnhold
) {
    val status: Status = Status.OPPRETTET
}

enum class BrevProsessType {
    MANUELL,
    AUTOMATISK;

    companion object {
        fun fra(behandling: Behandling): BrevProsessType {
            return when (behandling.sakType) {
                SakType.OMSTILLINGSSTOENAD -> omsBrev(behandling)
                SakType.BARNEPENSJON -> bpBrev(behandling)
            }
        }

        private fun omsBrev(behandling: Behandling): BrevProsessType {
            return when (behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> AUTOMATISK
                VedtakType.OPPHOER,
                VedtakType.AVSLAG,
                VedtakType.ENDRING -> MANUELL
            }
        }

        private fun bpBrev(behandling: Behandling): BrevProsessType {
            return when (behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> AUTOMATISK
                VedtakType.ENDRING -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> AUTOMATISK
                    else -> MANUELL
                }

                VedtakType.OPPHOER -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.ADOPSJON -> AUTOMATISK
                    RevurderingAarsak.OMGJOERING_AV_FARSKAP -> AUTOMATISK
                    else -> MANUELL
                }

                VedtakType.AVSLAG -> MANUELL
            }
        }
    }
}

enum class Spraak(@get:JsonValue val verdi: String) {
    NB("nb"), NN("nn"), EN("en");

    fun locale(): Locale =
        when (this) {
            NB -> Locale.forLanguageTag("no")
            NN -> Locale.forLanguageTag("no")
            EN -> Locale.UK
        }
}