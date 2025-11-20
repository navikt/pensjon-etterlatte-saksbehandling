package no.nav.etterlatte.behandling.etteroppgjoer.brev

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevRequestData
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.DetaljertForbehandlingDto
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.SummertePensjonsgivendeInntekter
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentVergeForSak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevGrunnlag
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class EtteroppgjoerForbehandlingBrevService(
    private val brevKlient: BrevKlient,
    private val grunnlagService: GrunnlagService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val behandlingService: BehandlingService,
) {
    private val logger: Logger = LoggerFactory.getLogger(EtteroppgjoerForbehandlingBrevService::class.java)

    suspend fun opprettVarselBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val brevRequest = utledBrevRequest(forbehandlingId, brukerTokenInfo)

        return brevKlient
            .opprettStrukturertBrev(
                forbehandlingId,
                brevRequest,
                brukerTokenInfo,
            ).also {
                etteroppgjoerForbehandlingService.lagreBrevreferanse(forbehandlingId, it)
            }
    }

    suspend fun tilbakestillVarselBrev(
        brevId: BrevID,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload {
        etteroppgjoerForbehandlingService.sjekkAtOppgavenErTildeltSaksbehandler(forbehandlingId, brukerTokenInfo)

        val brevRequest = utledBrevRequest(forbehandlingId, brukerTokenInfo)

        return brevKlient.tilbakestillStrukturertBrev(
            brevID = brevId,
            behandlingId = forbehandlingId,
            brevRequest = brevRequest,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    suspend fun ferdigstillForbehandlingMedBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val detaljertForbehandling =
            etteroppgjoerForbehandlingService
                .hentDetaljertForbehandling(forbehandlingId, brukerTokenInfo)

        val brevId =
            detaljertForbehandling.forbehandling.brevId ?: throw UgyldigForespoerselException(
                code = "MANGLER_BREVID",
                detail = "Forbehandling $forbehandlingId mangler brevId og kan ikke ferdigstilles.",
            )

        val sakId = detaljertForbehandling.forbehandling.sak.id
        krevBrevKanFerdigstilles(detaljertForbehandling, sakId, brevId, brukerTokenInfo)

        val forbehandlingMedVarselbrevSendt = detaljertForbehandling.forbehandling.medVarselbrevSendt(LocalDate.now())
        etteroppgjoerForbehandlingService.ferdigstillForbehandling(forbehandlingMedVarselbrevSendt, brukerTokenInfo)

        brevKlient.ferdigstillJournalfoerStrukturertBrev(
            forbehandlingId,
            Brevkoder.OMS_EO_FORHAANDSVARSEL.brevtype,
            brukerTokenInfo,
        )
    }

    suspend fun genererPdf(
        brevID: BrevID,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        val brevRequest = utledBrevRequest(forbehandlingId, brukerTokenInfo)
        return brevKlient.genererPdf(brevID, forbehandlingId, brevRequest, brukerTokenInfo)
    }

    suspend fun hentVarselBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev? {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(forbehandlingId)
        return forbehandling.brevId?.let {
            brevKlient.hentBrev(
                sakId = forbehandling.sak.id,
                brevId = it,
                brukerTokenInfo = brukerTokenInfo,
            )
        }
    }

    private suspend fun utledBrevRequest(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevRequest =
        coroutineScope {
            val detaljertForbehandling =
                etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                    forbehandlingId,
                    brukerTokenInfo,
                )

            krevIkkeNull(detaljertForbehandling.beregnetEtteroppgjoerResultat) {
                "Forbehandlingen må ha et utregnet resultat for å sende et varselbrev"
            }

            if (detaljertForbehandling.beregnetEtteroppgjoerResultat.resultatType ==
                EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING
            ) {
                throw UgyldigForespoerselException(
                    "ETTEROPPGJOER_SKAL_IKKE_HA_BREV",
                    "Varselbrev skal ikke sendes ut for etteroppgjør " +
                        "forbehandlinger som har resultat ingen endring med ingen utbetaling.",
                )
            }

            val pensjonsgivendeInntekt = detaljertForbehandling.opplysninger.skatt

            val sisteIverksatteBehandling =
                behandlingService.hentBehandling(detaljertForbehandling.forbehandling.sisteIverksatteBehandlingId)
                    ?: throw InternfeilException("Fant ikke siste iverksatte behandling, kan ikke utlede brevinnhold")

            val (redigerbar, innhold, vedlegg, sak) =
                brevRequestDataMapper(
                    detaljertForbehandling,
                    sisteIverksatteBehandling,
                    pensjonsgivendeInntekt,
                )

            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)
                    ?: throw InternfeilException("Fant ikke grunnlag med sakId=${sak.id}")

            return@coroutineScope BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = hentVergeForSak(sak.sakType, null, grunnlag),
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = brukerTokenInfo.ident(),
                attestantIdent = null,
                skalLagre = true, // TODO: vurder riktig logikk for lagring
                brevFastInnholdData = innhold,
                brevRedigerbarInnholdData = redigerbar,
                brevVedleggData = vedlegg,
            )
        }

    private fun brevRequestDataMapper(
        data: DetaljertForbehandlingDto,
        sisteIverksatteBehandling: Behandling,
        pensjonsgivendeInntekt: SummertePensjonsgivendeInntekter?,
    ): EtteroppgjoerBrevRequestData {
        krevIkkeNull(data.beregnetEtteroppgjoerResultat) {
            "Beregnet etteroppgjoer resultat er null og kan ikke vises i brev"
        }
        if (data.beregnetEtteroppgjoerResultat.resultatType == EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING) {
            throw UgyldigForespoerselException(
                "SKAL_IKKE_HA_BREV",
                "Resultatet i etteroppgjøret er ingen endring og ingen utbetaling, så bruker skal ikke ha et varselbrev.",
            )
        }

        val bosattUtland = sisteIverksatteBehandling.erBosattUtland()
        val grunnlag =
            data.faktiskInntekt
                ?: throw InternfeilException("Etteroppgjør mangler faktisk inntekt og kan ikke vises i brev")

        // TODO: usikker om dette blir rett, følge opp ifm testing
        val norskInntekt = pensjonsgivendeInntekt != null && pensjonsgivendeInntekt.summertInntekt > 0

        return EtteroppgjoerBrevRequestData(
            redigerbar =
                EtteroppgjoerBrevData.ForhaandsvarselInnhold(
                    bosattUtland = bosattUtland,
                    norskInntekt = norskInntekt,
                    etteroppgjoersAar = data.forbehandling.aar,
                    rettsgebyrBeloep = Kroner(data.beregnetEtteroppgjoerResultat.grense.rettsgebyr),
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()),
                    sak = sisteIverksatteBehandling.sak,
                ),
            innhold =
                EtteroppgjoerBrevData.Forhaandsvarsel(
                    bosattUtland = bosattUtland,
                    norskInntekt = norskInntekt,
                    etteroppgjoersAar = data.forbehandling.aar,
                    rettsgebyrBeloep = Kroner(data.beregnetEtteroppgjoerResultat.grense.rettsgebyr),
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    stoenad = Kroner(data.beregnetEtteroppgjoerResultat.utbetaltStoenad.toInt()),
                    faktiskStoenad = Kroner(data.beregnetEtteroppgjoerResultat.nyBruttoStoenad.toInt()),
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()),
                    grunnlag = EtteroppgjoerBrevGrunnlag.fra(grunnlag, data.opplysninger.skatt.summertInntekt),
                ),
            vedlegg =
                listOf(
                    EtteroppgjoerBrevData.beregningsVedlegg(etteroppgjoersAar = data.forbehandling.aar, erVedtak = false),
                ),
            sak = sisteIverksatteBehandling.sak,
        )
    }

    private suspend fun krevBrevKanFerdigstilles(
        detaljertForbehandling: DetaljertForbehandlingDto,
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val brev = brevKlient.hentBrev(sakId, brevId, brukerTokenInfo)

        if (brev.status.ikkeFerdigstilt()) {
            val sistBeregnetTidspunkt = detaljertForbehandling.beregnetEtteroppgjoerResultat!!.tidspunkt
            if (sistBeregnetTidspunkt > brev.statusEndret) {
                throw IkkeTillattException(
                    code = "KAN_IKKE_FERDIGSTILLE_BREV",
                    detail =
                        "Behandling er redigert etter brevet ble opprettet. Gå gjennom brevet og vurder " +
                            "om det bør tilbakestilles for å få oppdaterte verdier fra behandlingen.",
                )
            }
        }

        val response = brevKlient.kanFerdigstilleBrev(brevId, sakId, brukerTokenInfo)
        if (!response.kanFerdigstille) {
            throw UgyldigForespoerselException(
                code = "KAN_IKKE_FERDIGSTILLE_BREV",
                detail = response.aarsak ?: "Ukjent feil",
            )
        }
    }

    suspend fun slettVarselbrev(
        brevSomskalSlettes: BrevID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        try {
            brevKlient.slettBrev(brevSomskalSlettes, sakId, brukerTokenInfo)
        } catch (ex: Exception) {
            logger.error(
                "Kunne ikke slette varselbrev etteroppgjør med id=$brevSomskalSlettes i sak $sakId. " +
                    "Forbehandlingen vet ikke lengre om brevet, og brevet bør settes til utgått manuelt.",
                ex,
            )
        }
    }
}
