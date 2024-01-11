package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.SakService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevProsessTypeFactory
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.SlateHelper
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

class Brevoppretter(
    private val sakService: SakService,
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val brevdataFacade: BrevdataFacade,
    private val brevProsessTypeFactory: BrevProsessTypeFactory,
    private val brevbaker: BrevbakerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
        // TODO EY-3232 - Fjerne migreringstilpasning
    ): Brev {
        require(hentVedtaksbrev(behandlingId) == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        if (brukerTokenInfo is Saksbehandler) {
            brevdataFacade.hentBehandling(behandlingId, brukerTokenInfo).status.let { status ->
                require(status.kanEndres()) {
                    "Behandling (id=$behandlingId) har status $status og kan ikke opprette vedtaksbrev"
                }
            }
        }

        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, brukerTokenInfo) }

        val mottaker =
            with(generellBrevData.personerISak) {
                when (verge) {
                    is Vergemaal ->
                        verge.toMottaker()

                    else -> {
                        val mottakerFnr =
                            innsender?.fnr?.value?.takeUnless { it == no.nav.etterlatte.libs.common.Vedtaksloesning.PESYS.name }
                                ?: soeker.fnr.value
                        adresseService.hentMottakerAdresse(mottakerFnr)
                    }
                }
            }

        val prosessType =
            brevProsessTypeFactory.fra(
                generellBrevData,
                erOmregningNyRegel = automatiskMigreringRequest?.erOmregningGjenny ?: false,
            )

        val nyttBrev =
            OpprettNyttBrev(
                sakId = sakId,
                behandlingId = behandlingId,
                prosessType = prosessType,
                soekerFnr = generellBrevData.personerISak.soeker.fnr.value,
                mottaker = mottaker,
                opprettet = Tidspunkt.now(),
                innhold =
                    opprettInnhold(
                        RedigerbarTekstRequest(
                            generellBrevData,
                            brukerTokenInfo,
                            prosessType,
                            automatiskMigreringRequest,
                        ),
                    ),
                innholdVedlegg = opprettInnholdVedlegg(generellBrevData, prosessType),
            )

        return db.opprettBrev(nyttBrev)
    }

    suspend fun opprettBrev(
        sakId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
    ): Brev {
        val sak = sakService.hentSak(sakId, bruker)

        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, bruker) }

        val mottaker = adresseService.hentMottakerAdresse(sak.ident)

        val prosessType =
            brevProsessTypeFactory.fra(
                generellBrevData,
                erOmregningNyRegel = automatiskMigreringRequest?.erOmregningGjenny ?: false,
            )

        val nyttBrev =
            OpprettNyttBrev(
                sakId = sakId,
                behandlingId = behandlingId,
                prosessType = prosessType,
                soekerFnr = generellBrevData.personerISak.soeker.fnr.value,
                mottaker = mottaker,
                opprettet = Tidspunkt.now(),
                innhold =
                    opprettInnhold(
                        RedigerbarTekstRequest(
                            generellBrevData,
                            bruker,
                            prosessType,
                            automatiskMigreringRequest,
                        ),
                    ),
                innholdVedlegg = opprettInnholdVedlegg(generellBrevData, prosessType),
            )

        return db.opprettBrev(nyttBrev)
    }

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId)
    }

    private suspend fun opprettInnhold(redigerbarTekstRequest: RedigerbarTekstRequest): BrevInnhold {
        val tittel = "Vedtak om ${redigerbarTekstRequest.vedtakstype()}"

        val payload =
            when (redigerbarTekstRequest.prosessType) {
                BrevProsessType.REDIGERBAR -> brevbaker.hentRedigerbarTekstFraBrevbakeren(redigerbarTekstRequest)
                BrevProsessType.AUTOMATISK -> null
                BrevProsessType.MANUELL -> SlateHelper.hentInitiellPayload(redigerbarTekstRequest.generellBrevData)
            }

        return BrevInnhold(tittel, redigerbarTekstRequest.generellBrevData.spraak, payload)
    }

    private fun opprettInnholdVedlegg(
        generellBrevData: GenerellBrevData,
        prosessType: BrevProsessType,
    ): List<BrevInnholdVedlegg>? =
        when (prosessType) {
            BrevProsessType.REDIGERBAR -> SlateHelper.hentInitiellPayloadVedlegg(generellBrevData)
            BrevProsessType.AUTOMATISK -> null
            BrevProsessType.MANUELL -> null
        }
}
