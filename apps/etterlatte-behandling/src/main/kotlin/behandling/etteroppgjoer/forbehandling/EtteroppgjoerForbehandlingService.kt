package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerTempService
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.PensjonsgivendeInntektService
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerForbehandlingService(
    private val dao: EtteroppgjoerForbehandlingDao,
    private val sakDao: SakLesDao,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val oppgaveService: OppgaveService,
    private val inntektskomponentService: InntektskomponentService,
    private val pensjonsgivendeInntektService: PensjonsgivendeInntektService,
    private val hendelserService: EtteroppgjoerHendelseService,
    private val beregningKlient: BeregningKlient,
    private val behandlingService: BehandlingService,
    private val vedtakKlient: VedtakKlient,
    private val etteroppgjoerTempService: EtteroppgjoerTempService,
) {
    private val logger: Logger = LoggerFactory.getLogger(EtteroppgjoerForbehandlingService::class.java)

    /**
     * Ferdigstiller en forbehandling som hører til en revurdering. Vi trenger ikke å avslutte noen oppgave
     * da denne ikke skal ha oppgave knyttet til seg og vi oppdaterer heller ikke etteroppgjøret.
     */
    fun ferdigstillRevurderingForbehandling(
        forbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        logger.info("Ferdigstiller forbehandling (tilknyttet revurdering) med id=${forbehandling.id}")

        val forbehandling = dao.hentForbehandling(forbehandling.id) ?: throw FantIkkeForbehandling(forbehandling.id)

        sjekkAtInntekterErOppdatert(forbehandling)

        return forbehandling.tilFerdigstilt().also {
            dao.lagreForbehandling(it)
            registrerOgSendHendelseFerdigstilt(it, brukerTokenInfo)
        }
    }

    /**
     * Ferdigstiller forbehandling, tilknyttet oppgave og endrer status på etteroppjøret.
     * Kontekst er en forbehandling som ikke hører til en revurdering.
     */
    fun ferdigstillForbehandling(
        forbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        logger.info("Ferdigstiller forbehandling med id=${forbehandling.id}")

        val forbehandling = dao.hentForbehandling(forbehandling.id) ?: throw FantIkkeForbehandling(forbehandling.id)

        sjekkAtSisteIverksatteBehandlingErRiktig(forbehandling)
        sjekkAtInntekterErOppdatert(forbehandling)
        sjekkAtOppgavenErTildeltSaksbehandler(forbehandling.id, brukerTokenInfo)

        ferdigstillEtteroppgjoerOppgave(forbehandling, brukerTokenInfo)

        return forbehandling.tilFerdigstilt().also {
            dao.lagreForbehandling(it)
            registrerOgSendHendelseFerdigstilt(it, brukerTokenInfo)
            etteroppgjoerService.oppdaterEtteroppgjoerVedFerdigstiltForbehandling(it)
        }
    }

    fun avbrytForbehandling(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        aarsak: AarsakTilAvbryteForbehandling,
        kommentar: String?,
    ) {
        logger.info("Avbryter forbehandling med id=$forbehandlingId")
        val forbehandling = hentForbehandling(forbehandlingId)
        sjekkAtOppgavenErTildeltSaksbehandler(forbehandling.id, brukerTokenInfo)

        if (!forbehandling.erRedigerbar()) {
            throw IkkeTillattException(
                "FEIL_STATUS_FORBEHANDLING",
                "Forbehandling med id=$forbehandlingId kan ikke avbrytes. Status er ${forbehandling.status}",
            )
        } else if (forbehandling.erRevurdering()) {
            throw IkkeTillattException(
                "FORBEHANDLING_ER_TILKNYTT_REVURDERING",
                "Forbehandling med id=$forbehandlingId er tilknytt revurdering og kan ikke avbrytes gjennom dette endepunktet.",
            )
        }

        if (aarsak == AarsakTilAvbryteForbehandling.ANNET && kommentar.isNullOrBlank()) {
            throw UgyldigForespoerselException(
                "VERDI_ER_NULL",
                "Kan ikke avbryte behandling uten å begrunne hvorfor. Kommentar er null eller blankt",
            )
        }

        forbehandling.tilAvbrutt(aarsak, kommentar.orEmpty()).let { avbruttForbehandling ->
            dao.lagreForbehandling(avbruttForbehandling)

            etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                avbruttForbehandling.sak.id,
                avbruttForbehandling.aar,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
            )

            oppgaveService.avbrytAapneOppgaverMedReferanse(
                forbehandlingId.toString(),
                "Avbrutt manuelt. Årsak: ${kommentar ?: aarsak.name}",
            )
            hendelserService.registrerOgSendEtteroppgjoerHendelse(
                etteroppgjoerForbehandling = avbruttForbehandling,
                hendelseType = EtteroppgjoerHendelseType.AVBRUTT,
                saksbehandler = (brukerTokenInfo as? Saksbehandler)?.ident,
                utlandstilknytning = hentUtlandstilknytning(forbehandling),
            )
        }
    }

    fun lagreForbehandling(forbehandling: EtteroppgjoerForbehandling) = dao.lagreForbehandling(forbehandling)

    fun lagreVarselbrevSendt(
        forbehandlingId: UUID,
        dato: LocalDate,
    ) {
        val forbehandling = hentForbehandling(forbehandlingId)
        lagreForbehandling(forbehandling.medVarselbrevSendt(dato))
    }

    fun hentForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling =
        dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

    // TODO: denne kan løses i sql
    fun hentSisteFerdigstillteForbehandling(sakId: SakId): EtteroppgjoerForbehandling {
        val sisteFerdigstilteForbehandling =
            dao
                .hentForbehandlingerForSak(sakId)
                .filter { it.erFerdigstilt() }
                .maxByOrNull { it.opprettet }

        return sisteFerdigstilteForbehandling
            ?: throw IkkeFunnetException(
                code = "IKKE_FUNNET",
                detail = "Fant ingen ferdigstilte forbehandlinger på sak med id=${sakId.sakId}",
            )
    }

    fun hentDetaljertForbehandling(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertForbehandlingDto {
        val forbehandling = hentForbehandling(forbehandlingId)
        val sisteIverksatteBehandling =
            behandlingService.hentBehandling(forbehandling.sisteIverksatteBehandlingId)
                ?: throw InternfeilException(
                    "Fant ikke relatert behandling=${forbehandling.sisteIverksatteBehandlingId} for forbehandling=$forbehandlingId",
                )

        val avkorting =
            try {
                hentAvkortingForForbehandling(forbehandling, sisteIverksatteBehandling, brukerTokenInfo)
            } catch (e: Exception) {
                logger.warn("Kunne ikke hente tidligere avkorting for behandling med id=$sisteIverksatteBehandling", e)
                null
            }

        val pensjonsgivendeInntekt = dao.hentPensjonsgivendeInntekt(forbehandlingId)

        if (pensjonsgivendeInntekt == null) {
            throw InternfeilException(
                "Mangler pensjonsgivendeInntekt for behandlingId=$forbehandlingId",
            )
        }
        val summerteInntekter =
            try {
                dao.hentSummerteInntekter(forbehandling.id)
            } catch (e: Exception) {
                logger.error("Kunne ikke hente summerte inntekter", e)
                null
            }

        val beregnetEtteroppgjoerResultat =
            runBlocking {
                beregningKlient.hentBeregnetEtteroppgjoerResultat(
                    EtteroppgjoerHentBeregnetResultatRequest(
                        forbehandling.aar,
                        forbehandlingId,
                        sisteIverksatteBehandling.id,
                    ),
                    brukerTokenInfo,
                )
            }

        return DetaljertForbehandlingDto(
            behandling = forbehandling,
            opplysninger =
                EtteroppgjoerOpplysninger(
                    skatt = pensjonsgivendeInntekt,
                    summerteInntekter = summerteInntekter,
                    tidligereAvkorting = avkorting?.avkortingMedForventaInntekt,
                ),
            beregnetEtteroppgjoerResultat = beregnetEtteroppgjoerResultat,
            faktiskInntekt = avkorting?.avkortingMedFaktiskInntekt?.avkortingGrunnlag?.firstOrNull() as? FaktiskInntektDto,
        )
    }

    fun lagreBrevreferanse(
        forbehandlingId: UUID,
        brev: Brev,
    ) {
        val forbehandling = dao.hentForbehandling(forbehandlingId) ?: throw FantIkkeForbehandling(forbehandlingId)
        dao.lagreForbehandling(forbehandling.medBrev(brev))
    }

    fun hentEtteroppgjoerForbehandlinger(sakId: SakId): List<EtteroppgjoerForbehandling> = dao.hentForbehandlingerForSak(sakId)

    fun opprettEtteroppgjoerForbehandling(
        sakId: SakId,
        inntektsaar: Int,
        oppgaveId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        logger.info("Oppretter forbehandling for etteroppgjør sakId=$sakId, inntektsår=$inntektsaar")
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Kunne ikke hente sak=$sakId")

        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        sjekkAtOppgaveErGyldigForForbehandling(oppgave, sakId)

        kanOppretteForbehandlingForEtteroppgjoer(sak, inntektsaar, oppgaveId)

        val nyForbehandling = opprettOgLagreForbehandling(sak, inntektsaar, brukerTokenInfo)

        try {
            val pensjonsgivendeInntekter =
                runBlocking { pensjonsgivendeInntektService.hentSummerteInntekter(sak.ident, inntektsaar) }
            dao.lagrePensjonsgivendeInntekt(nyForbehandling.id, pensjonsgivendeInntekter)
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke hente og lagre ned summerte inntekter fra Skatteetaten for forbehandlingen i sakId=$sakId",
                e,
            )
        }
        try {
            val summerteInntekter =
                runBlocking { inntektskomponentService.hentSummerteInntekter(sak.ident, inntektsaar) }
            dao.lagreSummerteInntekter(nyForbehandling.id, summerteInntekter)
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke hente og lagre ned summerte inntekter fra A-ordningen for forbehandlingen i sakId=$sakId",
                e,
            )
        }

        etteroppgjoerService.oppdaterEtteroppgjoerStatus(sak.id, inntektsaar, EtteroppgjoerStatus.UNDER_FORBEHANDLING)

        hendelserService.registrerOgSendEtteroppgjoerHendelse(
            etteroppgjoerForbehandling = nyForbehandling,
            hendelseType = EtteroppgjoerHendelseType.OPPRETTET,
            saksbehandler = (brukerTokenInfo as? Saksbehandler)?.ident,
            utlandstilknytning = hentUtlandstilknytning(nyForbehandling),
        )

        oppgaveService.endreTilKildeBehandlingOgOppdaterReferanseOgMerknad(
            oppgaveId = oppgave.id,
            referanse = nyForbehandling.id.toString(),
            merknad = "Etteroppgjør for ${nyForbehandling.aar}",
        )

        return nyForbehandling
    }

    private fun registrerOgSendHendelseFerdigstilt(
        ferdigstiltForbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        hendelserService.registrerOgSendEtteroppgjoerHendelse(
            etteroppgjoerForbehandling = ferdigstiltForbehandling,
            etteroppgjoerResultat = hentBeregnetEtteroppgjoerResultat(ferdigstiltForbehandling, brukerTokenInfo),
            hendelseType = EtteroppgjoerHendelseType.FERDIGSTILT,
            saksbehandler = brukerTokenInfo.ident().takeIf { brukerTokenInfo is Saksbehandler },
            utlandstilknytning = hentUtlandstilknytning(ferdigstiltForbehandling),
        )
    }

    private fun hentBeregnetEtteroppgjoerResultat(
        ferdigstiltForbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto? {
        val beregnetEtteroppgjoerResultat =
            runBlocking {
                beregningKlient.hentBeregnetEtteroppgjoerResultat(
                    EtteroppgjoerHentBeregnetResultatRequest(
                        ferdigstiltForbehandling.aar,
                        ferdigstiltForbehandling.id,
                        ferdigstiltForbehandling.sisteIverksatteBehandlingId,
                    ),
                    brukerTokenInfo,
                )
            }
        return beregnetEtteroppgjoerResultat
    }

    private fun ferdigstillEtteroppgjoerOppgave(
        forbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        oppgaveService.ferdigstillOppgaveUnderBehandling(
            forbehandling.id.toString(),
            OppgaveType.ETTEROPPGJOER,
            brukerTokenInfo,
        )
    }

    private fun sjekkAtOppgaveErGyldigForForbehandling(
        oppgave: OppgaveIntern,
        sakId: SakId,
    ) {
        if (oppgave.sakId != sakId) {
            throw UgyldigForespoerselException("OPPGAVE_IKKE_I_SAK", "OppgaveId=${oppgave.id} matcher ikke sakId=$sakId")
        }

        if (oppgave.erAvsluttet()) {
            throw UgyldigForespoerselException("OPPGAVE_AVSLUTTET", "Oppgaven tilknyttet forbehandling er avsluttet og kan ikke behandles")
        }

        if (oppgave.type != OppgaveType.ETTEROPPGJOER) {
            throw UgyldigForespoerselException(
                "OPPGAVE_FEIL_TYPE",
                "Oppgaven har feil oppgaveType=${oppgave.type} til å opprette forbehandling",
            )
        }
    }

    fun opprettOppgaveForOpprettForbehandling(sakId: SakId) {
        etteroppgjoerTempService.opprettOppgaveForOpprettForbehandling(sakId)
    }

    fun opprettEtteroppgjoerForbehandlingIBulk(
        inntektsaar: Int,
        antall: Int,
        etteroppgjoerFilter: EtteroppgjoerFilter,
        spesifikkeSaker: List<SakId>,
        ekskluderteSaker: List<SakId>,
        spesifikkeEnheter: List<String>,
    ) {
        val relevanteSaker: List<SakId> =
            etteroppgjoerService.hentEtteroppgjoerSakerIBulk(
                inntektsaar = inntektsaar,
                antall = antall,
                etteroppgjoerFilter = etteroppgjoerFilter,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                spesifikkeSaker = spesifikkeSaker,
                ekskluderteSaker = ekskluderteSaker,
                spesifikkeEnheter = spesifikkeEnheter,
            )

        relevanteSaker.map { sakId ->
            try {
                etteroppgjoerTempService.opprettOppgaveForOpprettForbehandling(sakId)
            } catch (e: Error) {
                logger.error("Kunne ikke opprette etteroppgjør forbehandling for sak med id: $sakId", e)
            }
        }
    }

    fun lagreOgBeregnFaktiskInntekt(
        forbehandlingId: UUID,
        request: BeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetResultatOgBrevSomSkalSlettes {
        val forbehandling = dao.hentForbehandling(forbehandlingId) ?: throw FantIkkeForbehandling(forbehandlingId)

        if (!forbehandling.erRedigerbar()) {
            throw ForbehandlingKanIkkeEndres()
        }

        val beregningRequest =
            EtteroppgjoerBeregnFaktiskInntektRequest(
                sakId = forbehandling.sak.id,
                forbehandlingId = forbehandling.id,
                sisteIverksatteBehandling = forbehandling.sisteIverksatteBehandlingId,
                aar = forbehandling.aar,
                loennsinntekt = request.loennsinntekt,
                naeringsinntekt = request.naeringsinntekt,
                afp = request.afp,
                utlandsinntekt = request.utlandsinntekt,
                spesifikasjon = request.spesifikasjon,
            )

        val beregnetEtteroppgjoerResultat =
            runBlocking { beregningKlient.beregnAvkortingFaktiskInntekt(beregningRequest, brukerTokenInfo) }

        val beregnetForbehandling =
            forbehandling
                .tilBeregnet(beregnetEtteroppgjoerResultat)
                .also { it ->
                    dao.lagreForbehandling(it)
                    behandlingService
                        .hentBehandlingerForSak(it.sak.id)
                        .firstOrNull { revurdering -> revurdering.relatertBehandlingId == forbehandling.id.toString() }
                        ?.let { revurdering -> behandlingService.settBeregnet(revurdering.id, brukerTokenInfo) }
                }

        // Hvis forbehandlingen ikke lengre henviser til brevet når den er beregnet skal brevet slettes
        val brevSomSkalSlettes = forbehandling.brevId?.takeIf { beregnetForbehandling.brevId == null }

        hendelserService.registrerOgSendEtteroppgjoerHendelse(
            etteroppgjoerForbehandling = beregnetForbehandling,
            etteroppgjoerResultat = beregnetEtteroppgjoerResultat,
            hendelseType = EtteroppgjoerHendelseType.BEREGNET,
            saksbehandler = brukerTokenInfo.ident().takeIf { brukerTokenInfo is Saksbehandler },
            utlandstilknytning = hentUtlandstilknytning(beregnetForbehandling),
        )

        return BeregnetResultatOgBrevSomSkalSlettes(
            resultat = beregnetEtteroppgjoerResultat,
            brevIdOgSakIdSomSkalSlettes = brevSomSkalSlettes?.let { it to forbehandling.sak.id },
        )
    }

    fun lagreInformasjonFraBruker(
        forbehandlingId: UUID,
        harMottattNyInformasjon: JaNei,
        endringErTilUgunstForBruker: JaNei?,
        beskrivelseAvUgunst: String?,
    ) {
        val forbehandling = dao.hentForbehandling(forbehandlingId) ?: throw FantIkkeForbehandling(forbehandlingId)
        if (!forbehandling.erRedigerbar()) {
            throw ForbehandlingKanIkkeEndres()
        }

        forbehandling
            .oppdaterBrukerHarSvart(harMottattNyInformasjon, endringErTilUgunstForBruker, beskrivelseAvUgunst)
            .also { dao.lagreForbehandling(it) }
    }

    fun sjekkAtOppgavenErTildeltSaksbehandler(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val oppgave =
            oppgaveService
                .hentOppgaverForReferanse(forbehandlingId.toString())
                .firstOrNull { it.erIkkeAvsluttet() }
                ?: throw InternfeilException("Fant ingen oppgaver under behandling for forbehandlingId=$forbehandlingId")

        if (oppgave.saksbehandler?.ident != brukerTokenInfo.ident()) {
            throw IkkeTillattException(
                "IKKE_TILGANG_TIL_BEHANDLING",
                "Saksbehandler ${brukerTokenInfo.ident()} er ikke tildelt oppgaveId=${oppgave.id}",
            )
        }

        if (oppgave.erAvsluttet()) {
            throw UgyldigForespoerselException(
                "OPPGAVE_AVSLUTTET",
                "Oppgaven tilknyttet forbehandlingId=$forbehandlingId er avsluttet og kan ikke behandles",
            )
        }
    }

    private fun kanOppretteForbehandlingForEtteroppgjoer(
        sak: Sak,
        inntektsaar: Int,
        oppgaveId: UUID,
    ) {
        // Sak
        if (sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id} med sakType=${sak.sakType}")
            throw IkkeTillattException("FEIL_SAKTYPE", "Kan ikke opprette forbehandling for sakType=${sak.sakType}")
        }

        if (oppgaveService
                .hentOppgaverForSak(sak.id)
                .filter { it.kilde == OppgaveKilde.BEHANDLING && it.id != oppgaveId }
                .any { it.erIkkeAvsluttet() }
        ) {
            logger.info("Kan ikke opprette forbehandling for sak=${sak.id} på grunn av allerede åpne behandlinger")
            throw IkkeTillattException(
                "ALLEREDE_AAPEN_BEHANDLING",
                "Kan ikke opprette forbehandling for sak=${sak.id} på grunn av allerede åpne behandlinger",
            )
        }

        if (behandlingService.hentSisteIverksatteBehandling(sak.id) == null) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id}, sak mangler iverksatt behandling")
            throw InternfeilException(
                "Kan ikke opprette forbehandling for sak=${sak.id}, sak mangler iverksatt behandling",
            )
        }

        // Etteroppgjør
        val etteroppgjoer = etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
        if (etteroppgjoer == null) {
            logger.error("Fant ikke etteroppgjør for sak=${sak.id} og inntektsår=$inntektsaar")
            throw IkkeTillattException(
                "MANGLER_ETTEROPPGJOER",
                "Kan ikke opprette forbehandling fordi sak=${sak.id} ikke har et etteroppgjør",
            )
        }

        if (!etteroppgjoer.mottattSkatteoppgjoer()) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id} på grunn av feil etteroppgjoerStatus=${etteroppgjoer.status}")
            throw IkkeTillattException(
                "FEIL_ETTEROPPGJOERS_STATUS",
                "Kan ikke opprette forbehandling på grunn av feil etteroppgjør status=${etteroppgjoer.status}",
            )
        }

        // Forbehandling
        val forbehandlinger = hentEtteroppgjoerForbehandlinger(sak.id)
        if (forbehandlinger.any { it.aar == inntektsaar && it.erUnderBehandling() }) {
            throw IkkeTillattException(
                "FORBEHANDLING_FINNES_ALLEREDE",
                "Kan ikke opprette forbehandling fordi det allerede eksisterer en forbehandling som ikke er ferdigstilt",
            )
        }
    }

    private fun opprettOgLagreForbehandling(
        sak: Sak,
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        val sisteIverksatteBehandling = runBlocking { hentSisteIverksatteBehandlingMedAvkorting(sak.id, brukerTokenInfo) }

        krevIkkeNull(sisteIverksatteBehandling) {
            "Fant ikke sisteIverksatteBehandling for Sak=${sak.id} kan derfor ikke opprette forbehandling"
        }

        logger.info("Oppretter forbehandling for ${sak.id} som baserer seg på siste iverksatte behandling med id ${sisteIverksatteBehandling}")

        val virkOgOpphoer = runBlocking { vedtakKlient.hentInnvilgedePerioder(sak.id, brukerTokenInfo) }
        val innvilgetPeriode = utledInnvilgetPeriode(virkOgOpphoer, inntektsaar)

        return EtteroppgjoerForbehandling.opprett(sak, innvilgetPeriode, sisteIverksatteBehandling.id).also {
            dao.lagreForbehandling(it)
        }
    }

    private suspend fun hentSisteIverksatteBehandlingMedAvkorting(sakId: SakId, brukerTokenInfo: BrukerTokenInfo): UUID {
        val behandlingerMedAarsoppgjoer = beregningKlient.hentBehandlingerMedAarsoppgjoerForSak(sakId, brukerTokenInfo)

        return behandlingService.hentBehandlingerForSak(sakId)
            .filter { BehandlingStatus.iverksattEllerAttestert().contains(it.status) && !it.erAvslagNySoeknad() }
            .filter { it.id in behandlingerMedAarsoppgjoer }
            .maxBy { it.behandlingOpprettet }.id
    }


    private fun utledInnvilgetPeriode(
        innvilgedePerioder: List<InnvilgetPeriodeDto>,
        inntektsaar: Int,
    ): Periode {
        val periode =
            krevIkkeNull(innvilgedePerioder.singleOrNull()) {
                "Støtter ikke utledning av innvilget periode med 0 eller mer enn en periode: $innvilgedePerioder"
            }.periode
        if (periode.fom > YearMonth.of(inntektsaar, Month.DECEMBER)) {
            throw InternfeilException(
                "Sak er ikke innvilget i året $inntektsaar, skal ikke kunne opprette etteroppgjør for året $inntektsaar",
            )
        }
        return Periode(
            fom = maxOf(periode.fom, YearMonth.of(inntektsaar, Month.JANUARY)),
            tom =
                minOf(
                    periode.tom ?: YearMonth.of(inntektsaar, Month.DECEMBER),
                    YearMonth.of(inntektsaar, Month.DECEMBER),
                ),
        )
    }

    private fun hentAvkortingForForbehandling(
        forbehandling: EtteroppgjoerForbehandling,
        sisteIverksatteBehandling: Behandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBeregnetAvkorting {
        val request =
            EtteroppgjoerBeregnetAvkortingRequest(
                forbehandling = forbehandling.id,
                sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                aar = forbehandling.aar,
                sakId = sisteIverksatteBehandling.sak.id,
            )
        logger.info("Henter avkorting for forbehandling: $request")
        return runBlocking {
            beregningKlient.hentAvkortingForForbehandlingEtteroppgjoer(
                request,
                brukerTokenInfo,
            )
        }
    }

    fun kopierOgLagreNyForbehandling(
        forbehandlingId: UUID,
        sakId: SakId,
    ): EtteroppgjoerForbehandling {
        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatteBehandling(sakId)
                ?: throw InternfeilException(
                    "Fant ikke siste iverksatte behandling for sak=$sakId ved kopiering av forbehandling",
                )

        val forbehandling =
            dao.hentForbehandling(forbehandlingId) ?: throw NotFoundException("Fant ikke forbehandling med id $forbehandlingId")

        val forbehandlingCopy =
            forbehandling.copy(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                opprettet = Tidspunkt.now(), // ny dato
                kopiertFra = forbehandling.id,
                sisteIverksatteBehandlingId = sisteIverksatteBehandling.id,
                brevId = null,
                varselbrevSendt = null,
            )

        dao.lagreForbehandling(forbehandlingCopy)
        dao.kopierSummerteInntekter(forbehandling.id, forbehandlingCopy.id)
        dao.kopierPensjonsgivendeInntekt(forbehandling.id, forbehandlingCopy.id)

        return forbehandlingCopy
    }

    private fun sjekkAtInntekterErOppdatert(forbehandling: EtteroppgjoerForbehandling) {
        val sistePensjonsgivendeInntekt =
            runBlocking {
                pensjonsgivendeInntektService.hentSummerteInntekter(forbehandling.sak.ident, forbehandling.aar)
            }
        val sisteSummerteInntekter =
            runBlocking {
                inntektskomponentService.hentSummerteInntekter(forbehandling.sak.ident, forbehandling.aar)
            }

        // verifisere at vi har siste pensjonsgivende inntekt i databasen
        dao.hentPensjonsgivendeInntekt(forbehandling.id).let { pgi ->
            if (pgi?.loensinntekt != sistePensjonsgivendeInntekt.loensinntekt ||
                pgi.naeringsinntekt != sistePensjonsgivendeInntekt.naeringsinntekt
            ) {
                throw InternfeilException(
                    "Forbehandling med id=${forbehandling.id} er ikke oppdatert med siste Pensjonsgivende inntekt",
                )
            }
        }

        // verifisere at vi har siste summerte inntekter fra A-inntekt
        dao.hentSummerteInntekterNonNull(forbehandling.id).let { summerteInntekter ->
            if (summerteInntekter.afp != sisteSummerteInntekter.afp) {
                throw InternfeilException(
                    "Forbehandling med id=${forbehandling.id} er ikke oppdatert med siste AFP inntekt",
                )
            }
            if (summerteInntekter.loenn != sisteSummerteInntekter.loenn) {
                throw InternfeilException(
                    "Forbehandling med id=${forbehandling.id} er ikke oppdatert med siste LOENN inntekt",
                )
            }
            if (summerteInntekter.oms != sisteSummerteInntekter.oms) {
                throw InternfeilException(
                    "Forbehandling med id=${forbehandling.id} er ikke oppdatert med siste OMS inntekt",
                )
            }
        }
    }

    private fun sjekkAtSisteIverksatteBehandlingErRiktig(forbehandling: EtteroppgjoerForbehandling) {
        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatteBehandling(forbehandling.sak.id)
                ?: throw InternfeilException("Kunne ikke finne siste iverksatte behandling for sakId=${forbehandling.sak.id}")

        // verifisere at vi bruker siste iverksatte behandling
        if (sisteIverksatteBehandling.id != forbehandling.sisteIverksatteBehandlingId) {
            throw InternfeilException(
                "Forbehandling med id=${forbehandling.id} er ikke oppdatert med siste iverksatte behandling=${sisteIverksatteBehandling.id}",
            )
        }
    }

    fun ferdigstillForbehandlingUtenBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        val detaljertBehandling = hentDetaljertForbehandling(forbehandlingId, brukerTokenInfo)
        if (detaljertBehandling.beregnetEtteroppgjoerResultat?.resultatType != EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING) {
            throw UgyldigForespoerselException(
                "ETTEROPPGJOER_RESULTAT_TRENGER_BREV",
                "Etteroppgjøret kan kun ferdigstilles uten utsendt brev hvis resultatet er ingen utbetaling " +
                    "og ingen endring, mens resultatet for etteroppgjøret i sak " +
                    "${detaljertBehandling.behandling.sak.id} for år ${detaljertBehandling.behandling.aar} er " +
                    "${detaljertBehandling.beregnetEtteroppgjoerResultat?.resultatType}",
            )
        }
        return ferdigstillForbehandling(detaljertBehandling.behandling, brukerTokenInfo)
    }

    private fun hentUtlandstilknytning(ferdigstiltForbehandling: EtteroppgjoerForbehandling): Utlandstilknytning? =
        behandlingService.hentUtlandstilknytningForSak(ferdigstiltForbehandling.sak.id)
}

data class BeregnFaktiskInntektRequest(
    val loennsinntekt: Int,
    val afp: Int,
    val naeringsinntekt: Int,
    val utlandsinntekt: Int,
    val spesifikasjon: String,
)

data class InformasjonFraBrukerRequest(
    val harMottattNyInformasjon: JaNei,
    val endringErTilUgunstForBruker: JaNei?,
    val beskrivelseAvUgunst: String?,
)

data class BeregnetResultatOgBrevSomSkalSlettes(
    val resultat: BeregnetEtteroppgjoerResultatDto,
    val brevIdOgSakIdSomSkalSlettes: Pair<BrevID, SakId>?,
)

class FantIkkeForbehandling(
    val behandlingId: UUID,
) : IkkeFunnetException(
        code = "MANGLER_FORBEHANDLING_ETTEROPPGJOER",
        detail = "Fant ikke forbehandling etteroppgjør $behandlingId",
    )

class ForbehandlingKanIkkeEndres :
    IkkeTillattException(
        code = "FORBEHANDLINGEN_KAN_IKKE_ENDRES",
        detail = "Forbehandlingen kan ikke endres",
    )
