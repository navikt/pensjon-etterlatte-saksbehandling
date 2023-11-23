package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.BehandlingMedGrunnlagsopplysning
import no.nav.etterlatte.behandling.domain.toDetaljertBehandlingWithPersongalleri
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.etterbetaling.EtterbetalingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.common.tidligsteIverksatteVirkningstidspunkt
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.UtenlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakMedUtenlandstilknytning
import no.nav.etterlatte.tilgangsstyring.filterForEnheter
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

enum class BehandlingServiceFeatureToggle(private val key: String) : FeatureToggle {
    FiltrerMedEnhetId("pensjon-etterlatte.filtrer-behandlinger-med-enhet-id"),
    ;

    override fun key() = key
}

class BehandlingFinnesIkkeException(message: String) : Exception(message)

class KravdatoMaaFinnesHvisBosattutland(message: String) :
    UgyldigForespoerselException(code = "BOSATTUTLAND_MÅ_HA_KRAVDATO", detail = message)

class VirkningstidspunktMaaHaUtenlandstilknytning(message: String) :
    UgyldigForespoerselException(code = "VIRK_MÅ_HA UTENLANDSTILKNYTNING", detail = message)

interface BehandlingService {
    fun hentBehandling(behandlingId: UUID): Behandling?

    fun hentBehandlingerForSak(sakId: Long): List<Behandling>

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

    fun oppdaterVirkningstidspunkt(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        ident: String,
        begrunnelse: String,
        kravdato: YearMonth? = null,
    ): Virkningstidspunkt

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

    fun oppdaterGrunnlagOgStatus(behandlingId: UUID)
}

