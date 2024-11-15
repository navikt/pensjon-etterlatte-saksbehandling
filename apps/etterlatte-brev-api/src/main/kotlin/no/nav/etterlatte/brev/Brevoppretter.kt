package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.VERGENAVN_FOR_MOTTAKER
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class Brevoppretter(
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val innholdTilRedigerbartBrevHenter: InnholdTilRedigerbartBrevHenter,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettBrevSomHarInnhold(
        sakId: SakId,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: Brevkoder,
        brevData: BrevDataRedigerbar,
    ): Pair<Brev, Enhetsnummer> {
        with(
            innholdTilRedigerbartBrevHenter.hentInnDataForBrevMedData(
                sakId,
                behandlingId,
                bruker,
                brevKode,
                brevData,
            ),
        ) {
            val nyttBrev =
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    prosessType = BrevProsessType.REDIGERBAR,
                    soekerFnr = soekerFnr,
                    mottaker = finnMottaker(sakType, personerISak),
                    opprettet = Tidspunkt.now(),
                    innhold = innhold,
                    innholdVedlegg = innholdVedlegg,
                    brevtype = brevKode.brevtype,
                    brevkoder = brevkode,
                )

            return Pair(db.opprettBrev(nyttBrev, bruker), enhet)
        }
    }

    suspend fun opprettBrev(
        sakId: SakId,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKodeMapping: (b: BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataRedigerbarRequest) -> BrevDataRedigerbar,
    ): Pair<Brev, Enhetsnummer> =
        with(
            innholdTilRedigerbartBrevHenter.hentInnData(
                sakId,
                behandlingId,
                bruker,
                brevKodeMapping,
                brevDataMapping,
            ),
        ) {
            val nyttBrev =
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    prosessType = BrevProsessType.REDIGERBAR,
                    soekerFnr = soekerFnr,
                    mottaker = finnMottaker(sakType, personerISak),
                    opprettet = Tidspunkt.now(),
                    innhold = innhold,
                    innholdVedlegg = innholdVedlegg,
                    brevtype = brevkode.brevtype,
                    brevkoder = brevkode,
                )
            return Pair(db.opprettBrev(nyttBrev, bruker), enhet)
        }

    suspend fun hentNyttInnhold(
        sakId: SakId,
        brevId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKodeMapping: (b: BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataRedigerbarRequest) -> BrevDataRedigerbar,
    ): BrevService.BrevPayload {
        val spraak = db.hentBrevInnhold(brevId)?.spraak
        val opprinneligBrevkoder = db.hentBrevkoder(brevId)

        with(
            innholdTilRedigerbartBrevHenter.hentInnData(
                sakId,
                behandlingId,
                bruker,
                brevKodeMapping,
                brevDataMapping,
                spraak,
            ),
        ) {
            if (innhold.payload != null) {
                db.oppdaterPayload(brevId, innhold.payload, bruker)
            }

            if (innholdVedlegg != null) {
                db.oppdaterPayloadVedlegg(brevId, innholdVedlegg, bruker)
            }

            if (opprinneligBrevkoder != brevkode) {
                db.oppdaterBrevkoder(brevId, brevkode)
                db.oppdaterTittel(brevId, innhold.tittel, bruker)
            }

            return BrevService.BrevPayload(
                innhold.payload ?: db.hentBrevPayload(brevId),
                innholdVedlegg ?: db.hentBrevPayloadVedlegg(brevId),
            )
        }
    }

    private suspend fun finnMottaker(
        sakType: SakType,
        personerISak: PersonerISak,
    ): Mottaker =
        with(personerISak) {
            when (verge) {
                is Vergemaal -> {
                    logger.warn("Er verge, kan ikke ferdigstille uten å legge til adresse manuelt.")
                    tomVergeMottaker().copy(foedselsnummer = MottakerFoedselsnummer(verge.foedselsnummer.value))
                }
                is UkjentVergemaal -> {
                    logger.warn("Er verge, kan ikke ferdigstille uten å legge til adresse manuelt.")
                    tomVergeMottaker()
                }

                else ->
                    adresseService.hentMottakerAdresse(
                        sakType,
                        innsender?.fnr?.value?.takeIf { Folkeregisteridentifikator.isValid(it) }
                            ?: soeker.fnr.value,
                    )
            }
        }

    private fun tomVergeMottaker(): Mottaker =
        Mottaker(
            UUID.randomUUID(),
            navn = VERGENAVN_FOR_MOTTAKER,
            foedselsnummer = null,
            orgnummer = null,
            adresse = Adresse(adresseType = "", landkode = "", land = ""),
        )
}
