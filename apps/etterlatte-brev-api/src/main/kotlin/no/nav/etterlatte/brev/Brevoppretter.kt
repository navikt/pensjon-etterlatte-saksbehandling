package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import java.util.UUID

class Brevoppretter(
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val behandlingService: BehandlingService,
    private val innholdTilRedigerbartBrevHenter: InnholdTilRedigerbartBrevHenter,
) {
    suspend fun opprettVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevKodeMapping: (b: BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataRedigerbarRequest) -> BrevDataRedigerbar,
    ): Brev {
        require(db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull() == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        if (brukerTokenInfo is Saksbehandler) {
            val kanRedigeres = behandlingService.hentVedtaksbehandlingKanRedigeres(behandlingId, brukerTokenInfo)
            if (!kanRedigeres) {
                throw KanIkkeOppretteVedtaksbrev(behandlingId)
            }
        }

        return opprettBrev(
            sakId = sakId,
            behandlingId = behandlingId,
            bruker = brukerTokenInfo,
            brevKodeMapping = brevKodeMapping,
            brevtype = Brevtype.VEDTAK,
            brevDataMapping = brevDataMapping,
        ).first
    }

    suspend fun opprettBrev(
        sakId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKodeMapping: (b: BrevkodeRequest) -> Brevkoder,
        brevtype: Brevtype,
        brevDataMapping: suspend (BrevDataRedigerbarRequest) -> BrevDataRedigerbar,
    ): Pair<Brev, String> =
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
                    brevtype = brevtype,
                )
            return Pair(db.opprettBrev(nyttBrev), enhet)
        }

    suspend fun hentNyttInnhold(
        sakId: Long,
        brevId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKodeMapping: (b: BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataRedigerbarRequest) -> BrevDataRedigerbar,
    ): BrevService.BrevPayload {
        val spraak = db.hentBrevInnhold(brevId)?.spraak

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
                db.oppdaterPayload(brevId, innhold.payload)
            }

            if (innholdVedlegg != null) {
                db.oppdaterPayloadVedlegg(brevId, innholdVedlegg)
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
            val mottakerFnr: String? =
                when (verge) {
                    is Vergemaal -> verge.foedselsnummer.value
                    is UkjentVergemaal -> null

                    else ->
                        innsender?.fnr?.value?.takeIf { Folkeregisteridentifikator.isValid(it) }
                            ?: soeker.fnr.value
                }
            if (mottakerFnr != null) {
                adresseService.hentMottakerAdresse(sakType, mottakerFnr)
            } else {
                tomMottaker()
            }
        }

    private fun tomMottaker() =
        Mottaker(
            navn = "Ukjent",
            foedselsnummer = null,
            orgnummer = null,
            adresse = Adresse(adresseType = "", landkode = "", land = ""),
        )
}

class KanIkkeOppretteVedtaksbrev(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "KAN_IKKE_ENDRE_VEDTAKSBREV",
        detail = "Statusen til behandlingen tillater ikke at det opprettes vedtaksbrev",
        meta = mapOf("behandlingId" to behandlingId),
    )