internal class BehandlingServiceImpl(
    private val behandlingDao: BehandlingDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingRequestLogger: BehandlingRequestLogger,
    private val featureToggleService: FeatureToggleService,
    private val kommerBarnetTilGodeDao: KommerBarnetTilGodeDao,
    private val oppgaveService: OppgaveService,
    private val etterbetalingService: EtterbetalingService,
    private val grunnlagService: GrunnlagService,
    private val sakDao: SakDao,
) : BehandlingService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun hentBehandlingForId(id: UUID) =
        behandlingDao.hentBehandling(id)?.let { behandling ->
            listOf(behandling).filterForEnheter().firstOrNull()
        }

    private fun hentBehandlingerForSakId(sakId: Long) = behandlingDao.alleBehandlingerISak(sakId).filterForEnheter()

    override fun hentBehandling(behandlingId: UUID): Behandling? {
        return hentBehandlingForId(behandlingId)
    }

    override fun hentBehandlingerForSak(sakId: Long): List<Behandling> {
        return hentBehandlingerForSakId(sakId)
    }

    override fun hentSisteIverksatte(sakId: Long): Behandling? {
        return hentBehandlingerForSakId(sakId)
            .filter { BehandlingStatus.iverksattEllerAttestert().contains(it.status) }
            .maxByOrNull { it.behandlingOpprettet }
    }

    override fun avbrytBehandling(
        behandlingId: UUID,
        saksbehandler: BrukerTokenInfo,
    ) {
        val behandling =
            hentBehandlingForId(behandlingId)
                ?: throw BehandlingNotFoundException("Fant ikke behandling med id=$behandlingId som skulle avbrytes")
        if (!behandling.status.kanAvbrytes()) {
            throw IllegalStateException("Kan ikke avbryte en behandling med status ${behandling.status}")
        }

        behandlingDao.avbrytBehandling(behandlingId).also {
            val hendelserKnyttetTilBehandling =
                grunnlagsendringshendelseDao.hentGrunnlagsendringshendelseSomErTattMedIBehandling(behandlingId)
            oppgaveService.avbrytOppgaveUnderBehandling(behandlingId.toString(), saksbehandler)

            hendelserKnyttetTilBehandling.forEach { hendelse ->
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    referanse = hendelse.id.toString(),
                    sakId = behandling.sak.id,
                    oppgaveKilde = OppgaveKilde.HENDELSE,
                    oppgaveType = OppgaveType.VURDER_KONSEKVENS,
                    merknad = hendelse.beskrivelse(),
                )
            }

            hendelseDao.behandlingAvbrutt(behandling, saksbehandler.ident())
            grunnlagsendringshendelseDao.kobleGrunnlagsendringshendelserFraBehandlingId(behandlingId)
        }

        val persongalleri =
            runBlocking { grunnlagKlient.hentPersongalleri(behandlingId, saksbehandler) }

        behandlingHendelser.sendMeldingForHendelseMedDetaljertBehandling(
            behandling.toStatistikkBehandling(persongalleri = persongalleri!!.opplysning),
            BehandlingHendelseType.AVBRUTT,
        )
    }

    override suspend fun hentStatistikkBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): StatistikkBehandling? {
        return inTransaction { hentBehandling(behandlingId) }?.let {
            val persongalleri: Persongalleri =
                grunnlagKlient.hentPersongalleri(behandlingId, brukerTokenInfo)
                    ?.opplysning
                    ?: throw NoSuchElementException("Persongalleri mangler for sak ${it.sak.id}")

            it.toStatistikkBehandling(persongalleri)
        }
    }

    override suspend fun hentDetaljertBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling? {
        return inTransaction { hentBehandling(behandlingId) }?.let {
            val persongalleri: Persongalleri =
                grunnlagKlient.hentPersongalleri(behandlingId, brukerTokenInfo)
                    ?.opplysning
                    ?: throw NoSuchElementException("Persongalleri mangler for sak ${it.sak.id}")

            it.toDetaljertBehandlingWithPersongalleri(persongalleri)
        }
    }

    internal suspend fun hentBehandlingMedEnkelPersonopplysning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        opplysningstype: Opplysningstype,
    ): BehandlingMedGrunnlagsopplysning<Person> {
        val behandling =
            requireNotNull(inTransaction { hentBehandling(behandlingId) }) { "Fant ikke behandling $behandlingId" }
        val personopplysning =
            grunnlagKlient.finnPersonOpplysning(behandlingId, opplysningstype, brukerTokenInfo)
        val sakUtenlandstilknytning = inTransaction { sakDao.hentUtenlandstilknytningForSak(behandling.sak.id) }

        return BehandlingMedGrunnlagsopplysning(
            behandling = behandling,
            soeknadMottattDato = behandling.mottattDato(),
            personopplysning = personopplysning,
            utenlandstilknytning = sakUtenlandstilknytning,
        ).also {
            personopplysning?.fnr?.let { behandlingRequestLogger.loggRequest(brukerTokenInfo, it, "behandling") }
        }
    }

    override suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        request: VirkningstidspunktRequest,
    ): Boolean {
        val virkningstidspunkt = request.dato
        val begrunnelse = request.begrunnelse
        val harGyldigFormat = virkningstidspunkt.year in (0..9999) && begrunnelse != null

        val behandlingMedDoedsdatoOgUtenlandstilknytning =
            hentBehandlingMedEnkelPersonopplysning(
                behandlingId,
                brukerTokenInfo,
                Opplysningstype.AVDOED_PDL_V1,
            )
        val doedsdato = YearMonth.from(behandlingMedDoedsdatoOgUtenlandstilknytning.personopplysning?.opplysning?.doedsdato)

        val sakMedUtenlandstilknytning = behandlingMedDoedsdatoOgUtenlandstilknytning.utenlandstilknytning
        val soeknadMottatt = YearMonth.from(behandlingMedDoedsdatoOgUtenlandstilknytning.soeknadMottattDato)
        var makstidspunktFoerSoeknad = soeknadMottatt.minusYears(3)

        if (sakMedUtenlandstilknytning?.utenlandstilknytning != null) {
            if (sakMedUtenlandstilknytning.utenlandstilknytning.type === UtenlandstilknytningType.BOSATT_UTLAND) {
                val kravdato = request.kravdato ?: throw KravdatoMaaFinnesHvisBosattutland("Kravdato må finnes hvis bosatt utland er valgt")
                makstidspunktFoerSoeknad = kravdato.minusYears(3)
            }
        } else {
            throw VirkningstidspunktMaaHaUtenlandstilknytning(
                "Utenlandstilknytning må være valgt for å kunne vurdere om virkningstidspunktet er gyldig",
            )
        }

        val etterMaksTidspunktEllersMinstManedEtterDoedsfall =
            if (doedsdato.isBefore(makstidspunktFoerSoeknad)) {
                virkningstidspunkt.isAfter(makstidspunktFoerSoeknad)
            } else {
                virkningstidspunkt.isAfter(doedsdato)
            }

        return harGyldigFormat && etterMaksTidspunktEllersMinstManedEtterDoedsfall
    }

    override fun hentFoersteVirk(sakId: Long): YearMonth? {
        val behandlinger = hentBehandlingerForSak(sakId)
        return behandlinger.tidligsteIverksatteVirkningstidspunkt()?.dato
    }

    override fun oppdaterGrunnlagOgStatus(behandlingId: UUID) {
        hentBehandlingOrThrow(behandlingId)
            .tilOpprettet()
            .let { behandling ->
                grunnlagService.oppdaterGrunnlag(behandling.id, behandling.sak.id, behandling.sak.sakType)

                behandlingDao.lagreStatus(behandling)
            }
    }

    data class BehandlingMedData(
        val behandling: Behandling,
        val kommerBarnetTilgode: KommerBarnetTilgode?,
        val hendelserIBehandling: List<LagretHendelse>,
        val sakOgUtenlandstilknytning: SakMedUtenlandstilknytning?,
    )

    override suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandlingDto {
        val (behandling, kommerBarnetTilgode, hendelserIBehandling, sakOgUtenlandstilknytning) =
            inTransaction {
                val behandling =
                    hentBehandling(behandlingId)
                        ?: throw BehandlingFinnesIkkeException("Vi kan ikke hente behandling $behandlingId, sjekk enhet")

                val hendelserIBehandling = hendelseDao.finnHendelserIBehandling(behandlingId)
                val kommerBarnetTilgode =
                    kommerBarnetTilGodeDao.hentKommerBarnetTilGode(behandlingId)
                        .takeIf { behandling.sak.sakType == SakType.BARNEPENSJON }
                val sakOgUtenlandstilknytning = sakDao.hentUtenlandstilknytningForSak(behandling.sak.id)
                BehandlingMedData(behandling, kommerBarnetTilgode, hendelserIBehandling, sakOgUtenlandstilknytning)
            }

        val sakId = behandling.sak.id
        val sakType = behandling.sak.sakType

        logger.info("Hentet behandling for $behandlingId")
        return coroutineScope {
            logger.info("Hentet vedtak for $behandlingId")
            val avdoed =
                async {
                    grunnlagKlient.finnPersonOpplysning(
                        behandlingId,
                        Opplysningstype.AVDOED_PDL_V1,
                        brukerTokenInfo,
                    )
                }
            logger.info("Hentet Opplysningstype.AVDOED_PDL_V1 for $behandlingId")

            val soeker =
                async {
                    grunnlagKlient.finnPersonOpplysning(
                        behandlingId,
                        Opplysningstype.SOEKER_PDL_V1,
                        brukerTokenInfo,
                    )
                }
            logger.info("Hentet Opplysningstype.SOEKER_PDL_V1 for $behandlingId")

            val gjenlevende =
                if (sakType == SakType.OMSTILLINGSSTOENAD) {
                    soeker
                } else {
                    async {
                        grunnlagKlient.finnPersonOpplysning(
                            behandlingId,
                            Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                            brukerTokenInfo,
                        )
                    }
                }
            logger.info("Hentet Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1 for $behandlingId")

            DetaljertBehandlingDto(
                id = behandling.id,
                sakId = sakId,
                sakType = sakType,
                gyldighetsprøving = behandling.gyldighetsproeving(),
                kommerBarnetTilgode = kommerBarnetTilgode,
                soeknadMottattDato = behandling.mottattDato(),
                virkningstidspunkt = behandling.virkningstidspunkt,
                utenlandstilknytning = sakOgUtenlandstilknytning?.utenlandstilknytning,
                boddEllerArbeidetUtlandet = behandling.boddEllerArbeidetUtlandet,
                status = behandling.status,
                hendelser = hendelserIBehandling,
                familieforhold = Familieforhold(avdoed.await(), gjenlevende.await()),
                behandlingType = behandling.type,
                søker = soeker.await()?.opplysning,
                revurderingsaarsak = behandling.revurderingsaarsak(),
                revurderinginfo = behandling.revurderingInfo(),
                begrunnelse = behandling.begrunnelse(),
                etterbetaling = inTransaction { etterbetalingService.hentEtterbetaling(behandlingId) },
            ).also {
                gjenlevende.await()?.fnr?.let { behandlingRequestLogger.loggRequest(brukerTokenInfo, it, "behandling") }
                soeker.await()?.fnr?.let { behandlingRequestLogger.loggRequest(brukerTokenInfo, it, "behandling") }
            }
        }
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

    override fun oppdaterVirkningstidspunkt(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        ident: String,
        begrunnelse: String,
        kravdato: YearMonth?,
    ): Virkningstidspunkt {
        val behandling =
            hentBehandling(behandlingId) ?: run {
                logger.error("Prøvde å oppdatere virkningstidspunkt på en behandling som ikke eksisterer: $behandlingId")
                throw RuntimeException("Fant ikke behandling")
            }

        val virkningstidspunktData = Virkningstidspunkt.create(virkningstidspunkt, ident, begrunnelse, kravdato)
        try {
            behandling.oppdaterVirkningstidspunkt(virkningstidspunktData)
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
            behandling.oppdaterBoddEllerArbeidetUtlandnet(boddEllerArbeidetUtlandet)
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

    private fun List<Behandling>.filterForEnheter() =
        this.filterBehandlingerForEnheter(
            featureToggleService = featureToggleService,
            user = Kontekst.get().AppUser,
        )

    private fun hentBehandlingOrThrow(behandlingId: UUID) =
        (
            behandlingDao.hentBehandling(behandlingId)
                ?: throw BehandlingNotFoundException("Fant ikke behandling med id=$behandlingId")
        )
}

fun <T : Behandling> List<T>.filterBehandlingerForEnheter(
    featureToggleService: FeatureToggleService,
    user: User,
) = this.filterForEnheter(
    featureToggleService,
    BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
    user,
) { item, enheter ->
    enheter.contains(item.sak.enhet)
}
