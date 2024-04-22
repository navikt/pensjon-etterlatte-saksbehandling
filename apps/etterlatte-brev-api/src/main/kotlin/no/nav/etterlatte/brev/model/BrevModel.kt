package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

fun mottakerFraAdresse(
    fnr: Folkeregisteridentifikator,
    regoppslag: RegoppslagResponseDTO,
) = Mottaker(
    navn = regoppslag.navn,
    foedselsnummer = fnr,
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

fun tomMottaker(fnr: Folkeregisteridentifikator) =
    Mottaker(
        navn = "N/A",
        foedselsnummer = fnr,
        adresse =
            Adresse(
                adresseType = "",
                landkode = "",
                land = "",
            ),
    )

fun opprettBrevFra(
    id: BrevID,
    opprettNyttBrev: OpprettNyttBrev,
) = Brev(
    id = id,
    sakId = opprettNyttBrev.sakId,
    behandlingId = opprettNyttBrev.behandlingId,
    tittel = opprettNyttBrev.innhold.tittel,
    spraak = opprettNyttBrev.innhold.spraak,
    prosessType = opprettNyttBrev.prosessType,
    soekerFnr = opprettNyttBrev.soekerFnr,
    status = opprettNyttBrev.status,
    statusEndret = opprettNyttBrev.opprettet,
    mottaker = opprettNyttBrev.mottaker,
    opprettet = opprettNyttBrev.opprettet,
    brevtype = opprettNyttBrev.brevtype,
)

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
)

enum class BrevVedleggKey {
    OMS_BEREGNING,
    OMS_FORHAANDSVARSEL_FEILUTBETALING,
    BP_BEREGNING_TRYGDETID,
    BP_FORHAANDSVARSEL_FEILUTBETALING,
}

data class OpprettNyttBrev(
    val sakId: Long,
    val behandlingId: UUID?,
    val soekerFnr: String,
    val prosessType: BrevProsessType,
    val mottaker: Mottaker,
    val opprettet: Tidspunkt,
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
    val brevtype: Brevtype,
) {
    val status: Status = Status.OPPRETTET
}

data class EtterbetalingDTO(
    val datoFom: LocalDate,
    val datoTom: LocalDate,
)
