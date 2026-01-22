package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

fun mottakerFraAdresse(
    fnr: Folkeregisteridentifikator,
    regoppslag: RegoppslagResponseDTO,
    type: MottakerType,
) = Mottaker(
    id = UUID.randomUUID(),
    navn = regoppslag.navn,
    foedselsnummer = MottakerFoedselsnummer(fnr.value),
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
    tvingSentralPrint = false,
    type = type,
)

fun tomMottaker(
    fnr: Folkeregisteridentifikator? = null,
    type: MottakerType,
) = Mottaker(
    id = UUID.randomUUID(),
    navn = "N/A",
    foedselsnummer = fnr?.let { MottakerFoedselsnummer(it.value) },
    adresse =
        Adresse(
            adresseType = "",
            landkode = "",
            land = "",
        ),
    type = type,
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
    mottakere = opprettNyttBrev.mottakere,
    opprettet = opprettNyttBrev.opprettet,
    brevtype = opprettNyttBrev.brevtype,
    brevkoder = opprettNyttBrev.brevkoder,
)

data class BrevInnhold(
    val tittel: String,
    val spraak: Spraak,
    val payload: Slate? = null,
)

data class OpprettNyttBrev(
    val sakId: SakId,
    val behandlingId: UUID?,
    val soekerFnr: String,
    val prosessType: BrevProsessType,
    val mottakere: List<Mottaker>,
    val opprettet: Tidspunkt,
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
    val brevtype: Brevtype,
    val brevkoder: Brevkoder,
) {
    val status: Status = Status.OPPRETTET
}
