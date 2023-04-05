package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.BehandlingMedGrunnlagsopplysninger
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
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
import java.util.UUID

interface GenerellBehandlingService {

    fun hentBehandlinger(): List<Behandling>
    fun hentBehandling(behandlingId: UUID): Behandling?
    fun hentBehandlingstype(behandlingId: UUID): BehandlingType?
    fun hentBehandlingerISak(sakId: Long): List<Behandling>
    fun hentSenestIverksatteBehandling(sakId: Long): Behandling?
    fun avbrytBehandling(behandlingId: UUID, saksbehandler: String)
    fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
        hendelseType: HendelseType
    )

    fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse>
    fun alleBehandlingerForSoekerMedFnr(fnr: String): List<Behandling>
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
    val behandlinger: BehandlingDao,
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val revurderingFactory: RevurderingFactory,
    private val hendelser: HendelseDao,
    private val manueltOpphoerService: ManueltOpphoerService,
    private val vedtakKlient: VedtakKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val sporingslogg: Sporingslogg
) : GenerellBehandlingService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun hentBehandlinger(): List<Behandling> {
        return inTransaction { behandlinger.alleBehandlinger() }
    }

    override fun hentBehandling(behandlingId: UUID): Behandling? {
        return inTransaction {
            behandlinger.hentBehandling(behandlingId)
        }
    }

    override fun hentBehandlingstype(behandlingId: UUID): BehandlingType? {
        return inTransaction { behandlinger.hentBehandlingType(behandlingId) }
    }

    override fun hentBehandlingerISak(sakId: Long): List<Behandling> {
        return inTransaction {
            behandlinger.alleBehandlingerISak(sakId)
        }
    }

    override fun hentSenestIverksatteBehandling(sakId: Long): Behandling? {
        val alleBehandlinger = inTransaction { behandlinger.alleBehandlingerISak(sakId) }

        return alleBehandlinger
            .filter { BehandlingStatus.iverksattEllerAttestert().contains(it.status) }
            .maxByOrNull { it.behandlingOpprettet }
    }

    override fun avbrytBehandling(behandlingId: UUID, saksbehandler: String) {
        inTransaction {
            val behandling = behandlinger.hentBehandling(behandlingId)
                ?: throw BehandlingNotFoundException("Fant ikke behandling med id=$behandlingId som skulle avbrytes")
            if (!behandling.status.kanAvbrytes()) {
                throw IllegalStateException("Kan ikke avbryte en behandling med status ${behandling.status}")
            }
            behandlinger.avbrytBehandling(behandlingId).also {
                hendelser.behandlingAvbrutt(behandling, saksbehandler)
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
                søker = soeker.await()?.opplysning
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
        behandlinger.hentBehandlingType(behandlingId)?.let {
            when (it) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandlingId).registrerVedtakHendelse(
                        vedtakHendelse.vedtakId,
                        hendelseType,
                        vedtakHendelse.inntruffet,
                        vedtakHendelse.saksbehandler,
                        vedtakHendelse.kommentar,
                        vedtakHendelse.valgtBegrunnelse
                    )
                }

                BehandlingType.REVURDERING -> {
                    revurderingFactory.hentRevurdering(behandlingId).registrerVedtakHendelse(
                        vedtakHendelse.vedtakId,
                        hendelseType,
                        vedtakHendelse.inntruffet,
                        vedtakHendelse.saksbehandler,
                        vedtakHendelse.kommentar,
                        vedtakHendelse.valgtBegrunnelse
                    )
                }

                BehandlingType.MANUELT_OPPHOER -> {
                    manueltOpphoerService.registrerVedtakHendelse(
                        behandlingId,
                        vedtakHendelse.vedtakId,
                        hendelseType,
                        vedtakHendelse.inntruffet,
                        vedtakHendelse.saksbehandler,
                        vedtakHendelse.kommentar,
                        vedtakHendelse.valgtBegrunnelse
                    )
                }
            }
        }
    }

    override fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse> {
        return inTransaction {
            hendelser.finnHendelserIBehandling(behandlingId)
        }
    }

    override fun alleBehandlingerForSoekerMedFnr(fnr: String): List<Behandling> {
        return inTransaction {
            behandlinger.alleBehandlingerForSoekerMedFnr(fnr)
        }
    }
}