package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.ReguleringFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingMedGrunnlagsopplysninger
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.sak.Saksbehandler
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import java.time.YearMonth
import java.util.*

interface GenerellBehandlingService {

    fun hentBehandlinger(): List<Behandling>
    fun hentBehandling(behandlingId: UUID): Behandling?
    fun hentBehandlingstype(behandlingId: UUID): BehandlingType?
    fun hentBehandlingerISak(sakid: Long): List<Behandling>
    fun slettBehandlingerISak(sak: Long)
    fun avbrytBehandling(behandlingId: UUID, saksbehandler: String)
    fun grunnlagISakEndret(sak: Long)
    fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
        hendelseType: HendelseType
    )

    fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse>
    fun alleBehandlingerForSoekerMedFnr(fnr: String): List<Behandling>
    fun alleSakIderForSoekerMedFnr(fnr: String): List<Long>
    fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling?
    suspend fun hentDetaljertBehandlingMedTilbehoer(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
        accessToken: String
    ): DetaljertBehandlingDto

    fun hentSakerOgRollerMedFnrIPersongalleri(fnr: String): List<Pair<Saksrolle, Long>>

    suspend fun hentBehandlingMedEnkelPersonopplysning(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
        accessToken: String,
        opplysningstype: Opplysningstype
    ): BehandlingMedGrunnlagsopplysninger<Person>

    suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
        accessToken: String,
        request: VirkningstidspunktRequest
    ): Boolean
}

class RealGenerellBehandlingService(
    val behandlinger: BehandlingDao,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val revurderingFactory: RevurderingFactory,
    private val reguleringFactory: ReguleringFactory,
    private val hendelser: HendelseDao,
    private val manueltOpphoerService: ManueltOpphoerService,
    private val vedtakKlient: VedtakKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val sporingslogg: Sporingslogg
) : GenerellBehandlingService {

    override fun hentBehandlinger(): List<Behandling> {
        return inTransaction { behandlinger.alleBehandlinger() }
    }

    override fun hentBehandling(behandlingId: UUID): Behandling? {
        return inTransaction {
            behandlinger.hentBehandlingType(behandlingId)?.let { behandlinger.hentBehandling(behandlingId, it) }
        }
    }

    override fun hentBehandlingstype(behandlingId: UUID): BehandlingType? {
        return inTransaction { behandlinger.hentBehandlingType(behandlingId) }
    }

    override fun hentBehandlingerISak(sakid: Long): List<Behandling> {
        return inTransaction {
            behandlinger.alleBehandlingerISak(sakid)
        }
    }

    override fun slettBehandlingerISak(sak: Long) {
        inTransaction {
            println("Sletter alle behandlinger i sak: $sak")
            behandlinger.slettBehandlingerISak(sak)
        }
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

    override fun grunnlagISakEndret(sak: Long) {
        inTransaction {
            behandlinger.alleAktiveBehandlingerISak(sak)
        }.also {
            runBlocking {
                it.forEach {
                    behandlingHendelser.send(it.id to BehandlingHendelseType.GRUNNLAGENDRET)
                }
            }
        }
    }

    override fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling? {
        return hentBehandling(behandlingId)?.toDetaljertBehandling()
    }

    override suspend fun hentBehandlingMedEnkelPersonopplysning(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
        accessToken: String,
        opplysningstype: Opplysningstype
    ): BehandlingMedGrunnlagsopplysninger<Person> {
        val behandling = hentBehandling(behandlingId)?.toDetaljertBehandling()!!
        val sakId = behandling.sak
        val personopplysning = grunnlagKlient.finnPersonOpplysning(
            sakId,
            opplysningstype,
            accessToken
        )
        return BehandlingMedGrunnlagsopplysninger(
            id = behandling.id,
            soeknadMottattDato = behandling.soeknadMottattDato,
            personopplysning = personopplysning
        ).also {
            personopplysning?.fnr?.let { loggRequest(saksbehandler, it) }
        }
    }

    override suspend fun erGyldigVirkningstidspunkt(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
        accessToken: String,
        request: VirkningstidspunktRequest
    ): Boolean {
        val virkningstidspunkt = request.dato
        val begrunnelse = request.begrunnelse
        val harGyldigFormat = virkningstidspunkt.year in (0..9999) && begrunnelse != null

        val behandlingMedDoedsdato = hentBehandlingMedEnkelPersonopplysning(
            behandlingId,
            saksbehandler,
            accessToken,
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
        saksbehandler: Saksbehandler,
        accessToken: String
    ): DetaljertBehandlingDto {
        val detaljertBehandling = hentBehandling(behandlingId)?.toDetaljertBehandling()!!
        val hendelserIBehandling = hentHendelserIBehandling(behandlingId)
        val sakId = detaljertBehandling.sak
        return coroutineScope {
            val vedtak = async { vedtakKlient.hentVedtak(behandlingId.toString(), accessToken) }
            val avdoed = async {
                grunnlagKlient.finnPersonOpplysning(sakId, Opplysningstype.AVDOED_PDL_V1, accessToken)
            }
            val gjenlevende = async {
                grunnlagKlient.finnPersonOpplysning(
                    sakId,
                    Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                    accessToken
                )
            }
            val soeker = async {
                grunnlagKlient.finnPersonOpplysning(sakId, Opplysningstype.SOEKER_PDL_V1, accessToken)
            }

            DetaljertBehandlingDto(
                id = detaljertBehandling.id,
                sak = detaljertBehandling.sak,
                gyldighetsprøving = detaljertBehandling.gyldighetsproeving,
                kommerBarnetTilgode = detaljertBehandling.kommerBarnetTilgode,
                saksbehandlerId = vedtak.await()?.saksbehandlerId,
                datoFattet = vedtak.await()?.datoFattet,
                datoattestert = vedtak.await()?.datoattestert,
                attestant = vedtak.await()?.attestant,
                soeknadMottattDato = detaljertBehandling.soeknadMottattDato,
                virkningstidspunkt = detaljertBehandling.virkningstidspunkt,
                status = detaljertBehandling.status,
                hendelser = hendelserIBehandling,
                familieforhold = Familieforhold(avdoed.await(), gjenlevende.await()),
                behandlingType = detaljertBehandling.behandlingType,
                søker = soeker.await()?.opplysning
            ).also {
                gjenlevende.await()?.fnr?.let { loggRequest(saksbehandler, it) }
                soeker.await()?.fnr?.let { loggRequest(saksbehandler, it) }
            }
        }
    }

    private fun loggRequest(saksbehandler: Saksbehandler, fnr: Foedselsnummer) =
        sporingslogg.logg(
            Sporingsrequest(
                kallendeApplikasjon = "behandling",
                oppdateringstype = HttpMethod.GET,
                brukerId = saksbehandler.ident,
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

                BehandlingType.OMREGNING -> {
                    reguleringFactory.hentRegulering(behandlingId).registrerVedtakHendelse(
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

    override fun alleSakIderForSoekerMedFnr(fnr: String): List<Long> {
        return inTransaction {
            behandlinger.alleSakIderMedUavbruttBehandlingForSoekerMedFnr(fnr)
        }
    }

    override fun hentSakerOgRollerMedFnrIPersongalleri(fnr: String): List<Pair<Saksrolle, Long>> {
        return inTransaction {
            behandlinger.sakerOgRollerMedFnrIPersongalleri(fnr)
        }
    }
}