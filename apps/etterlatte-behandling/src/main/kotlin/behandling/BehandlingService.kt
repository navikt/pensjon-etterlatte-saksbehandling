package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.hentUtlandstilknytning
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.behandling.domain.toDetaljertBehandlingWithPersongalleri
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.tidligsteIverksatteVirkningstidspunkt
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RedigertFamilieforhold
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.PersonManglerSak
import no.nav.etterlatte.sak.SakMedUtlandstilknytning
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BehandlingFinnesIkkeException(
    message: String,
) : Exception(message)

class KravdatoMaaFinnesHvisBosattutland(
    message: String,
) : UgyldigForespoerselException(code = "BOSATTUTLAND_MAA_HA_KRAVDATO", detail = message)

class VirkningstidspunktMaaHaUtenlandstilknytning(
    message: String,
) : UgyldigForespoerselException(code = "VIRK_MAA_HA_UTENLANDSTILKNYTNING", detail = message)

class VirkningstidspunktKanIkkeVaereEtterOpphoer :
    UgyldigForespoerselException(
        code = "VIRK_KAN_IKKE_VAERE_ETTER_OPPHOER",
        detail = "Virkningstidspunkt kan ikke være etter opphør",
    )

class BehandlingNotFoundException(
    behandlingId: UUID,
) : IkkeFunnetException(
        code = "FANT_IKKE_BEHANDLING",
        detail = "Kunne ikke finne ønsket behandling, id: $behandlingId",
    )

class BehandlingKanIkkeAvbrytesException(
    behandlingStatus: BehandlingStatus,
) : UgyldigForespoerselException(
        code = "BEHANDLING_KAN_IKKE_AVBRYTES",
        detail = "Behandlingen kan ikke avbrytes, status: $behandlingStatus",
    )

class PersongalleriFinnesIkkeException :
    IkkeFunnetException(
        code = "FANT_IKKE_PERSONGALLERI",
        detail = "Kunne ikke finne persongalleri",
    )

class KanIkkeEndreSendeBrevForFoerstegangsbehandling :
    UgyldigForespoerselException(
        "KAN_IKKE_ENDRE_SEND_BREV",
        "Kan ikke endre send brev for førstegangsbehandling, skal alltid sendes",
    )

class KanIkkeOppretteRevurderingUtenIverksattFoerstegangsbehandling :
    UgyldigForespoerselException(
        "KAN_IKKE_OPPRETTE_REVURDERING_MANGLER_FOERSTEGANGSBEHANDLING_IVERKSATT",
        "Kan ikke opprette revurdering når man mangler føstegangsbehandling med virkningstidspunkt",
    )

interface BehandlingService {
    fun hentBehandling(behandlingId: UUID): Behandling?

    fun hentBehandlingerForSak(sakId: Long): List<Behandling>

    fun hentSakMedBehandlinger(saker: List<Sak>): SakMedBehandlinger

    fun hentSisteIverksatte(sakId: Long): Behandling?

    fun avbrytBehandling(
        behandlingId: UUID,
        saksbehandler: BrukerTokenInfo,
    )

    fun registrerBehandlingHendelse(
        behandling: Behandling,
        saksbehandler: String,
    )

    fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
        hendelseType: HendelseType,
    )

    suspend fun oppdaterVirkningstidspunkt(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
        begrunnelse: String,
        kravdato: LocalDate? = null,
    ): Virkningstidspunkt

    fun oppdaterUtlandstilknytning(
        behandlingId: UUID,
        utlandstilknytning: Utlandstilknytning,
    )

    fun oppdaterViderefoertOpphoer(
        behandlingId: UUID,
        viderefoertOpphoer: ViderefoertOpphoer,
    )

    fun oppdaterBoddEllerArbeidetUtlandet(
        behandlingId: UUID,
        boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet,
    )

    suspend fun hentDetaljertBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling?

    suspend fun hentStatistikkBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): StatistikkBehandling?

    suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandlingDto

    suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        request: VirkningstidspunktRequest,
    ): Boolean

    fun hentFoersteVirk(sakId: Long): YearMonth?

    fun oppdaterGrunnlagOgStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun endrePersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        redigertFamilieforhold: RedigertFamilieforhold,
    )

    fun endreSkalSendeBrev(
        behandlingId: UUID,
        skalSendeBrev: Boolean,
    )

    fun hentUtlandstilknytningForSak(sakId: Long): Utlandstilknytning?

    fun lagreOpphoerFom(
        behandlingId: UUID,
        opphoerFraOgMed: YearMonth,
    )

    fun hentAapenRegulering(sakId: Long): UUID?
}

