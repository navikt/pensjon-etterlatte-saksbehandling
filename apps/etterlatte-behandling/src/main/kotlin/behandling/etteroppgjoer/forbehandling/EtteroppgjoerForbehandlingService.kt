package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDataService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.PensjonsgivendeInntektService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
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
    private val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService,
    private val etteroppgjoerDataService: EtteroppgjoerDataService,
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

        val forbehandling = hentForbehandling(forbehandling.id)

        sjekkAtViBrukerSisteInntekter(forbehandling)

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

        sjekkAtViBrukerSisteIverksatteBehandling(forbehandling, brukerTokenInfo)
        sjekkAtViBrukerSisteInntekter(forbehandling)

        etteroppgjoerOppgaveService.ferdigstillForbehandlingOppgave(forbehandling, brukerTokenInfo)

        haandterDoedsfallEtterEtteroppgjoersAar(forbehandling)

        return forbehandling.tilFerdigstilt().also {
            dao.lagreForbehandling(it)
            etteroppgjoerService.oppdaterEtteroppgjoerEtterFerdigstiltForbehandling(it)
            registrerOgSendHendelseFerdigstilt(it, brukerTokenInfo)
        }
    }

    fun ferdigstillForbehandlingUtenBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        val forbehandling = hentForbehandling(forbehandlingId)

        if (!forbehandling.kanFerdigstillesUtenBrev()) {
            throw UgyldigForespoerselException(
                "ETTEROPPGJOER_RESULTAT_TRENGER_BREV",
                "Etteroppgjøret kan kun ferdigstilles uten utsendt brev dersom resultatet er 'ingen utbetaling' " +
                    "og 'ingen endring' eller 'dødsfall i etteroppgjørsåret'. Resultat for etteroppgjøret i denne saken " +
                    "${forbehandling.sak.id} for år ${forbehandling.aar} er " +
                    "${forbehandling.etteroppgjoerResultatType!!}",
            )
        }

        return ferdigstillForbehandling(forbehandling, brukerTokenInfo)
    }

    fun avbrytForbehandling(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        aarsak: AarsakTilAvbryteForbehandling,
        kommentar: String?,
    ) {
        logger.info("Avbryter forbehandling med id=$forbehandlingId")
        val forbehandling = hentForbehandling(forbehandlingId)
        etteroppgjoerOppgaveService.sjekkAtOppgavenErTildeltSaksbehandler(forbehandling.id, brukerTokenInfo)

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

    fun hentForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling =
        dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

    fun hentForbehandlinger(
        sakId: SakId,
        etteroppgjoersAar: Int,
    ): List<EtteroppgjoerForbehandling> = dao.hentForbehandlingerForSak(sakId, etteroppgjoersAar)

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

        val pensjonsgivendeInntekt =
            try {
                dao.hentPensjonsgivendeInntekt(forbehandlingId)
            } catch (e: Exception) {
                logger.error("Kunne ikke hente pgi", e)
                null
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

        val avkorting =
            etteroppgjoerDataService.hentAvkortingForForbehandling(
                forbehandling,
                sisteIverksatteBehandling,
                brukerTokenInfo,
            )

        return DetaljertForbehandlingDto(
            forbehandling = forbehandling,
            opplysninger =
                EtteroppgjoerOpplysninger(
                    summertPgi = pensjonsgivendeInntekt,
                    summertAInntekt = summerteInntekter,
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
        val forbehandling = hentForbehandling(forbehandlingId)
        dao.lagreForbehandling(forbehandling.medBrev(brev))
    }

    fun opprettEtteroppgjoerForbehandling(
        sakId: SakId,
        inntektsaar: Int,
        oppgaveId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        logger.info("Oppretter forbehandling for etteroppgjør sakId=$sakId, inntektsår=$inntektsaar")
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Kunne ikke hente sak=$sakId")

        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        validerOppgaveForOpprettForbehandling(oppgave, sakId)

        val etteroppgjoer =
            etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
                ?: throw IkkeTillattException(
                    "MANGLER_ETTEROPPGJOER",
                    "Kan ikke opprette forbehandling fordi sak=${sak.id} ikke har et etteroppgjør",
                )

        kanOppretteForbehandlingForEtteroppgjoer(sak, inntektsaar, oppgaveId, etteroppgjoer)

        val nyForbehandling = opprettForbehandling(sak, inntektsaar, etteroppgjoer, brukerTokenInfo)
        dao.lagreForbehandling(nyForbehandling)
        etteroppgjoerService.oppdaterEtteroppgjoerStatus(sak.id, inntektsaar, EtteroppgjoerStatus.UNDER_FORBEHANDLING)

        hentOgLagreAInntektOgPgi(sak, nyForbehandling)

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

    fun hentBeregnetEtteroppgjoerResultat(
        forbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto? {
        val beregnetEtteroppgjoerResultat =
            runBlocking {
                beregningKlient.hentBeregnetEtteroppgjoerResultat(
                    EtteroppgjoerHentBeregnetResultatRequest(
                        forbehandling.aar,
                        forbehandling.id,
                        forbehandling.sisteIverksatteBehandlingId,
                    ),
                    brukerTokenInfo,
                )
            }
        return beregnetEtteroppgjoerResultat
    }

    fun opprettOppgaveForOpprettForbehandling(
        sakId: SakId,
        opprettetManuelt: Boolean,
        etteroppgjoerAar: Int,
    ) {
        etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(
            sakId = sakId,
            opprettetManuelt = opprettetManuelt,
            inntektsAar = etteroppgjoerAar,
        )
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
                etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(
                    sakId = sakId,
                    inntektsAar = inntektsaar,
                )
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
        val forbehandling = hentForbehandling(forbehandlingId)

        if (!forbehandling.erRedigerbar()) {
            throw ForbehandlingKanIkkeEndres()
        }

        val opphoerFom =
            runBlocking { vedtakKlient.hentInnvilgedePerioder(forbehandling.sak.id, brukerTokenInfo) }
                .maxBy { it.periode.fom }
                .periode.tom
                ?.plusMonths(1)

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
                harDoedsfall =
                    forbehandling.opphoerSkyldesDoedsfall == JaNei.JA &&
                        forbehandling.opphoerSkyldesDoedsfallIEtteroppgjoersaar == JaNei.NEI,
                innvilgetPeriodeIEtteroppgjoersAar = forbehandling.innvilgetPeriode,
                opphoerFom = opphoerFom,
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
        val forbehandling = hentForbehandling(forbehandlingId)
        if (!forbehandling.erRedigerbar()) {
            throw ForbehandlingKanIkkeEndres()
        }

        forbehandling
            .oppdaterBrukerHarSvart(harMottattNyInformasjon, endringErTilUgunstForBruker, beskrivelseAvUgunst)
            .also { dao.lagreForbehandling(it) }
    }

    fun lagreOmOpphoerSkyldesDoedsfall(
        forbehandlingId: UUID,
        opphoerSkyldesDoedsfall: JaNei,
        opphoerSkyldesDoedsfallIEtteroppgjoersaar: JaNei?,
    ) {
        val forbehandling = hentForbehandling(forbehandlingId)
        if (!forbehandling.erRedigerbar()) {
            throw ForbehandlingKanIkkeEndres()
        }

        forbehandling
            .oppdaterOmOpphoerSkyldesDoedsfall(
                opphoerSkyldesDoedsfall,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar,
            ).also {
                dao.lagreForbehandling(it)
            }
    }

    fun kanOppretteForbehandlingForEtteroppgjoer(
        sak: Sak,
        inntektsaar: Int,
        oppgaveId: UUID,
        etteroppgjoer: Etteroppgjoer,
    ) {
        // Sak
        if (sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id} med sakType=${sak.sakType}")
            throw IkkeTillattException("FEIL_SAKTYPE", "Kan ikke opprette forbehandling for sakType=${sak.sakType}")
        }

        if (!etteroppgjoer.kanOppretteForbehandling()) {
            logger.error(
                "Kan ikke opprette forbehandling for sak=${sak.id} på grunn av feil status i etteroppgjoeret ${etteroppgjoer.status}",
            )
            throw IkkeTillattException(
                "FEIL_ETTEROPPGJOERS_STATUS",
                "Kan ikke opprette forbehandling på grunn av feil etteroppgjør status=${etteroppgjoer.status}",
            )
        }

        sjekkHarAapneBehandlinger(sak.id, oppgaveId)

        // Siste iverksatte behandling
        if (behandlingService.hentSisteIverksatteBehandling(sak.id) == null) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id}, sak mangler iverksatt behandling")
            throw InternfeilException(
                "Kan ikke opprette forbehandling for sak=${sak.id}, sak mangler iverksatt behandling",
            )
        }

        // Forbehandling
        val forbehandlinger = hentForbehandlinger(sak.id, etteroppgjoer.inntektsaar)
        if (forbehandlinger.any { it.aar == inntektsaar && it.erUnderBehandling() }) {
            throw IkkeTillattException(
                "FORBEHANDLING_FINNES_ALLEREDE",
                "Kan ikke opprette forbehandling fordi det allerede eksisterer en forbehandling som ikke er ferdigstilt",
            )
        }
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

    private fun opprettForbehandling(
        sak: Sak,
        inntektsaar: Int,
        etteroppgjoer: Etteroppgjoer,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        val sisteAvkortingOgOpphoer =
            runBlocking { etteroppgjoerDataService.hentSisteIverksatteBehandlingMedAvkorting(sak.id, brukerTokenInfo) }

        krevIkkeNull(sisteAvkortingOgOpphoer) {
            "Fant ikke sisteIverksatteBehandling for Sak=${sak.id} kan derfor ikke opprette forbehandling"
        }

        logger.info(
            "Oppretter forbehandling for ${sak.id} som baserer seg på siste iverksatte behandling med id $sisteAvkortingOgOpphoer",
        )

        val virkOgOpphoer = runBlocking { vedtakKlient.hentInnvilgedePerioder(sak.id, brukerTokenInfo) }
        val innvilgetPeriode = utledInnvilgetPeriode(virkOgOpphoer, inntektsaar)

        return EtteroppgjoerForbehandling
            .opprett(
                sak = sak,
                innvilgetPeriode = innvilgetPeriode,
                sisteIverksatteBehandling = sisteAvkortingOgOpphoer.sisteBehandlingMedAvkorting,
                harVedtakAvTypeOpphoer = sisteAvkortingOgOpphoer.opphoerFom != null,
                mottattSkatteoppgjoer = etteroppgjoer.status != EtteroppgjoerStatus.MANGLER_SKATTEOPPGJOER,
            )
    }

    private fun utledInnvilgetPeriode(
        innvilgedePerioder: List<InnvilgetPeriodeDto>,
        inntektsaar: Int,
    ): Periode {
        if (innvilgedePerioder.isEmpty()) {
            throw UgyldigForespoerselException(
                "MANGLER_INNVILGET_PERIODE",
                "Saken har ingen innvilget periode. Dobbeltsjekk at dette stemmer, hvis saken er opphørt fra første " +
                    "virkiningstidspunkt er det ikke noe å behandle et etteroppgjør på. Hvis det ikke stemmer " +
                    "må det meldes feil i porten.",
            )
        }
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

    fun sjekkHarAapneBehandlinger(
        sakId: SakId,
        oppgaveId: UUID?,
    ) {
        // Åpne behandlinger
        if (oppgaveService
                .hentOppgaverForSak(sakId)
                .filter { it.kilde == OppgaveKilde.BEHANDLING && it.id != oppgaveId }
                .any { it.erIkkeAvsluttet() }
        ) {
            logger.info("Kan ikke opprette forbehandling for sak=$sakId på grunn av allerede åpne behandlinger")
            throw IkkeTillattException(
                "ALLEREDE_AAPEN_BEHANDLING",
                "Kan ikke opprette forbehandling for sak=$sakId på grunn av allerede åpne behandlinger",
            )
        }
    }

    fun kopierOgLagreNyForbehandling(
        forbehandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandling {
        val sisteIverksatteBehandling =
            runBlocking { etteroppgjoerDataService.hentSisteIverksatteBehandlingMedAvkorting(sakId, brukerTokenInfo) }

        val forbehandling = hentForbehandling(forbehandlingId)

        val forbehandlingCopy =
            forbehandling.copy(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                opprettet = Tidspunkt.now(), // ny dato
                kopiertFra = forbehandling.id,
                sisteIverksatteBehandlingId = sisteIverksatteBehandling.sisteBehandlingMedAvkorting,
                brevId = null,
                varselbrevSendt = null,
            )

        dao.lagreForbehandling(forbehandlingCopy)
        dao.kopierSummerteInntekter(forbehandling.id, forbehandlingCopy.id)
        dao.kopierPensjonsgivendeInntekt(forbehandling.id, forbehandlingCopy.id)

        return forbehandlingCopy
    }

    private fun sjekkAtViBrukerSisteInntekter(forbehandling: EtteroppgjoerForbehandling) {
        if (forbehandling.mottattSkatteoppgjoer) {
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
        } else {
            logger.info(
                "Forbehandling=${forbehandling.id} har ikke mottatt skatteoppgjør, sjekker derfor ikke at vi har siste inntekter i sakId=${forbehandling.sak.id}",
            )
        }
    }

    private fun sjekkAtViBrukerSisteIverksatteBehandling(
        forbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val sisteIverksatteBehandling =
            runBlocking {
                etteroppgjoerDataService.hentSisteIverksatteBehandlingMedAvkorting(
                    forbehandling.sak.id,
                    brukerTokenInfo,
                )
            }

        // verifisere at vi bruker siste iverksatte behandling
        if (sisteIverksatteBehandling.sisteBehandlingMedAvkorting != forbehandling.sisteIverksatteBehandlingId) {
            throw InternfeilException(
                "Forbehandling med id=${forbehandling.id} er ikke oppdatert med siste iverksatte behandling=${sisteIverksatteBehandling.sisteBehandlingMedAvkorting}",
            )
        }
    }

    private fun hentUtlandstilknytning(ferdigstiltForbehandling: EtteroppgjoerForbehandling): Utlandstilknytning? =
        behandlingService.hentUtlandstilknytningForSak(ferdigstiltForbehandling.sak.id)

    private fun haandterDoedsfallEtterEtteroppgjoersAar(forbehandling: EtteroppgjoerForbehandling) {
        val opphoerSkyldesDoedsfall = forbehandling.opphoerSkyldesDoedsfall == JaNei.JA

        if (opphoerSkyldesDoedsfall) {
            krevIkkeNull(forbehandling.opphoerSkyldesDoedsfallIEtteroppgjoersaar) {
                "Mangler svar på opphør skyldes dødsfall i etteroppgjørsåret"
            }

            if (forbehandling.skalEtterbetalesTilDoedsbo()) {
                oppgaveService.opprettOppgave(
                    referanse = forbehandling.id.toString(),
                    sakId = forbehandling.sak.id,
                    type = OppgaveType.ETTEROPPGJOER_OPPRETT_REVURDERING,
                    merknad = "Revurdering for etterbetaling til dødsbo kan opprettes",
                    kilde = OppgaveKilde.SAKSBEHANDLER,
                )
            }
        }
    }

    private fun hentOgLagreAInntektOgPgi(
        sak: Sak,
        forbehandling: EtteroppgjoerForbehandling,
    ) {
        try {
            val pensjonsgivendeInntekter =
                runBlocking { pensjonsgivendeInntektService.hentSummerteInntekter(sak.ident, forbehandling.aar) }
            dao.lagrePensjonsgivendeInntekt(forbehandling.id, pensjonsgivendeInntekter)
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke hente og lagre PGI fra Skatt for forbehandlingen i sakId=${sak.id}",
                e,
            )
            if (forbehandling.mottattSkatteoppgjoer) {
                throw InternfeilException(
                    "Kunne ikke hente PGI fra skatt. Forbehandlingen kunne ikke opprettes. Prøv igjen senere, og meld sak hvis det ikke fungerer. Sak = ${sak.id}",
                    e,
                )
            }
        }

        try {
            val summerteInntekter =
                runBlocking { inntektskomponentService.hentSummerteInntekter(sak.ident, forbehandling.aar) }
            dao.lagreSummerteInntekter(forbehandling.id, summerteInntekter)
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke hente og lagre ned summerte inntekter fra A-ordningen for forbehandlingen i sakId=${sak.id}",
                e,
            )
            if (forbehandling.mottattSkatteoppgjoer) {
                throw InternfeilException(
                    "Kunne ikke inntekter fra A-ordningen. Forbehandlingen kunne ikke opprettes. Prøv igjen senere, og meld sak hvis det ikke fungerer. Sak = ${sak.id}",
                    e,
                )
            }
        }
    }

    private fun validerOppgaveForOpprettForbehandling(
        oppgave: OppgaveIntern,
        sakId: SakId,
    ): OppgaveIntern {
        if (oppgave.sakId != sakId) {
            throw UgyldigForespoerselException(
                "OPPGAVE_IKKE_I_SAK",
                "OppgaveId=${oppgave.id} matcher ikke sakId=$sakId",
            )
        }

        if (oppgave.erAvsluttet()) {
            throw UgyldigForespoerselException(
                "OPPGAVE_AVSLUTTET",
                "Oppgaven tilknyttet forbehandling er avsluttet og kan ikke behandles",
            )
        }

        if (oppgave.type != OppgaveType.ETTEROPPGJOER) {
            throw UgyldigForespoerselException(
                "OPPGAVE_FEIL_TYPE",
                "Oppgaven har feil oppgaveType=${oppgave.type} til å opprette forbehandling",
            )
        }

        return oppgave
    }
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

data class OpphoerSkyldesDoedsfallRequest(
    val opphoerSkyldesDoedsfall: JaNei,
    val opphoerSkyldesDoedsfallIEtteroppgjoersaar: JaNei?,
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

class FantIkkEtteroppgjoer(
    val sakId: SakId,
    val inntektsaar: Int,
) : IkkeFunnetException(
        code = "MANGLER_ETTEROPPGJOER",
        detail = "Fant ikke etteroppgjør $inntektsaar for sakId=$sakId",
    )

class ForbehandlingKanIkkeEndres :
    IkkeTillattException(
        code = "FORBEHANDLINGEN_KAN_IKKE_ENDRES",
        detail = "Forbehandlingen kan ikke endres",
    )
