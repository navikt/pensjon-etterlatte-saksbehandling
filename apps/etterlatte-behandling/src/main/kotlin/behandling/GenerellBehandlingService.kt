package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    )

    fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse>
    fun alleBehandlingerForSoekerMedFnr(fnr: String): List<Behandling>
    fun alleSakIderForSoekerMedFnr(fnr: String): List<Long>
    fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling?
    suspend fun hentDetaljertBehandlingMedTilbehoer(behandlingId: UUID, accessToken: String): DetaljertBehandlingDto
    fun hentSakerOgRollerMedFnrIPersongalleri(fnr: String): List<Pair<Saksrolle, Long>>
}

class RealGenerellBehandlingService(
    val behandlinger: BehandlingDao,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val revurderingFactory: RevurderingFactory,
    private val hendelser: HendelseDao,
    private val manueltOpphoerService: ManueltOpphoerService,
    private val vedtakKlient: VedtakKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient
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

    override suspend fun hentDetaljertBehandlingMedTilbehoer(behandlingId: UUID, accessToken: String):
        DetaljertBehandlingDto {
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
            val beregning = async {
                beregningKlient.hentBeregning(UUID.fromString(behandlingId.toString()), accessToken)
            }
            val vilkaarsvurdering = async {
                vilkaarsvurderingKlient.hentVilkaarsvurdering(
                    behandlingId,
                    accessToken
                )
            }
            DetaljertBehandlingDto(
                id = detaljertBehandling.id,
                sak = detaljertBehandling.sak,
                gyldighetsprøving = detaljertBehandling.gyldighetsproeving,
                kommerBarnetTilgode = detaljertBehandling.kommerBarnetTilgode,
                vilkårsprøving = vilkaarsvurdering.await(),
                beregning = beregning.await(),
                saksbehandlerId = vedtak.await()?.saksbehandlerId,
                fastsatt = vedtak.await()?.vedtakFattet,
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
            )
        }
    }

    override fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) {
        inTransaction {
            behandlinger.hentBehandlingType(behandlingId)?.let {
                when (it) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> {
                        foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandlingId).registrerVedtakHendelse(
                            vedtakId,
                            hendelse,
                            inntruffet,
                            saksbehandler,
                            kommentar,
                            begrunnelse,
                            behandlinger
                        )
                    }

                    BehandlingType.REVURDERING -> {
                        revurderingFactory.hentRevurdering(behandlingId).registrerVedtakHendelse(
                            vedtakId,
                            hendelse,
                            inntruffet,
                            saksbehandler,
                            kommentar,
                            begrunnelse,
                            behandlinger
                        )
                    }

                    BehandlingType.MANUELT_OPPHOER -> {
                        manueltOpphoerService.registrerVedtakHendelse(
                            behandlingId,
                            vedtakId,
                            hendelse,
                            inntruffet,
                            saksbehandler,
                            kommentar,
                            begrunnelse,
                            behandlinger
                        )
                    }
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