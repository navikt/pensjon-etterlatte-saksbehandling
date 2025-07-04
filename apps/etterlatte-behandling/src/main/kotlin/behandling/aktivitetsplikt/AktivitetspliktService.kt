package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradOgUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_100
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktSkjoennsmessigVurdering
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.revurdering.BehandlingKanIkkeEndres
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktResponse
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktResponse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class AktivitetspliktService(
    private val aktivitetspliktDao: AktivitetspliktDao,
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao,
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val statistikkKafkaProducer: BehandlingHendelserKafkaProducer,
    private val aktivitetspliktKopierService: AktivitetspliktKopierService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? =
        aktivitetspliktDao.finnSenesteAktivitetspliktOppfolging(behandlingId)

    fun hentAktivitetspliktDto(
        sakId: SakId,
        behandlingId: UUID?,
    ): AktivitetspliktDto {
        val faktiskBehandlingId =
            behandlingId ?: behandlingService.hentSisteIverksatte(sakId)?.id
                ?: throw ManglerBehandlingIdEllerIverksattBehandlingException(sakId)

        val grunnlag =
            krevIkkeNull(grunnlagService.hentOpplysningsgrunnlag(faktiskBehandlingId)) {
                "Fant ikke opplysningsgrunnlag for behandlingId=$faktiskBehandlingId"
            }

        val avdoedDoedsdato =
            grunnlag
                .hentAvdoede()
                .singleOrNull()
                ?.hentDoedsdato()
                ?.verdi

        if (avdoedDoedsdato == null) {
            val aktuellBehandling = behandlingService.hentBehandling(faktiskBehandlingId)
            if (aktuellBehandling?.status in BehandlingStatus.iverksattEllerAttestert()) {
                throw InternfeilException(
                    "Mangler avdødes dødsdato i en behandling som er iverksatt/attestert, " +
                        "med sakId=$sakId. Dette gjør at vi ikke får hentet ut riktig aktivitetsplikt for saken " +
                        "og de vil kunne mangle fra statistikken. Årsaken til at vi ikke har en dødsdato og " +
                        "dermed ikke får riktig? statistikk bør sees på, og en sending av dto for denne saken " +
                        "til statistikk bør vurderes",
                )
            } else {
                throw ManglerDoedsdatoUnderBehandlingException(sakId)
            }
        }

        val aktiviteter = hentAktiviteter(faktiskBehandlingId)

        val sisteVurdering = hentVurderingForSak(sakId)

        return AktivitetspliktDto(
            sakId = sakId,
            avdoedDoedsmaaned = YearMonth.from(avdoedDoedsdato),
            aktivitetsgrad = sisteVurdering.aktivitet.map { it.toDto() },
            unntak = sisteVurdering.unntak.map { it.toDto() },
            brukersAktivitet = aktiviteter.map { it.toDto() },
        )
    }

    fun oppfyllerAktivitetsplikt6mnd(
        sakId: SakId,
        aktivitetspliktDato: LocalDate,
    ): Boolean {
        if (harVarigUnntak(sakId)) {
            return true
        }
        val nyesteVurdering = hentVurderingForSak(sakId)

        // TODO("se på å heller ha en tom på vurderinger")
        val relevantVurdering =
            nyesteVurdering.aktivitet.filter { it.fom <= aktivitetspliktDato }.maxByOrNull { it.fom }
        val relevantUnntak =
            nyesteVurdering.unntak.find {
                (it.fom ?: aktivitetspliktDato) >= aktivitetspliktDato &&
                    (it.tom ?: aktivitetspliktDato) <= aktivitetspliktDato
            }

        val oppfyllerAktivitet = relevantVurdering?.let { oppfyllerAktivitet(it) } == true
        val harUnntak = relevantUnntak?.let { harUnntakPaaDato(it, aktivitetspliktDato) } == true
        return oppfyllerAktivitet || harUnntak
    }

    private fun oppfyllerAktivitet(aktivitetsgrad: AktivitetspliktAktivitetsgrad) =
        (aktivitetsgrad.aktivitetsgrad in listOf(AKTIVITET_OVER_50, AKTIVITET_100)).also {
            if (it) {
                logger.info("Aktivitetsgrad er over 50% eller 100%, ingen revurdering opprettes for sak ${aktivitetsgrad.sakId}")
            } else {
                logger.info("Aktivitetsgrad er under 50%, revurdering skal opprettes for sak ${aktivitetsgrad.sakId}")
            }
        }

    fun opprettOppfoelgingsoppgaveUnntak(
        unntak: List<AktivitetspliktUnntak>,
        sakId: SakId,
        doedsdatoAvdoed: LocalDate? = null,
    ) {
        if (!featureToggleService.isEnabled(AktivitetspliktOppgaveToggles.UNNTAK_UTEN_FRIST, false)) {
            return
        }

        try {
            // Hvis bruker har varig unntak er det ikke nødvendig å følge opp
            if (unntak.any { it.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT }) {
                return
            }
            if (doedsdatoAvdoed != null && doedsdatoAvdoed.plusMonths(4L) >= LocalDate.now()) {
                // unødvendig å lage oppfølgingsoppgave hvis det er under 4 måneder fra dødsfallet,
                // siden vi uansett lager en oppgave for å sende ut informasjonsbrev om aktivitetsplikten
                return
            }

            val unntakUtenTilOgMed =
                unntak
                    .filter { it.tom == null }
                    .map {
                        // Lag en oppfølgingsoppgave for å følge opp unntak uten frist
                        oppgaveService.opprettOppgave(
                            referanse = it.id.toString(),
                            sakId = sakId,
                            kilde = OppgaveKilde.SAKSBEHANDLER,
                            type = OppgaveType.OPPFOELGING,
                            merknad = "Unntak ${it.unntak.lesbartNavn} har ikke til og med dato.",
                            frist =
                                LocalDate
                                    .now()
                                    .plusMonths(2)
                                    .atStartOfDay()
                                    .toTidspunkt(),
                            saksbehandler = null,
                            gruppeId = null,
                        )
                    }
            logger.info("opprettet ${unntakUtenTilOgMed.size} oppgaver for oppfølging av unntak i sak $sakId")
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke opprette oppfølgingsoppgave i ferdigstilling av aktivitetsplikt for " +
                    "sak $sakId. Stopper ikke ferdigstillingen av oppgave/behandling",
                e,
            )
        }
    }

    private fun harUnntakPaaDato(
        unntak: AktivitetspliktUnntak,
        dato: LocalDate,
    ) = (unntak.tom == null || unntak.tom.isAfter(dato)).also {
        if (it) {
            logger.info("Det er unntak for aktivitetsplikt, ingen revurdering opprettes for sak ${unntak.sakId}")
        } else {
            logger.info("Det er ikke unntak for aktivitetsplikt i perioden, revurdering skal opprettes for sak ${unntak.sakId}")
        }
    }

    fun harVarigUnntak(sakId: SakId): Boolean {
        val varigUnntak =
            hentVurderingForSak(sakId)
                .unntak
                .find { it.unntak == AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT }

        return varigUnntak != null
    }

    fun hentAktiviteterHendelser(
        behandlingId: UUID? = null,
        sakId: SakId? = null,
    ): AktivitetspliktAktivitet =
        (
            if (behandlingId != null) {
                val perioder = aktivitetspliktDao.hentAktiviteterForBehandling(behandlingId)
                val hendelser = aktivitetspliktDao.hentHendelserForBehandling(behandlingId)
                AktivitetspliktAktivitet(hendelser, perioder)
            } else if (sakId != null) {
                val perioder = aktivitetspliktDao.hentAktiviteterForSak(sakId)
                val hendelser = aktivitetspliktDao.hentHendelserForSak(sakId)
                AktivitetspliktAktivitet(hendelser, perioder)
            } else {
                throw ManglerSakEllerBehandlingIdException()
            }
        )

    fun hentAktiviteter(
        behandlingId: UUID? = null,
        sakId: SakId? = null,
    ): List<AktivitetspliktAktivitetPeriode> =
        (
            if (behandlingId != null) {
                aktivitetspliktDao.hentAktiviteterForBehandling(behandlingId)
            } else if (sakId != null) {
                aktivitetspliktDao.hentAktiviteterForSak(sakId)
            } else {
                throw ManglerSakEllerBehandlingIdException()
            }
        )

    fun upsertAktivitet(
        aktivitet: LagreAktivitetspliktAktivitet,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID? = null,
        sakId: SakId? = null,
    ) {
        if (aktivitet.tom != null && aktivitet.tom < aktivitet.fom) {
            throw TomErFoerFomException()
        }
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())

        if (behandlingId != null) {
            val behandling = hentBehandlingOgSjekkOmRedigerbar(behandlingId)
            if (aktivitet.sakId != behandling.sak.id) {
                throw SakidTilhoererIkkeBehandlingException()
            }
            if (aktivitet.id != null) {
                aktivitetspliktDao.oppdaterAktivitetForBehandling(behandlingId, aktivitet, kilde)
            } else {
                aktivitetspliktDao.opprettAktivitetForBehandling(behandlingId, aktivitet, kilde)
            }
            sendDtoTilStatistikk(aktivitet.sakId, behandlingId)
        } else if (sakId != null) {
            if (aktivitet.sakId != sakId) {
                throw SakidTilhoererIkkeBehandlingException()
            }

            if (aktivitet.id != null) {
                aktivitetspliktDao.oppdaterAktivitetForSak(sakId, aktivitet, kilde)
            } else {
                aktivitetspliktDao.opprettAktivitetForSak(sakId, aktivitet, kilde)
            }
        } else {
            throw ManglerSakEllerBehandlingIdException()
        }
    }

    fun slettAktivitet(
        aktivitetId: UUID,
        behandlingId: UUID? = null,
        sakId: SakId? = null,
    ) {
        if (behandlingId != null) {
            val behandling = hentBehandlingOgSjekkOmRedigerbar(behandlingId)
            aktivitetspliktDao.slettAktivitetForBehandling(aktivitetId, behandlingId)
            sendDtoTilStatistikk(behandling.sak.id, behandlingId)
        } else if (sakId != null) {
            aktivitetspliktDao.slettAktivitetForSak(aktivitetId, sakId)
        } else {
            throw ManglerSakEllerBehandlingIdException()
        }
    }

    fun hentHendelser(
        behandlingId: UUID? = null,
        sakId: SakId? = null,
    ): List<AktivitetspliktAktivitetHendelse> =
        (
            if (behandlingId != null) {
                aktivitetspliktDao.hentHendelserForBehandling(behandlingId)
            } else if (sakId != null) {
                aktivitetspliktDao.hentHendelserForSak(sakId)
            } else {
                throw ManglerSakEllerBehandlingIdException()
            }
        )

    fun upsertHendelse(
        hendelse: LagreAktivitetspliktHendelse,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID? = null,
        sakId: SakId? = null,
    ) {
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())

        if (behandlingId != null) {
            val behandling =
                requireNotNull(behandlingService.hentBehandling(behandlingId)) { "Fant ikke behandling $behandlingId" }
            if (!behandling.status.kanEndres()) {
                throw BehandlingKanIkkeEndres()
            }
            if (hendelse.sakId != behandling.sak.id) {
                throw SakidTilhoererIkkeBehandlingException()
            }
            aktivitetspliktDao.upsertHendelse(behandlingId, hendelse, kilde)

            sendDtoTilStatistikk(hendelse.sakId, behandlingId)
        } else if (sakId != null) {
            if (hendelse.sakId != sakId) {
                throw SakidTilhoererIkkeBehandlingException()
            }
            aktivitetspliktDao.upsertHendelse(null, hendelse, kilde)
        } else {
            throw ManglerSakEllerBehandlingIdException()
        }
    }

    fun slettHendelse(
        hendelseId: UUID,
        sakId: SakId,
    ) {
        val hendelseForSak =
            aktivitetspliktDao.hentHendelserForSak(sakId).firstOrNull { it.id == hendelseId }
                ?: throw GenerellIkkeFunnetException()

        if (hendelseForSak.behandlingId != null) {
            val behandling =
                requireNotNull(
                    behandlingService.hentBehandling(hendelseForSak.behandlingId),
                ) { "Fant ikke behandling ${hendelseForSak.behandlingId}" }
            if (!behandling.status.kanEndres()) {
                throw BehandlingKanIkkeEndres()
            }
            aktivitetspliktDao.slettHendelse(hendelseId)
            sendDtoTilStatistikk(behandling.sak.id, hendelseForSak.behandlingId)
        } else {
            aktivitetspliktDao.slettHendelse(hendelseId)
        }
    }

    fun upsertAktivitetsgradForOppgave(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        oppgaveId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): AktivitetspliktVurdering {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        sjekkOppgaveTilhoererSakOgErRedigerbar(oppgave, sakId)
        sjekkOmAktivitetsgradErGyldig(aktivitetsgrad)
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        aktivitetspliktAktivitetsgradDao.upsertAktivitetsgradForOppgaveEllerBehandling(
            aktivitetsgrad,
            sakId,
            kilde,
            oppgaveId = oppgaveId,
        )
        sendDtoTilStatistikk(sakId)

        val vurdering = hentVurderingForOppgave(oppgaveId)
        if (vurdering.erTom()) {
            throw InternfeilException(
                "Vi har ingen vurderinger men vi " +
                    "oppdaterte nettopp en vurdering i en oppgave. Gjelder oppgave med id=$oppgaveId i sak $sakId",
            )
        }
        return vurdering
    }

    private fun sjekkOmAktivitetsgradErGyldig(aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad) {
        if (!aktivitetsgrad.erGyldigUtfylt()) {
            throw UgyldigForespoerselException(
                "AKTIVITETSVURDERING_HAR_MANGLER",
                "Vurderingen av aktivitetsgrad kan ikke lagres, siden den er gjort for kravet fra 12 måneder " +
                    "men inneholder ikke vurdering av skjønn.",
            )
        }
    }

    fun upsertAktivtetsgradOgUnntakForOppgave(
        aktivitetsgradOgUnntak: AktivitetspliktAktivitetsgradOgUnntak,
        oppgaveId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): AktivitetspliktVurdering {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        sjekkOppgaveTilhoererSakOgErRedigerbar(oppgave, sakId)

        sjekkOmAktivitetsgradErGyldig(aktivitetsgradOgUnntak.aktivitetsgrad)
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())

        if (aktivitetsgradOgUnntak.unntak != null) {
            aktivitetspliktUnntakDao.upsertUnntak(
                unntak = aktivitetsgradOgUnntak.unntak,
                sakId = sakId,
                oppgaveId = oppgaveId,
                kilde = kilde,
            )
        }

        aktivitetspliktAktivitetsgradDao.upsertAktivitetsgradForOppgaveEllerBehandling(
            aktivitetsgradOgUnntak.aktivitetsgrad,
            sakId,
            kilde,
            oppgaveId = oppgaveId,
        )

        sendDtoTilStatistikk(sakId)
        return hentVurderingForOppgave(oppgaveId)
    }

    fun upsertAktivitetsgradOgUnntakForBehandling(
        aktivitetsgradOgUnntak: AktivitetspliktAktivitetsgradOgUnntak,
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): AktivitetspliktVurdering {
        hentBehandlingOgSjekkOmRedigerbar(behandlingId)
        sjekkOmAktivitetsgradErGyldig(aktivitetsgradOgUnntak.aktivitetsgrad)
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())

        if (aktivitetsgradOgUnntak.unntak != null) {
            if (aktivitetsgradOgUnntak.unntak.fom != null &&
                aktivitetsgradOgUnntak.unntak.tom != null &&
                aktivitetsgradOgUnntak.unntak.fom > aktivitetsgradOgUnntak.unntak.tom
            ) {
                throw TomErFoerFomException()
            }

            aktivitetspliktUnntakDao.upsertUnntak(
                unntak = aktivitetsgradOgUnntak.unntak,
                sakId = sakId,
                behandlingId = behandlingId,
                kilde = kilde,
            )
        }

        aktivitetspliktAktivitetsgradDao.upsertAktivitetsgradForOppgaveEllerBehandling(
            aktivitetsgradOgUnntak.aktivitetsgrad,
            sakId,
            kilde,
            behandlingId = behandlingId,
        )

        sendDtoTilStatistikk(sakId, behandlingId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId)
        val aktivitet = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId)

        return AktivitetspliktVurdering(aktivitet = aktivitet, unntak = unntak)
    }

    fun upsertUnntakForOppgave(
        unntak: LagreAktivitetspliktUnntak,
        oppgaveId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): AktivitetspliktVurdering {
        if (unntak.fom != null && unntak.tom != null && unntak.fom > unntak.tom) {
            throw TomErFoerFomException()
        }
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        sjekkOppgaveTilhoererSakOgErRedigerbar(oppgave, sakId)

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        aktivitetspliktUnntakDao.upsertUnntak(unntak, sakId, kilde, oppgaveId = oppgaveId)
        sendDtoTilStatistikk(
            sakId = sakId,
        )

        return hentVurderingForOppgave(oppgaveId)
    }

    fun slettAktivitetsgradForOppgave(
        oppgaveId: UUID,
        aktivitetsgradId: UUID,
        sakId: SakId,
    ): AktivitetspliktVurdering {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        sjekkOppgaveTilhoererSakOgErRedigerbar(oppgave, sakId)

        aktivitetspliktAktivitetsgradDao.slettAktivitetsgradForOppgave(aktivitetsgradId, oppgaveId)

        sendDtoTilStatistikk(sakId)
        return hentVurderingForOppgave(oppgaveId)
    }

    fun slettUnntakForOppgave(
        oppgaveId: UUID,
        unntakId: UUID,
        sakId: SakId,
    ): AktivitetspliktVurdering {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        sjekkOppgaveTilhoererSakOgErRedigerbar(oppgave, sakId)

        sendDtoTilStatistikk(sakId)
        aktivitetspliktUnntakDao.slettUnntakForOppgave(oppgaveId, unntakId)
        return hentVurderingForOppgave(oppgaveId)
    }

    fun hentVurderingForOppgave(oppgaveId: UUID): AktivitetspliktVurdering {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId)

        return AktivitetspliktVurdering(aktivitetsgrad, unntak)
    }

    fun hentVurderingForBehandlingNy(behandlingId: UUID): AktivitetspliktVurdering {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId)

        return AktivitetspliktVurdering(aktivitetsgrad, unntak)
    }

    fun upsertAktivitetsgradForBehandling(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): AktivitetspliktVurdering {
        hentBehandlingOgSjekkOmRedigerbar(behandlingId)
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        sjekkOmAktivitetsgradErGyldig(aktivitetsgrad)
        aktivitetspliktAktivitetsgradDao.upsertAktivitetsgradForOppgaveEllerBehandling(
            aktivitetsgrad,
            sakId,
            kilde,
            behandlingId = behandlingId,
        )

        sendDtoTilStatistikk(sakId, behandlingId = behandlingId)

        return hentVurderingForBehandlingNy(behandlingId)
    }

    fun slettAktivitetsgradForBehandling(
        behandlingId: UUID,
        aktivitetsgradId: UUID,
        sakId: SakId,
    ): AktivitetspliktVurdering {
        hentBehandlingOgSjekkOmRedigerbar(behandlingId)
        aktivitetspliktAktivitetsgradDao.slettAktivitetsgradForBehandling(aktivitetsgradId, behandlingId)

        sendDtoTilStatistikk(sakId, behandlingId = behandlingId)
        return hentVurderingForBehandlingNy(behandlingId)
    }

    fun slettUnntakForBehandling(
        behandlingId: UUID,
        unntakId: UUID,
        sakId: SakId,
    ): AktivitetspliktVurdering {
        hentBehandlingOgSjekkOmRedigerbar(behandlingId)

        sendDtoTilStatistikk(sakId, behandlingId = behandlingId)
        aktivitetspliktUnntakDao.slettUnntakForBehandling(unntakId = unntakId, behandlingId = behandlingId)
        return hentVurderingForBehandlingNy(behandlingId)
    }

    fun upsertUnntakForBehandling(
        unntak: LagreAktivitetspliktUnntak,
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): AktivitetspliktVurdering {
        if (unntak.fom != null && unntak.tom != null && unntak.fom > unntak.tom) {
            throw TomErFoerFomException()
        }
        hentBehandlingOgSjekkOmRedigerbar(behandlingId)

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        aktivitetspliktUnntakDao.upsertUnntak(unntak, sakId, kilde, behandlingId = behandlingId)
        sendDtoTilStatistikk(
            sakId = sakId,
        )

        return hentVurderingForBehandlingNy(behandlingId)
    }

    fun hentVurderingForSak(sakId: SakId): AktivitetspliktVurdering =
        hentVurderingForSakHelper(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao, sakId)

    private fun oppfyllerAktivitetsplikt12mnd(sakId: SakId): Boolean {
        if (harVarigUnntak(sakId)) {
            return true
        }
        val oppgave12mnd =
            oppgaveService
                .hentOppgaverForSak(sakId, OppgaveType.AKTIVITETSPLIKT_12MND)
                .filter { it.erFerdigstilt() }
                .maxByOrNull { it.opprettet }
        if (oppgave12mnd == null) {
            return false
        }

        val vurderingForOppgave = hentVurderingForOppgave(oppgave12mnd.id)
        val sistevurdering =
            vurderingForOppgave.aktivitet.maxByOrNull { it.fom }
        if (sistevurdering == null) {
            return false
        }
        if (!sistevurdering.vurdertFra12Mnd) {
            return false
        }

        if (sistevurdering.aktivitetsgrad == AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50) {
            return false
        }
        if (sistevurdering.aktivitetsgrad == AKTIVITET_OVER_50 &&
            sistevurdering.skjoennsmessigVurdering == AktivitetspliktSkjoennsmessigVurdering.NEI
        ) {
            return false
        }
        return true
    }

    fun opprettRevurderingHvisKravIkkeOppfylt(
        request: OpprettRevurderingForAktivitetspliktDto,
    ): OpprettRevurderingForAktivitetspliktResponse {
        val forrigeBehandling =
            krevIkkeNull(behandlingService.hentSisteIverksatte(request.sakId)) {
                "Fant ikke forrige behandling i sak ${request.sakId}sakId"
            }
        val persongalleri =
            krevIkkeNull(
                grunnlagService.hentPersongalleri(forrigeBehandling.id),
            ) {
                "Fant ikke persongalleri for behandling ${forrigeBehandling.id}"
            }

        val aktivitetspliktDato = request.behandlingsmaaned.atDay(1).plusMonths(1)
        val jobbType = request.jobbType
        // Hvis man oppfyller kravene skal det ikke opprettes en revurdering
        val oppfyllerKrav =
            when (jobbType) {
                JobbType.OMS_DOED_6MND -> oppfyllerAktivitetsplikt6mnd(request.sakId, aktivitetspliktDato)
                JobbType.OMS_DOED_12MND -> oppfyllerAktivitetsplikt12mnd(request.sakId)
                else -> throw InternfeilException("Oppretting av revurdering støttes ikke for jobb ${jobbType.name}")
            }
        return if (oppfyllerKrav) {
            OpprettRevurderingForAktivitetspliktResponse(forrigeBehandlingId = forrigeBehandling.id)
        } else {
            if (behandlingService.hentBehandlingerForSak(request.sakId).any { it.status.aapenBehandling() }) {
                opprettOppgaveForRevurdering(request, forrigeBehandling)
            } else {
                opprettRevurdering(request, forrigeBehandling, aktivitetspliktDato, persongalleri)
            }
        }
    }

    fun opprettOppgaveHvisVarigUnntak(request: OpprettOppgaveForAktivitetspliktDto): OpprettOppgaveForAktivitetspliktResponse =
        if (harVarigUnntak(request.sakId)) {
            opprettOppgaveForVarigUnntak(request)
        } else {
            OpprettOppgaveForAktivitetspliktResponse()
        }

    private fun hentBehandlingOgSjekkOmRedigerbar(behandlingId: UUID): Behandling {
        val behandling =
            behandlingService.hentBehandling(behandlingId) ?: throw BehandlingNotFoundException(behandlingId)
        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }
        return behandling
    }

    private fun opprettOppgaveForRevurdering(
        request: OpprettRevurderingForAktivitetspliktDto,
        forrigeBehandling: Behandling,
    ): OpprettRevurderingForAktivitetspliktResponse {
        logger.info("Oppretter oppgave for revurdering av aktivitetsplikt for sak ${request.sakId}")
        return oppgaveService
            .opprettOppgave(
                sakId = request.sakId,
                referanse = forrigeBehandling.id.toString(),
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.AKTIVITETSPLIKT_REVURDERING,
                merknad = request.jobbType.beskrivelse,
                frist = request.frist,
            ).let { oppgave ->
                OpprettRevurderingForAktivitetspliktResponse(
                    opprettetOppgave = true,
                    oppgaveId = oppgave.id,
                    forrigeBehandlingId = forrigeBehandling.id,
                )
            }
    }

    private fun opprettOppgaveForVarigUnntak(request: OpprettOppgaveForAktivitetspliktDto): OpprettOppgaveForAktivitetspliktResponse {
        logger.info("Oppretter oppgave for infobrev for varig unntak av aktivitetsplikt for sak ${request.sakId}")
        return oppgaveService
            .opprettOppgave(
                sakId = request.sakId,
                referanse = request.referanse ?: "",
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK,
                merknad = request.jobbType.beskrivelse,
                frist = request.frist,
            ).let { oppgave ->
                OpprettOppgaveForAktivitetspliktResponse(
                    opprettetOppgave = true,
                    oppgaveId = oppgave.id,
                )
            }
    }

    private fun opprettRevurdering(
        request: OpprettRevurderingForAktivitetspliktDto,
        forrigeBehandling: Behandling,
        aktivitetspliktDato: LocalDate?,
        persongalleri: Persongalleri,
    ): OpprettRevurderingForAktivitetspliktResponse {
        logger.info("Oppretter behandling for revurdering av aktivitetsplikt for sak ${request.sakId}")
        return revurderingService
            .opprettRevurdering(
                sakId = request.sakId,
                persongalleri = persongalleri,
                forrigeBehandling = forrigeBehandling,
                mottattDato = null,
                prosessType = Prosesstype.MANUELL,
                kilde = Vedtaksloesning.GJENNY,
                revurderingAarsak = Revurderingaarsak.AKTIVITETSPLIKT,
                virkningstidspunkt = aktivitetspliktDato?.tilVirkningstidspunkt("Aktivitetsplikt"),
                begrunnelse = request.jobbType.beskrivelse,
                saksbehandlerIdent = Fagsaksystem.EY.navn,
                frist = request.frist,
                opprinnelse = BehandlingOpprinnelse.AUTOMATISK_JOBB,
            ).oppdater()
            .let { revurdering ->
                revurderingService.fjernSaksbehandlerFraRevurderingsOppgave(revurdering)
                OpprettRevurderingForAktivitetspliktResponse(
                    opprettetRevurdering = true,
                    nyBehandlingId = revurdering.id,
                    forrigeBehandlingId = forrigeBehandling.id,
                )
            }
    }

    private fun sendDtoTilStatistikk(
        sakId: SakId,
        behandlingId: UUID? = null,
    ) {
        try {
            val dto = hentAktivitetspliktDto(sakId, behandlingId)
            statistikkKafkaProducer.sendMeldingOmAktivitetsplikt(dto)
        } catch (e: ManglerDoedsdatoUnderBehandlingException) {
            // Dette er ikke kritisk og vi vil bare logge en advarsel
            logger.warn(e.detail, e)
        } catch (e: ManglerBehandlingIdEllerIverksattBehandlingException) {
            // Dette er heller ikke kritisk og vi vil bare logge en advarsel
            logger.warn(e.detail, e)
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke sende hendelse til statistikk om oppdatert aktivitetsplikt, for sak $sakId. " +
                    "Dette betyr at vi kan mangle oppdatert informasjon om aktivitetsplikten i saken for bruker, og " +
                    "bør sees på / vurdere en ekstra sending for akkurat denne saken.",
                e,
            )
        }
    }

    /**
     * Kopierer inn siste vurdering av aktivitet i saken inn i oppgaven som er forespurt,
     * hvis oppgaven er en aktivitetspliktoppgave og den ikke har noen vurderingenr enda
     * og den ikke er avsluttet.
     */
    fun kopierInnTilOppgave(
        sakId: SakId,
        oppgaveId: UUID,
    ): AktivitetspliktVurdering {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        sjekkOppgaveTilhoererSakOgErRedigerbar(oppgave, sakId)

        when (oppgave.type) {
            OppgaveType.AKTIVITETSPLIKT,
            OppgaveType.AKTIVITETSPLIKT_12MND,
            -> {
                logger.info("Kopierer inn vurdering i sak $sakId til oppgave med id $oppgaveId")
            }

            else -> throw UgyldigForespoerselException(
                "OPPGAVE_HAR_FEIL_TYPE",
                "Kan ikke kopiere inn vurderingen av aktivitetsplikt til en oppgave som ikke går på aktivitetsplikt!",
            )
        }

        aktivitetspliktKopierService.kopierVurderingTilOppgave(sakId, oppgaveId)
        return hentVurderingForOppgave(oppgaveId)
    }

    private fun sjekkOppgaveTilhoererSakOgErRedigerbar(
        oppgave: OppgaveIntern,
        sakId: SakId,
    ) {
        if (oppgave.sakId != sakId) {
            throw OppgaveTilhoererIkkeSakException(sakId, oppgave.id)
        }

        if (oppgave.erAvsluttet()) {
            throw UgyldigForespoerselException(
                "OPPGAVE_ER_AVSLUTTET",
                "Kan ikke endre på unntak / vurderinger i en oppgave som er avsluttet",
            )
        }
    }

    fun opprettOppgaveHvisIkkeVarigUnntak(dto: OpprettOppgaveForAktivitetspliktDto): OpprettOppgaveForAktivitetspliktResponse {
        val sakId = dto.sakId
        if (harVarigUnntak(sakId)) {
            return OpprettOppgaveForAktivitetspliktResponse(
                opprettetOppgave = false,
            )
        }
        logger.info("Sakid $sakId har ikke varig unntak, oppretter oppgave for aktivitetsplikt infobrev")
        val oppgaveType =
            when (dto.jobbType) {
                JobbType.OMS_DOED_4MND -> OppgaveType.AKTIVITETSPLIKT
                JobbType.OMS_DOED_10MND -> OppgaveType.AKTIVITETSPLIKT_12MND
                else -> throw UgyldigForespoerselException(
                    "FEIL_JOBBTYPE",
                    "Kan ikke opprette en aktivitetspliktoppgave for jobbtype=${dto.jobbType} i sak $sakId",
                )
            }

        val kanOpprette =
            when (oppgaveType) {
                OppgaveType.AKTIVITETSPLIKT -> validerMnd6KanOpprette(sakId)
                OppgaveType.AKTIVITETSPLIKT_12MND -> valider12MndKanOpprette(sakId)
                else -> throw UgyldigForespoerselException("FEIL_OPPGAVETYPE", "Kan ikke håndtere oppgavetype $oppgaveType i sak $sakId")
            }
        if (!kanOpprette) {
            logger.info("Oppretter ikke oppgavetype $oppgaveType i sak $sakId da kravet for 6/12 mnd ikke oppfylles.")
            return OpprettOppgaveForAktivitetspliktResponse(
                opprettetOppgave = false,
            )
        }
        if (validerFinnesIkkeAllerede(sakId, oppgaveType)) {
            logger.info(
                "Oppretter ikke oppgavetype $oppgaveType i sak $sakId da den finnes allerede som under behandling eller som ferdigdstilt",
            )
            return OpprettOppgaveForAktivitetspliktResponse(
                opprettetOppgave = false,
            )
        }

        val opprettetOppgave =
            oppgaveService.opprettOppgave(
                referanse = dto.referanse ?: "",
                sakId = sakId,
                kilde = OppgaveKilde.HENDELSE,
                type = oppgaveType,
                merknad = dto.jobbType.beskrivelse,
                frist = dto.frist,
            )
        return OpprettOppgaveForAktivitetspliktResponse(
            opprettetOppgave = true,
            oppgaveId = opprettetOppgave.id,
        )
    }

    private fun validerFinnesIkkeAllerede(
        sakId: SakId,
        oppgaveType: OppgaveType,
    ): Boolean =
        oppgaveService
            .hentOppgaverForSak(
                sakId,
                oppgaveType,
            ).any {
                it.erIkkeAvsluttet() ||
                    it.erFerdigstilt()
            }

    private fun validerMnd6KanOpprette(sakId: SakId): Boolean =
        oppgaveService
            .hentOppgaverForSak(
                sakId,
                OppgaveType.AKTIVITETSPLIKT_12MND,
            ).none { it.status != Status.AVBRUTT }

    private fun valider12MndKanOpprette(sakId: SakId): Boolean {
        val oppfoelging6mnd =
            oppgaveService.hentOppgaverForSak(
                sakId,
                OppgaveType.AKTIVITETSPLIKT,
            )
        if (oppfoelging6mnd.any { it.erIkkeAvsluttet() }) {
            return false
        }
        val ferdigstilt6mndOppgave = oppfoelging6mnd.filter { it.erFerdigstilt() }
        return ferdigstilt6mndOppgave.isNotEmpty()
    }
}

