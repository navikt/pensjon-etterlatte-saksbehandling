package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class Brevoppretter(
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val innholdTilRedigerbartBrevHenter: InnholdTilRedigerbartBrevHenter,
) {
    suspend fun opprettBrevSomHarInnhold(
        sakId: SakId,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: Brevkoder,
        brevData: BrevDataRedigerbar,
        spraak: Spraak? = null,
    ): Pair<Brev, Enhetsnummer> {
        with(
            innholdTilRedigerbartBrevHenter.hentInnDataForBrevMedData(
                sakId,
                behandlingId,
                bruker,
                brevKode,
                brevData,
                spraak,
            ),
        ) {
            val nyttBrev =
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    prosessType = BrevProsessType.REDIGERBAR,
                    soekerFnr = soekerFnr,
                    mottakere = adresseService.hentMottakere(sakType, personerISak, bruker),
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
                    mottakere = adresseService.hentMottakere(sakType, personerISak, bruker),
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
}
