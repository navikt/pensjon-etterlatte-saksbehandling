package no.nav.etterlatte.behandling.foerstegangsbehandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.ManuellVurdering
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

interface FoerstegangsbehandlingService {
    fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling?
    fun startFoerstegangsbehandling(
        sak: Long,
        persongalleri: Persongalleri,
        mottattDato: String
    ): Foerstegangsbehandling

    fun lagreGyldighetsproeving(
        behandling: UUID,
        navIdent: String,
        svar: JaNei,
        begrunnelse: String
    ): GyldighetsResultat

    fun lagreGyldighetsproeving(behandling: UUID, gyldighetsproeving: GyldighetsResultat)
    fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode)
    fun settOpprettet(behandlingId: UUID, dryRun: Boolean = true)
    fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean = true, utfall: VilkaarsvurderingUtfall?)
    fun settBeregnet(behandlingId: UUID, dryRun: Boolean = true)
    fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean = true)
    fun settAttestert(behandlingId: UUID, dryRun: Boolean = true)
    fun settReturnert(behandlingId: UUID, dryRun: Boolean = true)
    fun settIverksatt(behandlingId: UUID, dryRun: Boolean = true)
}

class RealFoerstegangsbehandlingService(
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val klokke: Clock = utcKlokke()
) : FoerstegangsbehandlingService {
    private val logger = LoggerFactory.getLogger(RealFoerstegangsbehandlingService::class.java)

    fun hentBehandling(id: UUID): Foerstegangsbehandling = requireNotNull(
        behandlingDao.hentBehandling(id) as Foerstegangsbehandling
    )

    override fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling {
        return inTransaction {
            hentBehandling(behandling)
        }
    }

    override fun startFoerstegangsbehandling(
        sak: Long,
        persongalleri: Persongalleri,
        mottattDato: String
    ): Foerstegangsbehandling {
        logger.info("Starter behandling i sak $sak")
        return inTransaction {
            OpprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = LocalDateTime.parse(mottattDato),
                persongalleri = persongalleri
            ).let { opprettBehandling ->
                behandlingDao.opprettBehandling(opprettBehandling)
                hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

                logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

                hentBehandling(opprettBehandling.id)
            }
        }.also {
            runBlocking {
                behandlingHendelser.send(it.id to BehandlingHendelseType.OPPRETTET)
            }
        }
    }

    override fun lagreGyldighetsproeving(
        behandling: UUID,
        navIdent: String,
        svar: JaNei,
        begrunnelse: String
    ): GyldighetsResultat {
        return inTransaction {
            val resultat =
                if (svar == JaNei.JA) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
            val gyldighetsResultat = GyldighetsResultat(
                resultat = resultat,
                vurderinger = listOf(
                    VurdertGyldighet(
                        navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
                        resultat = resultat,
                        basertPaaOpplysninger = ManuellVurdering(
                            begrunnelse = begrunnelse,
                            kilde = Grunnlagsopplysning.Saksbehandler(navIdent, Tidspunkt.from(klokke.norskKlokke()))
                        )
                    )
                ),
                vurdertDato = Tidspunkt(klokke.instant()).toLocalDatetimeNorskTid()
            )

            hentBehandling(behandling).lagreGyldighetsproeving(gyldighetsResultat)

            gyldighetsResultat
        }
    }

    override fun lagreGyldighetsproeving(behandling: UUID, gyldighetsproeving: GyldighetsResultat) {
        inTransaction {
            hentBehandling(behandling).lagreGyldighetsproeving(gyldighetsproeving)
        }
    }

    private fun Foerstegangsbehandling.lagreGyldighetsproeving(gyldighetsproeving: GyldighetsResultat) {
        this.oppdaterGyldighetsproeving(gyldighetsproeving)
            .also {
                behandlingDao.lagreGyldighetsproving(it)
                logger.info("behandling ${it.id} i sak ${it.sak} er gyldighetsprøvd")
            }
    }

    override fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode) {
        return inTransaction {
            hentBehandling(behandlingId).lagreKommerBarnetTilgode(kommerBarnetTilgode)
        }
    }

    private fun Foerstegangsbehandling.lagreKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode) {
        this.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .also { behandlingDao.lagreKommerBarnetTilgode(it.id, kommerBarnetTilgode) }
            .also { behandlingDao.lagreStatus(it) }
    }

    override fun settOpprettet(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilOpprettet().lagreEndring(dryRun)
    }

    override fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean, utfall: VilkaarsvurderingUtfall?) {
        val behandling = hentFoerstegangsbehandling(behandlingId).tilVilkaarsvurdert(utfall)

        if (!dryRun) {
            inTransaction {
                behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)
                behandlingDao.lagreVilkaarstatus(behandling.id, behandling.vilkaarUtfall)
            }
        }
    }

    override fun settBeregnet(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilBeregnet().lagreEndring(dryRun)
    }

    override fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilFattetVedtak().lagreEndring(dryRun)
    }

    override fun settAttestert(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilAttestert().lagreEndring(dryRun)
    }

    override fun settReturnert(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilReturnert().lagreEndring(dryRun)
    }

    override fun settIverksatt(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilIverksatt().lagreEndring(dryRun)
    }

    private fun Foerstegangsbehandling.lagreEndring(dryRun: Boolean) {
        if (dryRun) return

        lagreNyBehandlingStatus(this)
    }

    private fun lagreNyBehandlingStatus(behandling: Foerstegangsbehandling) {
        inTransaction {
            behandling.let {
                behandlingDao.lagreStatus(it.id, it.status, it.sistEndret)
            }
        }
    }
}