package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.util.UUID

typealias BrevID = Long

enum class Status {
    OPPRETTET,
    OPPDATERT,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    SLETTET,
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
    val land: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val navn: String,
    val foedselsnummer: Foedselsnummer? = null,
    val orgnummer: String? = null,
    val adresse: Adresse,
) {
    init {
        require(foedselsnummer != null || orgnummer != null) {
            "Fødselsnummer eller orgnummer må være spesifisert"
        }
        require(navn.isNotBlank()) {
            "Navn på mottaker må være satt"
        }
    }

    companion object {
        fun fra(
            fnr: Folkeregisteridentifikator,
            regoppslag: RegoppslagResponseDTO,
        ) = Mottaker(
            navn = regoppslag.navn,
            foedselsnummer = Foedselsnummer(fnr.value),
            adresse =
                Adresse(
                    adresseType = regoppslag.adresse.type.name,
                    adresselinje1 = regoppslag.adresse.adresselinje1,
                    adresselinje2 = regoppslag.adresse.adresselinje2,
                    adresselinje3 = regoppslag.adresse.adresselinje3,
                    postnummer = regoppslag.adresse.postnummer,
                    poststed = regoppslag.adresse.poststed,
                    landkode = regoppslag.adresse.landkode,
                    land = regoppslag.adresse.land,
                ),
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
    val mottaker: Mottaker,
) {
    fun kanEndres() = status in listOf(Status.OPPRETTET, Status.OPPDATERT)

    companion object {
        fun fra(
            id: BrevID,
            opprettNyttBrev: OpprettNyttBrev,
        ) = Brev(
            id = id,
            sakId = opprettNyttBrev.sakId,
            behandlingId = opprettNyttBrev.behandlingId,
            prosessType = opprettNyttBrev.prosessType,
            soekerFnr = opprettNyttBrev.soekerFnr,
            status = opprettNyttBrev.status,
            mottaker = opprettNyttBrev.mottaker,
        )
    }
}

class Pdf(val bytes: ByteArray)

data class BrevInnhold(
    val tittel: String,
    val spraak: Spraak,
    val payload: Slate? = null,
)

data class BrevInnholdVedlegg(
    val tittel: String,
    val key: BrevVedleggKey,
    val payload: Slate? = null,
) {
    companion object {
        fun inntektsendringOMS(): List<BrevInnholdVedlegg> =
            listOf(
                utfallBeregningOMS(),
            )

        fun innvilgelseOMS(): List<BrevInnholdVedlegg> =
            listOf(
                utfallBeregningOMS(),
            )

        private fun utfallBeregningOMS() =
            BrevInnholdVedlegg(
                tittel = "Utfall ved beregning av omstillingsstønad",
                key = BrevVedleggKey.BEREGNING_INNHOLD,
                payload = getJsonFile("/maler/vedlegg/oms_utfall_beregning.json").let { deserialize<Slate>(it) },
            )

        private fun getJsonFile(url: String) = this::class.java.getResource(url)!!.readText()
    }
}

enum class BrevVedleggKey {
    BEREGNING_INNHOLD,
}

data class OpprettNyttBrev(
    val sakId: Long,
    val behandlingId: UUID?,
    val soekerFnr: String,
    val prosessType: BrevProsessType,
    val mottaker: Mottaker,
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
) {
    val status: Status = Status.OPPRETTET
}

enum class BrevProsessType {
    MANUELL,
    REDIGERBAR,
    AUTOMATISK,
}

data class EtterbetalingDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
)

data class Beregningsinfo(
    val innhold: List<Slate.Element>,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<NyBeregningsperiode>,
    val trygdetidsperioder: List<Trygdetidsperiode>,
)

data class NyBeregningsperiode(
    val inntekt: Kroner,
    val trygdetid: Int,
    val stoenadFoerReduksjon: Kroner,
    var utbetaltBeloep: Kroner,
)