class OppgaveTilhoererIkkeSakException(
    sakId: SakId,
    oppgaveId: UUID,
) : UgyldigForespoerselException(
        code = "OPPGAVE_TILHOERER_IKKE_SAK",
        detail = "Oppgave med id=$oppgaveId tilhører ikke sak med id=$sakId",
    )

class ManglerDoedsdatoUnderBehandlingException(
    sakId: SakId,
) : UgyldigForespoerselException(
        "MANGLER_DOEDSDATO_SAK",
        "Mangler dødsdato avdød i sak $sakId. Dette er en sak under behandling, så statistikk skal " +
            "plukke opp aktiviteten i vedtaket hvis det blir innvilgelse.",
    )

/**
 * Henter det nyeste bildet på hva som er vurderingen av aktivitetsgrad og unntak fra aktivitet på sak.
 *
 * Grunnen til at man må hente ut både unntak og aktivitetgrad fra samme kilde, og gjøre sammenstilling for å
 * sikre at det er samme kilde kan mse på følgende scenario:
 *
 * Man har følgende samling av vurderinger for det som er “riktig” for saken:
 * |-------- Under 50 % ----|------ over 50 % ----|----- Unntak midlertidig sykdom --->
 *
 * Der unntaket er lagt inn nylig. La oss si at det ikke stemmer at det er et unntak allikevel i saken,
 * og man korrigerer i en behandling:
 * |-------- Under 50 % ----|------ over 50 % ------->
 *
 * Hvis vi nå skal se hva som er “riktig” i saken må man hente ut begge deler, fordi hvis vi henter ut
 * siste vurdering og siste unntak får man med seg det slettede unntaket.
 */
