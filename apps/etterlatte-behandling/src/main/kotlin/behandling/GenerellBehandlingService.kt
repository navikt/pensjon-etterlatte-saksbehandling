package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
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
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.filterForEnheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import no.nav.etterlatte.token.Bruker
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

    fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse>
    fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling?
    suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        bruker: Bruker
    ): DetaljertBehandlingDto

    suspend fun hentBehandlingMedEnkelPersonopplysning(
        behandlingId: UUID,
        bruker: Bruker,
        opplysningstype: Opplysningstype
    ): BehandlingMedGrunnlagsopplysninger<Person>

    suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        bruker: Bruker,
        request: VirkningstidspunktRequest
    ): Boolean
}

class RealGenerellBehandlingService(
    private val behandlingDao: BehandlingDao,
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val hendelseDao: HendelseDao,
    private val vedtakKlient: VedtakKlient,
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
                runBlocking {
                    behandlingHendelser.send(behandlingId to BehandlingHendelseType.AVBRUTT)
                }
            }
        }
    }

    override fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling? {
        return hentBehandling(behandlingId)?.toDetaljertBehandling()
    }

    override suspend fun hentBehandlingMedEnkelPersonopplysning(
        behandlingId: UUID,
        bruker: Bruker,
        opplysningstype: Opplysningstype
    ): BehandlingMedGrunnlagsopplysninger<Person> {
        val behandling = hentBehandling(behandlingId)?.toDetaljertBehandling()!!
        val sakId = behandling.sak
        val personopplysning = grunnlagKlient.finnPersonOpplysning(
            sakId,
            opplysningstype,
            bruker
        )
        return BehandlingMedGrunnlagsopplysninger(
            id = behandling.id,
            soeknadMottattDato = behandling.soeknadMottattDato,
            personopplysning = personopplysning
        ).also {
            personopplysning?.fnr?.let { loggRequest(bruker, it) }
        }
    }

    override suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        bruker: Bruker,
        request: VirkningstidspunktRequest
    ): Boolean {
        val virkningstidspunkt = request.dato
        val begrunnelse = request.begrunnelse
        val harGyldigFormat = virkningstidspunkt.year in (0..9999) && begrunnelse != null

        val behandlingMedDoedsdato = hentBehandlingMedEnkelPersonopplysning(
            behandlingId,
            bruker,
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

    override suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        bruker: Bruker
    ): DetaljertBehandlingDto {
        val detaljertBehandling = hentBehandling(behandlingId)?.toDetaljertBehandling()!!
        val hendelserIBehandling = hentHendelserIBehandling(behandlingId)
        val kommerBarnetTilgode =
            detaljertBehandling.kommerBarnetTilgode.takeIf { detaljertBehandling.sakType == SakType.BARNEPENSJON }

        val sakId = detaljertBehandling.sak
        logger.info("Hentet behandling for $behandlingId")
        return coroutineScope {
            val vedtak = async { vedtakKlient.hentVedtak(behandlingId.toString(), bruker) }
            logger.info("Hentet vedtak for $behandlingId")
            val avdoed = async {
                grunnlagKlient.finnPersonOpplysning(sakId, Opplysningstype.AVDOED_PDL_V1, bruker)
            }
            logger.info("Hentet Opplysningstype.AVDOED_PDL_V1 for $behandlingId")

            val soeker = async {
                grunnlagKlient.finnPersonOpplysning(sakId, Opplysningstype.SOEKER_PDL_V1, bruker)
            }
            logger.info("Hentet Opplysningstype.SOEKER_PDL_V1 for $behandlingId")

            val gjenlevende = if (detaljertBehandling.sakType == SakType.OMSTILLINGSSTOENAD) {
                soeker
            } else {
                async {
                    grunnlagKlient.finnPersonOpplysning(
                        sakId,
                        Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                        bruker
                    )
                }
            }
            logger.info("Hentet Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1 for $behandlingId")

            DetaljertBehandlingDto(
                id = detaljertBehandling.id,
                sak = detaljertBehandling.sak,
                sakType = detaljertBehandling.sakType,
                gyldighetsprøving = detaljertBehandling.gyldighetsproeving,
                kommerBarnetTilgode = kommerBarnetTilgode,
                saksbehandlerId = vedtak.await()?.vedtakFattet?.ansvarligSaksbehandler,
                datoFattet = vedtak.await()?.vedtakFattet?.tidspunkt,
                datoAttestert = vedtak.await()?.attestasjon?.tidspunkt,
                attestant = vedtak.await()?.attestasjon?.attestant,
                soeknadMottattDato = detaljertBehandling.soeknadMottattDato,
                virkningstidspunkt = detaljertBehandling.virkningstidspunkt,
                status = detaljertBehandling.status,
                hendelser = hendelserIBehandling,
                familieforhold = Familieforhold(avdoed.await(), gjenlevende.await()),
                behandlingType = detaljertBehandling.behandlingType,
                søker = soeker.await()?.opplysning,
                revurderingsaarsak = detaljertBehandling.revurderingsaarsak
            ).also {
                gjenlevende.await()?.fnr?.let { loggRequest(bruker, it) }
                soeker.await()?.fnr?.let { loggRequest(bruker, it) }
            }
        }
    }

    private fun loggRequest(bruker: Bruker, fnr: Folkeregisteridentifikator) =
        sporingslogg.logg(
            Sporingsrequest(
                kallendeApplikasjon = "behandling",
                oppdateringstype = HttpMethod.GET,
                brukerId = bruker.ident(),
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