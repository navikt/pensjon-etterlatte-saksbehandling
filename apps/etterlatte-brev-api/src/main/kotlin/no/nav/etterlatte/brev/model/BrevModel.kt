package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.util.Locale
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

        private fun getJsonFile(url: String) = javaClass.getResource(url)!!.readText()
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

class BrevProsessTypeFactory(private val featureToggleService: FeatureToggleService) {
    fun fra(behandling: Behandling): BrevProsessType {
        return when (behandling.sakType) {
            SakType.OMSTILLINGSSTOENAD -> omsBrev(behandling)
            SakType.BARNEPENSJON -> bpBrev(behandling)
        }
    }

    private fun omsBrev(behandling: Behandling): BrevProsessType {
        return when (behandling.vedtak.type) {
            VedtakType.INNVILGELSE -> BrevProsessType.REDIGERBAR
            VedtakType.OPPHOER ->
                when (behandling.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }
            VedtakType.AVSLAG,
            VedtakType.ENDRING,
            ->
                when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.INNTEKTSENDRING,
                    RevurderingAarsak.ANNEN,
                    -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }
        }
    }

    private fun bpBrev(behandling: Behandling): BrevProsessType {
        return when (behandling.vedtak.type) {
            VedtakType.INNVILGELSE ->
                when (
                    featureToggleService.isEnabled(
                        BrevDataFeatureToggle.NyMalInnvilgelse,
                        false,
                    )
                ) {
                    true -> BrevProsessType.REDIGERBAR
                    false -> BrevProsessType.AUTOMATISK
                }

            VedtakType.ENDRING ->
                when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.FENGSELSOPPHOLD -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.UT_AV_FENGSEL -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.YRKESSKADE -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.OPPHOER ->
                when (behandling.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.AVSLAG -> BrevProsessType.MANUELL
        }
    }
}

enum class Spraak(
    @get:JsonValue val verdi: String,
) {
    NB("nb"),
    NN("nn"),
    EN("en"),
    ;

    fun locale(): Locale =
        when (this) {
            NB -> Locale.forLanguageTag("no")
            NN -> Locale.forLanguageTag("no")
            EN -> Locale.UK
        }
}
