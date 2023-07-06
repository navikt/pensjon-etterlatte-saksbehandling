package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.BehandlingMedGrunnlagsopplysninger
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.common.tidligsteIverksatteVirkningstidspunkt
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import no.nav.etterlatte.tilgangsstyring.filterForEnheter
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.*

enum class BehandlingServiceFeatureToggle(private val key: String) : FeatureToggle {
    FiltrerMedEnhetId("pensjon-etterlatte.filtrer-behandlinger-med-enhet-id");

    override fun key() = key
}

interface GenerellBehandlingService {

    fun hentBehandling(behandlingId: UUID): Behandling?
    fun hentBehandlingerISak(sakId: Long): List<Behandling>
    fun hentSisteIverksatte(sakId: Long): Behandling?
    fun avbrytBehandling(behandlingId: UUID, saksbehandler: String)
    fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
        hendelseType: HendelseType
    )

    fun oppdaterVirkningstidspunkt(
        behandlingId: UUID,
        dato: YearMonth,
        ident: String,
        begrunnelse: String
    ): Virkningstidspunkt

    fun oppdaterUtenlandstilsnitt(
        behandlingId: UUID,
        utenlandstilsnitt: Utenlandstilsnitt
    )

    fun oppdaterBoddEllerArbeidetUtlandet(
        behandlingId: UUID,
        boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet
    )

    fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse>
    fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling?
    suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): DetaljertBehandlingDto

    suspend fun hentBehandlingMedEnkelPersonopplysning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        opplysningstype: Opplysningstype
    ): BehandlingMedGrunnlagsopplysninger<Person>

    suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        request: VirkningstidspunktRequest
    ): Boolean

    fun hentFoersteVirk(sakId: Long): YearMonth?
}