fun hentVurderingForSakHelper(
    aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao,
    aktivitetspliktUnntakDao: AktivitetspliktUnntakDao,
    sakId: SakId,
): AktivitetspliktVurdering {
    val aktivitet = aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId)
    val unntak = aktivitetspliktUnntakDao.hentNyesteUnntak(sakId)

    val idAktivitet = setOf(aktivitet.map { it.behandlingId to it.oppgaveId })
    val idUnntak = setOf(unntak.map { it.behandlingId to it.oppgaveId })
    if (aktivitet.isNotEmpty() && unntak.isNotEmpty() && idAktivitet != idUnntak) {
        // Vi har hentet både fra vurdering og unntak, men vi har hentet fra forskjellige oppgaver / behandlinger.
        // For å hente riktig i dette tilfellet må vi finne hvilken som er nyest, og bruke den id'en til å hente
        // den andre
        val nyesteEndringAktivitet = aktivitet.maxOf { it.endret.endretDatoOrNull() ?: Tidspunkt.MIN }
        val nyesteEndringUnntak = unntak.maxOf { it.endret?.endretDatoOrNull() ?: Tidspunkt.MIN }

        if (nyesteEndringUnntak > nyesteEndringAktivitet) {
            val foersteUnntak = unntak.first()
            if (foersteUnntak.behandlingId != null) {
                val aktivitetForBehandling =
                    aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(foersteUnntak.behandlingId)
                return AktivitetspliktVurdering(aktivitetForBehandling, unntak)
            } else {
                val oppgaveId =
                    krevIkkeNull(foersteUnntak.oppgaveId) {
                        "Har et unntak med id=${foersteUnntak.id} i sak=${foersteUnntak.sakId} som ikke " +
                            "er koblet på hverken sak eller oppgave."
                    }
                val aktivitetForOppgave = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId)
                return AktivitetspliktVurdering(aktivitetForOppgave, unntak)
            }
        } else {
            val foersteVurdering = aktivitet.first()
            if (foersteVurdering.behandlingId != null) {
                val unntakForBehandling =
                    aktivitetspliktUnntakDao.hentUnntakForBehandling(foersteVurdering.behandlingId)
                return AktivitetspliktVurdering(aktivitet, unntakForBehandling)
            } else {
                val oppgaveId =
                    krevIkkeNull(foersteVurdering.oppgaveId) {
                        "Har en vurdering med id=${foersteVurdering.id} i sak=${foersteVurdering.sakId} som ikke " +
                            "er koblet på hverken sak eller oppgave."
                    }
                val unntakForOppgave = aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId)
                return AktivitetspliktVurdering(aktivitet, unntakForOppgave)
            }
        }
    }

    return AktivitetspliktVurdering(aktivitet, unntak)
}

