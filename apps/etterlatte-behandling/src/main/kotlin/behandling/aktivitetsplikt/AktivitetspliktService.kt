package no.nav.etterlatte.behandling.aktivitetsplikt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_100
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.BehandlingKanIkkeEndres
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktResponse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class AktivitetspliktService(
    private val aktivitetspliktDao: AktivitetspliktDao,
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao,
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao,
    private val behandlingService: BehandlingService,
    private val grunnlagKlient: GrunnlagKlient,
    private val automatiskRevurderingService: AutomatiskRevurderingService,
    private val statistikkKafkaProducer: BehandlingHendelserKafkaProducer,
    private val oppgaveService: OppgaveService,
) {
    fun hentAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? =
        aktivitetspliktDao.finnSenesteAktivitetspliktOppfolging(behandlingId)

    fun lagreAktivitetspliktOppfolging(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String,
    ): AktivitetspliktOppfolging {
        aktivitetspliktDao.lagre(behandlingId, nyOppfolging, navIdent)
        return hentAktivitetspliktOppfolging(behandlingId)!!
    }

    suspend fun hentAktivitetspliktDto(
        sakId: Long,
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
    ): AktivitetspliktDto {
        val grunnlag = grunnlagKlient.hentGrunnlagForBehandling(behandlingId, bruker)
        val avdoedDoedsdato =
            requireNotNull(
                grunnlag
                    .hentAvdoede()
                    .singleOrNull()
                    ?.hentDoedsdato()
                    ?.verdi,
            ) {
                "Kunne ikke hente ut avdødes dødsdato for behandling med id=$behandlingId"
            }

        val sisteBehandling = behandlingService.hentSisteIverksatte(sakId)
        val aktiviteter = sisteBehandling?.id?.let { hentAktiviteter(it) } ?: emptyList()

        val nyesteAktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId)
        val nyesteUnntak = aktivitetspliktUnntakDao.hentNyesteUnntak(sakId)
        val sisteVurdering =
            listOfNotNull(nyesteUnntak, nyesteAktivitetsgrad).sortedBy { it.opprettet.endretDatoOrNull() }.lastOrNull()

        val (unntak, aktivitetsgrad) =
            when (sisteVurdering) {
                is AktivitetspliktAktivitetsgrad -> emptyList<AktivitetspliktUnntak>() to listOf(sisteVurdering)
                is AktivitetspliktUnntak -> listOf(sisteVurdering) to emptyList()
                else -> emptyList<AktivitetspliktUnntak>() to emptyList()
            }

        return AktivitetspliktDto(
            sakId = sakId,
            avdoedDoedsmaaned = YearMonth.from(avdoedDoedsdato),
            aktivitetsgrad = aktivitetsgrad.map { it.toDto() },
            unntak = unntak.map { it.toDto() },
            brukersAktivitet = aktiviteter.map { it.toDto() },
        )
    }

    fun oppfyllerAktivitetsplikt(
        sakId: Long,
        aktivitetspliktDato: LocalDate,
    ): Boolean {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId)
        val unntak = aktivitetspliktUnntakDao.hentNyesteUnntak(sakId)
        val nyesteVurdering =
            listOfNotNull(aktivitetsgrad, unntak).sortedBy { it.opprettet.endretDatoOrNull() }.lastOrNull()

        return when (nyesteVurdering) {
            is AktivitetspliktAktivitetsgrad -> oppfyllerAktivitet(nyesteVurdering)
            is AktivitetspliktUnntak -> harUnntakPaaDato(nyesteVurdering, aktivitetspliktDato)
            else -> {
                logger.info("Det er ikke gjort en vurdering av bruker på over 50% aktivitet, og finner ingen unntak for sak $sakId")
                false
            }
        }
    }

    private fun oppfyllerAktivitet(aktivitetsgrad: AktivitetspliktAktivitetsgrad) =
        (aktivitetsgrad.aktivitetsgrad in listOf(AKTIVITET_OVER_50, AKTIVITET_100)).also {
            if (it) {
                logger.info("Aktivitetsgrad er over 50% eller 100%, ingen revurdering opprettes for sak ${aktivitetsgrad.sakId}")
            } else {
                logger.info("Aktivitetsgrad er under 50%, revurdering skal opprettes for sak ${aktivitetsgrad.sakId}")
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

    fun hentAktiviteter(behandlingId: UUID) = aktivitetspliktDao.hentAktiviteter(behandlingId)

    fun upsertAktivitet(
        behandlingId: UUID,
        aktivitet: LagreAktivitetspliktAktivitet,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            requireNotNull(behandlingService.hentBehandling(behandlingId)) { "Fant ikke behandling $behandlingId" }

        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        if (aktivitet.sakId != behandling.sak.id) {
            throw SakidTilhoererIkkeBehandlingException()
        }

        if (aktivitet.tom != null && aktivitet.tom < aktivitet.fom) {
            throw TomErFoerFomException()
        }

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        if (aktivitet.id != null) {
            aktivitetspliktDao.oppdaterAktivitet(behandlingId, aktivitet, kilde)
        } else {
            aktivitetspliktDao.opprettAktivitet(behandlingId, aktivitet, kilde)
        }
        runBlocking { sendDtoTilStatistikk(aktivitet.sakId, brukerTokenInfo, behandlingId) }
    }

    fun slettAktivitet(
        behandlingId: UUID,
        aktivitetId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            requireNotNull(behandlingService.hentBehandling(behandlingId)) { "Fant ikke behandling $behandlingId" }

        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }
        aktivitetspliktDao.slettAktivitet(aktivitetId, behandlingId)
        runBlocking { sendDtoTilStatistikk(behandling.sak.id, brukerTokenInfo, behandlingId) }
    }

    fun opprettAktivitetsgradForOppgave(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        oppgaveId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        require(
            aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) == null,
        ) { "Aktivitetsgrad finnes allerede for oppgave $oppgaveId" }
        aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(aktivitetsgrad, sakId, kilde, oppgaveId)
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        runBlocking { sendDtoTilStatistikk(sakId, brukerTokenInfo, behandlingId = UUID.fromString(oppgave.referanse)) }
    }

    fun upsertAktivitetsgradForBehandling(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            requireNotNull(behandlingService.hentBehandling(behandlingId)) { "Fant ikke behandling $behandlingId" }

        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())

        if (aktivitetsgrad.id != null) {
            aktivitetspliktAktivitetsgradDao.oppdaterAktivitetsgrad(aktivitetsgrad, kilde, behandlingId)
        } else {
            require(
                aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) == null,
            ) { "Aktivitetsgrad finnes allerede for behandling $behandlingId" }
            val unntak = aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId)
            if (unntak != null) {
                aktivitetspliktUnntakDao.slettUnntak(unntak.id, behandlingId)
            }
            aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(
                aktivitetsgrad,
                sakId,
                kilde,
                behandlingId = behandlingId,
            )
        }

        runBlocking { sendDtoTilStatistikk(sakId, brukerTokenInfo, behandlingId) }
    }

    fun opprettUnntakForOpppgave(
        unntak: LagreAktivitetspliktUnntak,
        oppgaveId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (unntak.fom != null && unntak.tom != null && unntak.fom > unntak.tom) {
            throw TomErFoerFomException()
        }

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        require(
            aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) == null,
        ) { "Unntak finnes allerede for oppgave $oppgaveId" }
        aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, kilde, oppgaveId)
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        runBlocking {
            sendDtoTilStatistikk(
                sakId = sakId,
                brukerTokenInfo = brukerTokenInfo,
                behandlingId = UUID.fromString(oppgave.referanse),
            )
        }
    }

    fun upsertUnntakForBehandling(
        unntak: LagreAktivitetspliktUnntak,
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            requireNotNull(behandlingService.hentBehandling(behandlingId)) { "Fant ikke behandling $behandlingId" }

        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        if (unntak.fom != null && unntak.tom != null && unntak.fom > unntak.tom) {
            throw TomErFoerFomException()
        }

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())

        if (unntak.id != null) {
            aktivitetspliktUnntakDao.oppdaterUnntak(unntak, kilde, behandlingId)
        } else {
            require(
                aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) == null,
            ) { "Unntak finnes allerede for behandling $behandlingId" }

            val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId)
            if (aktivitetsgrad != null) {
                aktivitetspliktAktivitetsgradDao.slettAktivitetsgrad(aktivitetsgrad.id, behandlingId)
            }
            aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, kilde, behandlingId = behandlingId)
        }

        runBlocking { sendDtoTilStatistikk(sakId, brukerTokenInfo, behandlingId) }
    }

    private suspend fun sendDtoTilStatistikk(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
    ) {
        try {
            val dto = hentAktivitetspliktDto(sakId, brukerTokenInfo, behandlingId)
            statistikkKafkaProducer.sendMeldingOmAktivitetsplikt(dto)
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke sende hendelse til statistikk om oppdatert aktivitetsplikt, for sak $sakId. " +
                    "Dette betyr at vi kan mangle oppdatert informasjon om aktivitetsplikten i saken for bruker, og " +
                    "bør sees på / vurdere en ekstra sending for akkurat denne saken.",
                e,
            )
        }
    }

    fun hentVurderingForOppgave(oppgaveId: UUID): AktivitetspliktVurdering? {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId)

        if (aktivitetsgrad == null && unntak == null) {
            return null
        }

        return AktivitetspliktVurdering(aktivitetsgrad, unntak)
    }

    fun hentVurderingForBehandling(behandlingId: UUID): AktivitetspliktVurdering? {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId)

        if (aktivitetsgrad == null && unntak == null) {
            return null
        }

        return AktivitetspliktVurdering(aktivitetsgrad, unntak)
    }

    fun opprettRevurderingHvisKravIkkeOppfylt(
        request: OpprettRevurderingForAktivitetspliktDto,
        bruker: BrukerTokenInfo,
    ): OpprettRevurderingForAktivitetspliktResponse {
        val forrigeBehandling =
            requireNotNull(behandlingService.hentSisteIverksatte(request.sakId)) {
                "Fant ikke forrige behandling i sak ${request.sakId}sakId"
            }
        val persongalleri =
            runBlocking {
                requireNotNull(
                    grunnlagKlient
                        .hentPersongalleri(
                            forrigeBehandling.id,
                            bruker,
                        )?.opplysning,
                ) {
                    "Fant ikke persongalleri for behandling ${forrigeBehandling.id}"
                }
            }

        val aktivitetspliktDato = request.behandlingsmaaned.atDay(1).plusMonths(1)
        return if (oppfyllerAktivitetsplikt(request.sakId, aktivitetspliktDato)) {
            OpprettRevurderingForAktivitetspliktResponse(forrigeBehandlingId = forrigeBehandling.id)
        } else {
            if (behandlingService.hentBehandlingerForSak(request.sakId).any { it.status.aapenBehandling() }) {
                opprettOppgave(request, forrigeBehandling)
            } else {
                opprettRevurdering(request, forrigeBehandling, aktivitetspliktDato, persongalleri)
            }
        }
    }

    private fun opprettOppgave(
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

    private fun opprettRevurdering(
        request: OpprettRevurderingForAktivitetspliktDto,
        forrigeBehandling: Behandling,
        aktivitetspliktDato: LocalDate?,
        persongalleri: Persongalleri,
    ): OpprettRevurderingForAktivitetspliktResponse {
        logger.info("Oppretter behandling for revurdering av aktivitetsplikt for sak ${request.sakId}")
        return automatiskRevurderingService
            .opprettAutomatiskRevurdering(
                sakId = request.sakId,
                forrigeBehandling = forrigeBehandling,
                revurderingAarsak = Revurderingaarsak.AKTIVITETSPLIKT,
                virkningstidspunkt = aktivitetspliktDato,
                kilde = Vedtaksloesning.GJENNY,
                persongalleri = persongalleri,
                frist = request.frist,
                begrunnelse = request.jobbType.beskrivelse,
            ).oppdater()
            .let { revurdering ->
                fjernSaksbehandlerFraRevurderingsOppgave(revurdering)
                OpprettRevurderingForAktivitetspliktResponse(
                    opprettetRevurdering = true,
                    nyBehandlingId = revurdering.id,
                    forrigeBehandlingId = forrigeBehandling.id,
                )
            }
    }

    private fun fjernSaksbehandlerFraRevurderingsOppgave(revurdering: Revurdering) {
        val revurderingsOppgave =
            oppgaveService
                .hentOppgaverForReferanse(revurdering.id.toString())
                .find { it.type == OppgaveType.REVURDERING }

        if (revurderingsOppgave != null) {
            oppgaveService.fjernSaksbehandler(revurderingsOppgave.id)
        } else {
            logger.warn("Fant ikke oppgave for revurdering av aktivitetsplikt for sak ${revurdering.sak.id}")
        }
    }
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

data class AktivitetspliktVurdering(
    val aktivitet: AktivitetspliktAktivitetsgrad?,
    val unntak: AktivitetspliktUnntak?,
)

interface AktivitetspliktVurderingOpprettetDato {
    val opprettet: Grunnlagsopplysning.Kilde
}

fun Grunnlagsopplysning.Kilde.endretDatoOrNull(): Tidspunkt? = if (this is Grunnlagsopplysning.Saksbehandler) this.tidspunkt else null