class RealGenerellBehandlingService(
    private val behandlingDao: BehandlingDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
    private val sporingslogg: Sporingslogg,
    private val featureToggleService: FeatureToggleService
) : GenerellBehandlingService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun hentBehandlingForId(id: UUID) =
        behandlingDao.hentBehandling(id)?.let { behandling ->
            listOf(behandling).filterForEnheter().firstOrNull()
        }

    private fun hentBehandlingerForSakId(sakId: Long) =
        behandlingDao.alleBehandlingerISak(sakId).filterForEnheter()

    override fun hentBehandling(behandlingId: UUID): Behandling? {
        return inTransaction {
            hentBehandlingForId(behandlingId)
        }
    }

    override fun hentBehandlingerISak(sakId: Long): List<Behandling> {
        return inTransaction {
            hentBehandlingerForSakId(sakId)
        }
    }

    override fun hentSisteIverksatte(sakId: Long): Behandling? {
        return inTransaction { hentBehandlingerForSakId(sakId) }
            .filter { BehandlingStatus.iverksattEllerAttestert().contains(it.status) }
            .maxByOrNull { it.behandlingOpprettet }
    }

    override fun avbrytBehandling(behandlingId: UUID, saksbehandler: String) {
        inTransaction {
            val behandling = hentBehandlingForId(behandlingId)
                ?: throw BehandlingNotFoundException("Fant ikke behandling med id=$behandlingId som skulle avbrytes")
            if (!behandling.status.kanAvbrytes()) {
                throw IllegalStateException("Kan ikke avbryte en behandling med status ${behandling.status}")
            }

            behandlingDao.avbrytBehandling(behandlingId).also {
                hendelseDao.behandlingAvbrutt(behandling, saksbehandler)
            }.also {
                grunnlagsendringshendelseDao.kobleGrunnlagsendringshendelserFraBehandlingId(behandlingId)
            }
            behandlingHendelser.sendMeldingForHendelse(behandling, BehandlingHendelseType.AVBRUTT)
        }
    }

    override fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling? {
        return hentBehandling(behandlingId)?.toDetaljertBehandling()
    }

    override suspend fun hentBehandlingMedEnkelPersonopplysning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        opplysningstype: Opplysningstype
    ): BehandlingMedGrunnlagsopplysninger<Person> {
        val behandling = requireNotNull(hentBehandling(behandlingId))
        val personopplysning = grunnlagKlient.finnPersonOpplysning(behandling.sak.id, opplysningstype, brukerTokenInfo)

        return BehandlingMedGrunnlagsopplysninger(
            id = behandling.id,
            soeknadMottattDato = behandling.mottattDato(),
            personopplysning = personopplysning
        ).also {
            personopplysning?.fnr?.let { loggRequest(brukerTokenInfo, it) }
        }
    }

    override suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        request: VirkningstidspunktRequest
    ): Boolean {
        val virkningstidspunkt = request.dato
        val begrunnelse = request.begrunnelse
        val harGyldigFormat = virkningstidspunkt.year in (0..9999) && begrunnelse != null

        val behandlingMedDoedsdato = hentBehandlingMedEnkelPersonopplysning(
            behandlingId,
            brukerTokenInfo,
            Opplysningstype.AVDOED_PDL_V1
        )
        val doedsdato = YearMonth.from(behandlingMedDoedsdato.personopplysning?.opplysning?.doedsdato)
        val soeknadMottatt = YearMonth.from(behandlingMedDoedsdato.soeknadMottattDato)
        val makstidspunktFoerSoeknad = soeknadMottatt.minusYears(3)

        val etterMaksTidspunktEllersMinstManedEtterDoedsfall = if (doedsdato.isBefore(makstidspunktFoerSoeknad)) {
            virkningstidspunkt.isAfter(makstidspunktFoerSoeknad)
        } else {
            virkningstidspunkt.isAfter(doedsdato)
        }

        return harGyldigFormat && etterMaksTidspunktEllersMinstManedEtterDoedsfall
    }

    override fun hentFoersteVirk(sakId: Long): YearMonth? {
        val behandlinger = hentBehandlingerISak(sakId)
        return behandlinger.tidligsteIverksatteVirkningstidspunkt()?.dato
    }

    override suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): DetaljertBehandlingDto {
        val behandling = hentBehandling(behandlingId)!!
        val hendelserIBehandling = hentHendelserIBehandling(behandlingId)

        val sakId = behandling.sak.id
        val sakType = behandling.sak.sakType

        val kommerBarnetTilgode = behandling.kommerBarnetTilgode.takeIf { sakType == SakType.BARNEPENSJON }

        logger.info("Hentet behandling for $behandlingId")
        return coroutineScope {
            logger.info("Hentet vedtak for $behandlingId")
            val avdoed = async {
                grunnlagKlient.finnPersonOpplysning(sakId, Opplysningstype.AVDOED_PDL_V1, brukerTokenInfo)
            }
            logger.info("Hentet Opplysningstype.AVDOED_PDL_V1 for $behandlingId")

            val soeker = async {
                grunnlagKlient.finnPersonOpplysning(sakId, Opplysningstype.SOEKER_PDL_V1, brukerTokenInfo)
            }
            logger.info("Hentet Opplysningstype.SOEKER_PDL_V1 for $behandlingId")

            val gjenlevende = if (sakType == SakType.OMSTILLINGSSTOENAD) {
                soeker
            } else {
                async {
                    grunnlagKlient.finnPersonOpplysning(
                        sakId,
                        Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                        brukerTokenInfo
                    )
                }
            }
            logger.info("Hentet Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1 for $behandlingId")

            DetaljertBehandlingDto(
                id = behandling.id,
                sak = sakId,
                sakType = sakType,
                gyldighetsprøving = behandling.gyldighetsproeving(),
                kommerBarnetTilgode = kommerBarnetTilgode,
                soeknadMottattDato = behandling.mottattDato(),
                virkningstidspunkt = behandling.virkningstidspunkt,
                utenlandstilsnitt = behandling.utenlandstilsnitt,
                boddEllerArbeidetUtlandet = behandling.boddEllerArbeidetUtlandet,
                status = behandling.status,
                hendelser = hendelserIBehandling,
                familieforhold = Familieforhold(avdoed.await(), gjenlevende.await()),
                behandlingType = behandling.type,
                søker = soeker.await()?.opplysning,
                revurderingsaarsak = behandling.revurderingsaarsak(),
                revurderinginfo = behandling.revurderingInfo()
            ).also {
                gjenlevende.await()?.fnr?.let { loggRequest(brukerTokenInfo, it) }
                soeker.await()?.fnr?.let { loggRequest(brukerTokenInfo, it) }
            }
        }
    }

    private fun loggRequest(brukerTokenInfo: BrukerTokenInfo, fnr: Folkeregisteridentifikator) =
        sporingslogg.logg(
            Sporingsrequest(
                kallendeApplikasjon = "behandling",
                oppdateringstype = HttpMethod.GET,
                brukerId = brukerTokenInfo.ident(),
                hvemBlirSlaattOpp = fnr.value,
                endepunkt = "behandling",
                resultat = Decision.Permit,
                melding = "Hent behandling var vellykka"
            )
        )

    override fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
        hendelseType: HendelseType
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
                hendelseDao
            )
        }
    }

    override fun oppdaterVirkningstidspunkt(
        behandlingId: UUID,
        dato: YearMonth,
        ident: String,
        begrunnelse: String
    ): Virkningstidspunkt {
        val behandling = hentBehandling(behandlingId) ?: run {
            logger.error("Prøvde å oppdatere virkningstidspunkt på en behandling som ikke eksisterer: $behandlingId")
            throw RuntimeException("Fant ikke behandling")
        }

        val virkningstidspunkt = Virkningstidspunkt.create(dato, ident, begrunnelse)
        try {
            behandling.oppdaterVirkningstidspunkt(virkningstidspunkt)
                .also {
                    inTransaction {
                        behandlingDao.lagreNyttVirkningstidspunkt(behandlingId, virkningstidspunkt)
                        behandlingDao.lagreStatus(it)
                    }
                }
        } catch (e: NotImplementedError) {
            logger.error(
                "Kan ikke oppdatere virkningstidspunkt for behandling: $behandlingId med typen ${behandling.type}",
                e
            )
            throw e
        }

        return virkningstidspunkt
    }

    override fun oppdaterUtenlandstilsnitt(
        behandlingId: UUID,
        utenlandstilsnitt: Utenlandstilsnitt
    ) {
        val behandling = hentBehandling(behandlingId) ?: run {
            logger.error("Prøvde å oppdatere utenlandstilsnitt på en behandling som ikke eksisterer: $behandlingId")
            throw RuntimeException("Fant ikke behandling")
        }

        try {
            behandling.oppdaterUtenlandstilsnitt(utenlandstilsnitt)
                .also {
                    inTransaction {
                        behandlingDao.lagreUtenlandstilsnitt(behandlingId, utenlandstilsnitt)
                        behandlingDao.lagreStatus(it)
                    }
                }
        } catch (e: NotImplementedError) {
            logger.error(
                "Kan ikke oppdatere utenlandstilsnitt for behandling: $behandlingId med typen ${behandling.type}",
                e
            )
            throw e
        }
    }

    override fun oppdaterBoddEllerArbeidetUtlandet(
        behandlingId: UUID,
        boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet
    ) {
        val behandling = hentBehandling(behandlingId) ?: run {
            logger.error(
                "Prøvde å oppdatere bodd/arbeidet utlandet på en behandling som ikke eksisterer: $behandlingId"
            )
            throw RuntimeException("Fant ikke behandling")
        }

        try {
            behandling.oppdaterBoddEllerArbeidetUtlandnet(boddEllerArbeidetUtlandet)
                .also {
                    inTransaction {
                        behandlingDao.lagreBoddEllerArbeidetUtlandet(behandlingId, boddEllerArbeidetUtlandet)
                        behandlingDao.lagreStatus(it)
                    }
                }
        } catch (e: NotImplementedError) {
            logger.error(
                "Kan ikke oppdatere bodd/arbeidet utlandet for behandling: $behandlingId med typen ${behandling.type}",
                e
            )
            throw e
        }
    }

    override fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse> {
        return inTransaction {
            hendelseDao.finnHendelserIBehandling(behandlingId)
        }
    }

    private fun List<Behandling>.filterForEnheter() =
        this.filterBehandlingerForEnheter(
            featureToggleService = featureToggleService,
            user = Kontekst.get().AppUser
        )
}

fun <T : Behandling> List<T>.filterBehandlingerForEnheter(
    featureToggleService: FeatureToggleService,
    user: User
) = this.filterForEnheter(
    featureToggleService,
    BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
    user
) { item, enheter ->
    enheter.contains(item.sak.enhet)
}