data class AktivitetspliktVurdering(
    val aktivitet: List<AktivitetspliktAktivitetsgrad>,
    val unntak: List<AktivitetspliktUnntak>,
) {
    fun erTom() = aktivitet.isEmpty() && unntak.isEmpty()
}

fun Grunnlagsopplysning.Kilde.endretDatoOrNull(): Tidspunkt? = if (this is Grunnlagsopplysning.Saksbehandler) this.tidspunkt else null

interface AktivitetspliktVurderingOpprettetDato {
    val opprettet: Grunnlagsopplysning.Kilde
}

class SakidTilhoererIkkeBehandlingException :
    UgyldigForespoerselException(
        code = "SAK_ID_TILHOERER_IKKE_BEHANDLING",
        detail = "Sak id stemmer ikke over ens med behandling",
    )

class TomErFoerFomException :
    UgyldigForespoerselException(
        code = "TOM_ER_FOER_FOM",
        detail = "Til og med dato er kan ikke være før fra og med dato",
    )

class ManglerSakEllerBehandlingIdException :
    UgyldigForespoerselException(
        code = "MANGLER_SAK_ELLER_BEHANDLING_ID",
        detail = "Forespørsel mangler sak eller behandling id",
    )

class BehandlingNotFoundException(
    behandlingId: UUID,
) : IkkeFunnetException(
        code = "FANT_IKKE_BEHANDLING",
        detail = "Kunne ikke finne ønsket behandling, id: $behandlingId",
    )

class ManglerBehandlingIdEllerIverksattBehandlingException(
    sakId: SakId,
) : InternfeilException(
        "Fant ikke behandlingId eller iverksatt behandling for sakId=$sakId, " +
            "for uthenting av aktivitetspliktDto til statistikk",
    )