internal class BehandlingServiceImpl(
    private val behandlingDao: BehandlingDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingRequestLogger: BehandlingRequestLogger,
    private val kommerBarnetTilGodeDao: KommerBarnetTilGodeDao,
    private val oppgaveService: OppgaveService,
    private val grunnlagService: GrunnlagServiceImpl,
    private val beregningKlient: BeregningKlient,
) : BehandlingService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun hentBehandlingForId(id: UUID) =
        behandlingDao.hentBehandling(id)?.let { behandling ->
            listOf(behandling).filterForEnheter().firstOrNull()
        }

    private fun hentBehandlingerForSakId(sakId: Long) = behandlingDao.hentBehandlingerForSak(sakId).filterForEnheter()

    override fun hentBehandling(behandlingId: UUID): Behandling? = hentBehandlingForId(behandlingId)

    override fun hentBehandlingerForSak(sakId: Long): List<Behandling> = hentBehandlingerForSakId(sakId)

    /**
     * Funksjon for uthenting av [SakMedUtlandstilknytning] og tilknyttede [Behandling]er.
     *
     * Dersom det finnes flere saker vil det bli gjort en "prioritert gjetning" på hvilken som er gjeldende.
     * Gjøres som en midlertidig løsning inntil vi får avklart hvordan vi skal håndtere tilfeller hvor det
     * blir feilaktig opprettet flere saker på én og samme bruker.
     **/
    override fun hentSakMedBehandlinger(saker: List<Sak>): SakMedBehandlinger {
        if (saker.isEmpty()) throw PersonManglerSak()

        val sakerMedBehandlinger =
            saker.map { sak ->
                val behandlinger = hentBehandlingerForSak(sak.id)

                val utlandstilknytning = behandlinger.hentUtlandstilknytning()
                val sakMedUtlandstilknytning = SakMedUtlandstilknytning.fra(sak, utlandstilknytning)

                behandlinger
                    .map { it.toBehandlingSammendrag() }
                    .let { SakMedBehandlinger(sakMedUtlandstilknytning, it) }
            }

        return if (sakerMedBehandlinger.size > 1) {
            sakerMedBehandlinger.maxByOrNull {
                it.behandlinger
                    .count { behandling -> behandling.status != BehandlingStatus.AVBRUTT }
            }!!
        } else {
            sakerMedBehandlinger.single()
        }
    }

    override fun hentSisteIverksatte(sakId: Long): Behandling? =
        hentBehandlingerForSakId(sakId)
            .filter { BehandlingStatus.iverksattEllerAttestert().contains(it.status) }
            .maxByOrNull { it.behandlingOpprettet }

    override fun avbrytBehandling(
        behandlingId: UUID,
        saksbehandler: BrukerTokenInfo,
    ) {
        val behandling =
            hentBehandlingForId(behandlingId)
                ?: throw BehandlingNotFoundException(behandlingId)
        if (!behandling.status.kanAvbrytes()) {
            throw BehandlingKanIkkeAvbrytesException(behandling.status)
        }

        behandlingDao.avbrytBehandling(behandlingId).also {
            val hendelserKnyttetTilBehandling =
                grunnlagsendringshendelseDao.hentGrunnlagsendringshendelseSomErTattMedIBehandling(behandlingId)
            oppgaveService.avbrytOppgaveUnderBehandling(behandlingId.toString(), saksbehandler)

            hendelserKnyttetTilBehandling.forEach { hendelse ->
                oppgaveService.opprettOppgave(
                    referanse = hendelse.id.toString(),
                    sakId = behandling.sak.id,
                    kilde = OppgaveKilde.HENDELSE,
                    type = OppgaveType.VURDER_KONSEKVENS,
                    merknad = hendelse.beskrivelse(),
                )
            }

            if (behandling is Revurdering && behandling.revurderingsaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE) {
                val omgjoeringsoppgaveForKlage =
                    oppgaveService
                        .hentOppgaverForSak(behandling.sak.id)
                        .find { it.type == OppgaveType.OMGJOERING && it.referanse == behandling.relatertBehandlingId }
                        ?: throw InternfeilException(
                            "Kunne ikke finne en omgjøringsoppgave i sak=${behandling.sak.id}, " +
                                "så vi får ikke gjenopprettet omgjøringen hvis denne behandlingen avbrytes!",
                        )
                oppgaveService.opprettOppgave(
                    referanse = omgjoeringsoppgaveForKlage.referanse,
                    sakId = omgjoeringsoppgaveForKlage.sakId,
                    kilde = omgjoeringsoppgaveForKlage.kilde,
                    type = omgjoeringsoppgaveForKlage.type,
                    merknad = omgjoeringsoppgaveForKlage.merknad,
                    frist = omgjoeringsoppgaveForKlage.frist,
                )
            }

            hendelseDao.behandlingAvbrutt(behandling, saksbehandler.ident())
            grunnlagsendringshendelseDao.kobleGrunnlagsendringshendelserFraBehandlingId(behandlingId)
        }

        val persongalleri =
            runBlocking { grunnlagKlient.hentPersongalleri(behandlingId, saksbehandler) }

        behandlingHendelser.sendMeldingForHendelseStatisitkk(
            behandling.toStatistikkBehandling(persongalleri = persongalleri!!.opplysning),
            BehandlingHendelseType.AVBRUTT,
        )
    }

    override suspend fun hentStatistikkBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): StatistikkBehandling? =
        inTransaction { hentBehandling(behandlingId) }?.let {
            val persongalleri: Persongalleri =
                grunnlagKlient
                    .hentPersongalleri(behandlingId, brukerTokenInfo)
                    ?.opplysning
                    ?: throw NoSuchElementException("Persongalleri mangler for sak ${it.sak.id}")

            it.toStatistikkBehandling(persongalleri)
        }

    override suspend fun hentDetaljertBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling? =
        inTransaction { hentBehandling(behandlingId) }?.let {
            val persongalleri: Persongalleri =
                grunnlagKlient
                    .hentPersongalleri(behandlingId, brukerTokenInfo)
                    ?.opplysning
                    ?: throw NoSuchElementException("Persongalleri mangler for sak ${it.sak.id}")

            it.toDetaljertBehandlingWithPersongalleri(persongalleri)
        }

    override suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        request: VirkningstidspunktRequest,
    ): Boolean {
        val behandling =
            requireNotNull(hentBehandling(behandlingId)) { "Fant ikke behandling $behandlingId" }

        if (request.dato.year !in (0..9999) || request.begrunnelse == null) {
            return false
        }

        return when (behandling.type) {
            BehandlingType.REVURDERING -> erGyldigVirkningstidspunktRevurdering(request, behandling)
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                erGyldigVirkningstidspunktFoerstegangsbehandling(
                    request,
                    behandling,
                    brukerTokenInfo,
                )

            else -> throw Exception("BehandlingType ${behandling.type} er ikke støttet")
        }
    }

    /*
     * Gyldig virkningstidspunkt for førstegangsbehandling er basert på dødsdato opp mot når bruker har krav
     * på å søke, samt eventuell opphørsdato.
     * Kravdato er når søknad mottatt. Med unntak av utlandssak hvor egen kravdato kan være eksplisitt angitt.
     *
     * Virk er da gyldig hvis:
     * 1. Saktype BP, den er etter dødsdato, ikke etter eventuelt opphør, og den er samme måned eller etter 3 år før kravdato
     * 2. Saktype OMS, den er etter dødsdato, ikke etter eventuelt opphør, og den er etter 3 år før kravdato
     *
     *
     * UNNTAK:
     * 1. I saker med ukjent avdød vil dødsdato mangle og det vil derfor ikke være mulig å validere.
     *   (nb. når denne kommentaren er skrevet så er det uavklart om ukjent avdød skal støttes kun for migrerte saker fra Pesys).
     * 2. Saker som migreres/gjenopprettes fra Pesys. Det vil ikke være nødvendig å validere virk opp mot rettighet
     *   da bruker allerede har en innvilgelse i Pesys.
     *
     * Basert på lovverk § 22-12. og § 22-13.
     * https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_8-2#%C2%A722-12
     * https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_8-2#%C2%A722-13
     */
    private suspend fun erGyldigVirkningstidspunktFoerstegangsbehandling(
        request: VirkningstidspunktRequest,
        behandling: Behandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        val virkningstidspunkt = request.dato
        if (virkningstidspunktErEtterOpphoerFraOgMed(virkningstidspunkt, behandling.opphoerFraOgMed)) {
            throw VirkningstidspunktKanIkkeVaereEtterOpphoer()
        }
        if (behandling.kilde in listOf(Vedtaksloesning.PESYS, Vedtaksloesning.GJENOPPRETTA)) {
            return true
        }

        val doedsdato = hentDoedsdato(behandling.id, brukerTokenInfo)?.let { YearMonth.from(it) }
        val soeknadMottatt = behandling.mottattDato().let { YearMonth.from(it) }

        if (doedsdato == null) {
            // Mangler dødsfall når avdød er ukjent
            return true
        }

        // For BP er makstidspunkt 3 år - dette gjelder også unntaksvis for OMS
        var makstidspunktFoerSoeknad = soeknadMottatt.minusYears(3)

        if (behandling.utlandstilknytning == null) {
            throw VirkningstidspunktMaaHaUtenlandstilknytning(
                "Utenlandstilknytning må være valgt for å kunne vurdere om virkningstidspunktet er gyldig",
            )
        }
        if (behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND) {
            val kravdato =
                request.kravdato
                    ?: throw KravdatoMaaFinnesHvisBosattutland("Kravdato må finnes hvis bosatt utland er valgt")
            makstidspunktFoerSoeknad = YearMonth.from(kravdato.minusYears(3))
        }

        val virkErEtterDoedsdato = virkningstidspunkt.isAfter(doedsdato)
        val virkErEtterMakstidspunktForSoeknad =
            when (behandling.sak.sakType) {
                SakType.OMSTILLINGSSTOENAD ->
                    // For omstillingsstønad vil virkningstidspunktet tidligst være mnd etter makstidspunkt
                    virkningstidspunkt.isAfter(makstidspunktFoerSoeknad)
                SakType.BARNEPENSJON ->
                    // For barnepensjon vil virkningstidspunktet tidligst være samme mnd som makstidspunkt
                    virkningstidspunkt.isAfter(makstidspunktFoerSoeknad) || virkningstidspunkt == makstidspunktFoerSoeknad
            }
        return virkErEtterDoedsdato && virkErEtterMakstidspunktForSoeknad
    }

    /*
     * Virkningstidspunkt for revurdering kan tidligst være første virkningstidspunkt for saken,
     * og kan heller ikke være etter opphørsdato.
     */
    private fun erGyldigVirkningstidspunktRevurdering(
        request: VirkningstidspunktRequest,
        behandling: Behandling,
    ): Boolean {
        val virkningstidspunkt = request.dato
        if (virkningstidspunktErEtterOpphoerFraOgMed(virkningstidspunkt, behandling.opphoerFraOgMed)) {
            throw VirkningstidspunktKanIkkeVaereEtterOpphoer()
        }

        val foersteVirkDato = hentFoersteVirk(behandling.sak.id)
        if (foersteVirkDato == null) {
            throw KanIkkeOppretteRevurderingUtenIverksattFoerstegangsbehandling()
        } else {
            return virkningstidspunkt.isAfter(foersteVirkDato) || virkningstidspunkt == foersteVirkDato
        }
    }

    private suspend fun hentDoedsdato(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): LocalDate? =
        grunnlagKlient
            .finnPersonOpplysning(behandlingId, Opplysningstype.AVDOED_PDL_V1, brukerTokenInfo)
            .also {
                it?.fnr?.let { fnr ->
                    behandlingRequestLogger.loggRequest(brukerTokenInfo, fnr, "behandling")
                }
            }?.opplysning
            ?.doedsdato

    override fun hentFoersteVirk(sakId: Long): YearMonth? {
        val behandlinger = hentBehandlingerForSak(sakId)
        return behandlinger.tidligsteIverksatteVirkningstidspunkt()?.dato
    }

    override fun oppdaterGrunnlagOgStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        hentBehandlingOrThrow(behandlingId)
            .tilOpprettet()
            .let { behandling ->
                runBlocking { grunnlagService.oppdaterGrunnlag(behandling.id, behandling.sak.id, behandling.sak.sakType) }
                behandlingDao.lagreStatus(behandling)
                hendelseDao.opppdatertGrunnlagHendelse(behandlingId, behandling.sak.id, brukerTokenInfo.ident())
            }
    }

    override suspend fun endrePersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        redigertFamilieforhold: RedigertFamilieforhold,
    ) {
        val forrigePersonGalleri =
            grunnlagKlient.hentPersongalleri(behandlingId, brukerTokenInfo)?.opplysning
                ?: throw PersongalleriFinnesIkkeException()
        inTransaction {
            hentBehandlingOrThrow(behandlingId)
                .tilOpprettet()
                .let { behandling ->
                    val nyeOpplysinger =
                        listOf(
                            lagOpplysning(
                                opplysningsType = Opplysningstype.PERSONGALLERI_V1,
                                kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                                opplysning =
                                    forrigePersonGalleri
                                        .copy(
                                            avdoed = redigertFamilieforhold.avdoede,
                                            gjenlevende = redigertFamilieforhold.gjenlevende,
                                        ).toJsonNode(),
                                periode = null,
                            ),
                        )
                    runBlocking {
                        grunnlagService.leggTilNyeOpplysninger(
                            behandlingId,
                            NyeSaksopplysninger(behandling.sak.id, nyeOpplysinger),
                        )
                    }

                    runBlocking { grunnlagService.oppdaterGrunnlag(behandling.id, behandling.sak.id, behandling.sak.sakType) }
                    behandlingDao.lagreStatus(behandling)
                }
        }
    }

    override fun endreSkalSendeBrev(
        behandlingId: UUID,
        skalSendeBrev: Boolean,
    ) {
        inTransaction {
            val behandling = behandlingDao.hentBehandling(behandlingId)
            require(behandling != null) {
                "Behandling finnes ikke $behandlingId"
            }
            when (behandling.type) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> throw KanIkkeEndreSendeBrevForFoerstegangsbehandling()
                BehandlingType.REVURDERING -> {
                    behandlingDao.lagreSendeBrev(behandlingId, skalSendeBrev)
                }
            }
        }
    }

    override fun hentUtlandstilknytningForSak(sakId: Long): Utlandstilknytning? = hentBehandlingerForSakId(sakId).hentUtlandstilknytning()

    override fun lagreOpphoerFom(
        behandlingId: UUID,
        opphoerFraOgMed: YearMonth,
    ) {
        behandlingDao.lagreOpphoerFom(behandlingId, opphoerFraOgMed)
    }

    data class BehandlingMedData(
        val behandling: Behandling,
        val kommerBarnetTilgode: KommerBarnetTilgode?,
        val hendelserIBehandling: List<LagretHendelse>,
        val viderefoertOpphoer: ViderefoertOpphoer?,
    )

    override suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandlingDto {
        val (behandling, kommerBarnetTilgode, hendelserIBehandling, viderefoertOpphoer) =
            inTransaction {
                val behandling =
                    hentBehandling(behandlingId)
                        ?: throw BehandlingFinnesIkkeException("Vi kan ikke hente behandling $behandlingId, sjekk enhet")

                val hendelserIBehandling = hendelseDao.finnHendelserIBehandling(behandlingId)
                val kommerBarnetTilgode =
                    kommerBarnetTilGodeDao
                        .hentKommerBarnetTilGode(behandlingId)
                        .takeIf { behandling.sak.sakType == SakType.BARNEPENSJON }
                val viderefoertOpphoer = behandlingDao.hentViderefoertOpphoer(behandlingId)
                BehandlingMedData(behandling, kommerBarnetTilgode, hendelserIBehandling, viderefoertOpphoer)
            }

        val sakId = behandling.sak.id
        val sakType = behandling.sak.sakType

        logger.info("Hentet behandling for $behandlingId")

        return DetaljertBehandlingDto(
            id = behandling.id,
            sakId = sakId,
            sakType = sakType,
            sakEnhetId = behandling.sak.enhet,
            gyldighetsprøving = behandling.gyldighetsproeving(),
            kommerBarnetTilgode = kommerBarnetTilgode,
            soeknadMottattDato = behandling.mottattDato(),
            virkningstidspunkt = behandling.virkningstidspunkt,
            utlandstilknytning = behandling.utlandstilknytning,
            boddEllerArbeidetUtlandet = behandling.boddEllerArbeidetUtlandet,
            status = behandling.status,
            hendelser = hendelserIBehandling,
            behandlingType = behandling.type,
            revurderingsaarsak = behandling.revurderingsaarsak(),
            revurderinginfo = behandling.revurderingInfo(),
            begrunnelse = behandling.begrunnelse(),
            kilde = behandling.kilde,
            sendeBrev = behandling.sendeBrev,
            viderefoertOpphoer = viderefoertOpphoer,
        )
    }

    override fun registrerBehandlingHendelse(
        behandling: Behandling,
        saksbehandler: String,
    ) = hendelseDao.behandlingHendelse(
        behandlingId = behandling.id,
        sakId = behandling.sak.id,
        saksbehandler = saksbehandler,
        status = behandling.status,
    )

    override fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
        hendelseType: HendelseType,
    ) {
        hentBehandlingForId(behandlingId)?.let {
            registrerVedtakHendelseFelles(
                vedtakHendelse.vedtakId,
                hendelseType,
                vedtakHendelse.inntruffet,
                vedtakHendelse.saksbehandler,
                vedtakHendelse.kommentar,
                vedtakHendelse.valgtBegrunnelse,
                it,
                hendelseDao,
            )
        }
    }

    override suspend fun oppdaterVirkningstidspunkt(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
        begrunnelse: String,
        kravdato: LocalDate?,
    ): Virkningstidspunkt {
        val behandling =
            hentBehandling(behandlingId) ?: run {
                logger.error("Prøvde å oppdatere virkningstidspunkt på en behandling som ikke eksisterer: $behandlingId")
                throw RuntimeException("Fant ikke behandling")
            }

        val virkningstidspunktData =
            Virkningstidspunkt.create(
                virkningstidspunkt,
                begrunnelse,
                kravdato,
                if (brukerTokenInfo is Saksbehandler) {
                    Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident)
                } else {
                    Grunnlagsopplysning.Gjenny.create(brukerTokenInfo.ident())
                },
            )
        try {
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunktData)
                .also {
                    behandlingDao.lagreNyttVirkningstidspunkt(behandlingId, virkningstidspunktData)
                    behandlingDao.lagreStatus(it)
                }
        } catch (e: NotImplementedError) {
            logger.error(
                "Kan ikke oppdatere virkningstidspunkt for behandling: $behandlingId med typen ${behandling.type}",
                e,
            )
            throw e
        }

        if (behandling.sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            beregningKlient.slettAvkorting(behandling.id, brukerTokenInfo)
        }

        return virkningstidspunktData
    }

    override fun oppdaterBoddEllerArbeidetUtlandet(
        behandlingId: UUID,
        boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet,
    ) {
        val behandling =
            hentBehandling(behandlingId) ?: run {
                logger.error(
                    "Prøvde å oppdatere bodd/arbeidet utlandet på en behandling som ikke eksisterer: $behandlingId",
                )
                throw RuntimeException("Fant ikke behandling")
            }

        try {
            behandling
                .oppdaterBoddEllerArbeidetUtlandnet(boddEllerArbeidetUtlandet)
                .also {
                    behandlingDao.lagreBoddEllerArbeidetUtlandet(behandlingId, boddEllerArbeidetUtlandet)
                    behandlingDao.lagreStatus(it)
                }
        } catch (e: NotImplementedError) {
            logger.error(
                "Kan ikke oppdatere bodd/arbeidet utlandet for behandling: $behandlingId med typen ${behandling.type}",
                e,
            )
            throw e
        }
    }

    override fun oppdaterUtlandstilknytning(
        behandlingId: UUID,
        utlandstilknytning: Utlandstilknytning,
    ) {
        val behandling =
            hentBehandling(behandlingId)
                ?: throw InternfeilException("Kunne ikke oppdatere utlandstilknytning fordi behandlingen ikke finnes")

        behandling
            .oppdaterUtlandstilknytning(utlandstilknytning)
            .also {
                behandlingDao.lagreUtlandstilknytning(behandlingId, utlandstilknytning)
                behandlingDao.lagreStatus(it)
            }
    }

    override fun oppdaterViderefoertOpphoer(
        behandlingId: UUID,
        viderefoertOpphoer: ViderefoertOpphoer,
    ) {
        val behandling =
            hentBehandling(behandlingId)
                ?: throw InternfeilException("Kunne ikke oppdatere videreført opphør fordi behandlingen ikke finnes")

        if (viderefoertOpphoer.skalViderefoere == JaNei.JA &&
            viderefoertOpphoer.vilkaar == null
        ) {
            throw InternfeilException("Kunne ikke oppdatere videreført opphør for behandling $behandlingId fordi vilkår mangla")
        }

        if (virkningstidspunktErEtterOpphoerFraOgMed(behandling.virkningstidspunkt?.dato, viderefoertOpphoer.dato)) {
            throw VirkningstidspunktKanIkkeVaereEtterOpphoer()
        }

        behandling
            .oppdaterVidereførtOpphoer(viderefoertOpphoer)
            .also {
                behandlingDao.lagreViderefoertOpphoer(behandlingId, viderefoertOpphoer)
                behandlingDao.lagreStatus(it)
            }
    }

    override fun hentAapenRegulering(sakId: Long): UUID? =
        behandlingDao
            .hentAlleRevurderingerISakMedAarsak(sakId, Revurderingaarsak.REGULERING)
            .singleOrNull {
                it.status != BehandlingStatus.AVBRUTT && it.status != BehandlingStatus.IVERKSATT
            }?.id

    private fun hentBehandlingOrThrow(behandlingId: UUID) =
        behandlingDao.hentBehandling(behandlingId)
            ?: throw BehandlingNotFoundException(behandlingId)

    private fun List<Behandling>.filterForEnheter(): List<Behandling> {
        val enheterSomSkalFiltreresBort = ArrayList<String>()
        val appUser = Kontekst.get().AppUser
        if (appUser is SaksbehandlerMedEnheterOgRoller) {
            val bruker = appUser.saksbehandlerMedRoller
            if (!bruker.harRolleStrengtFortrolig()) {
                enheterSomSkalFiltreresBort.add(Enheter.STRENGT_FORTROLIG.enhetNr)
            }
            if (!bruker.harRolleEgenAnsatt()) {
                enheterSomSkalFiltreresBort.add(Enheter.EGNE_ANSATTE.enhetNr)
            }
        }

        return filterBehandlingerForEnheter(enheterSomSkalFiltreresBort, this)
    }

    private fun filterBehandlingerForEnheter(
        enheterSomSkalFiltreres: List<String>,
        behandlinger: List<Behandling>,
    ): List<Behandling> = behandlinger.filter { it.sak.enhet !in enheterSomSkalFiltreres }

    private fun virkningstidspunktErEtterOpphoerFraOgMed(
        virkningstidspunkt: YearMonth?,
        opphoerFraOgMed: YearMonth?,
    ) = opphoerFraOgMed != null &&
        virkningstidspunkt != null &&
        virkningstidspunkt.isAfter(opphoerFraOgMed)
}
