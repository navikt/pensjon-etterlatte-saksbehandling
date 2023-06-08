package no.nav.etterlatte.behandling.foerstegangsbehandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.filterBehandlingerForEnheter
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.ManuellVurdering
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.filterSakerForEnheter
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

interface FoerstegangsbehandlingService {
    fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling?
    fun startFoerstegangsbehandling(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning
    ): Foerstegangsbehandling?

    fun lagreGyldighetsproeving(
        behandlingId: UUID,
        navIdent: String,
        svar: JaNei,
        begrunnelse: String
    ): GyldighetsResultat?

    fun lagreGyldighetsproeving(behandlingId: UUID, gyldighetsproeving: GyldighetsResultat)
    fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode)
    fun settOpprettet(behandlingId: UUID, dryRun: Boolean = true)
    fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean = true)
    fun settBeregnet(behandlingId: UUID, dryRun: Boolean = true)
    fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean = true)
    fun settAttestert(behandlingId: UUID, dryRun: Boolean = true)
    fun settReturnert(behandlingId: UUID, dryRun: Boolean = true)
    fun settIverksatt(behandlingId: UUID, dryRun: Boolean = true)
}

class RealFoerstegangsbehandlingService(
    private val sakDao: SakDao,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val featureToggleService: FeatureToggleService,
    private val klokke: Clock = utcKlokke()
) : FoerstegangsbehandlingService {
    private val logger = LoggerFactory.getLogger(RealFoerstegangsbehandlingService::class.java)

    fun hentBehandling(id: UUID): Foerstegangsbehandling? =
        (behandlingDao.hentBehandling(id) as? Foerstegangsbehandling)?.sjekkEnhet()

    override fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling? {
        return inTransaction {
            hentBehandling(behandling)
        }
    }

    override fun startFoerstegangsbehandling(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning
    ): Foerstegangsbehandling? {
        logger.info("Starter behandling i sak $sakId")
        return inTransaction {
            val sak = requireNotNull(
                sakDao.hentSak(sakId)?.let {
                    listOf(it).filterSakerForEnheter(featureToggleService, Kontekst.get().AppUser).firstOrNull()
                }
            ) {
                "Fant ingen sak med id=$sakId!"
            }

            OpprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakId,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
                persongalleri = persongalleri,
                kilde = Vedtaksloesning.GJENNY,
                merknad = opprettMerknad(sak, persongalleri)
            ).let { opprettBehandling ->
                behandlingDao.opprettBehandling(opprettBehandling)
                hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

                logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

                hentBehandling(opprettBehandling.id)
            }
        }.also { behandling ->
            behandling?.let {
                runBlocking {
                    behandlingHendelser.send(it.id to BehandlingHendelseType.OPPRETTET)
                }
            }
        }
    }

    override fun lagreGyldighetsproeving(
        behandlingId: UUID,
        navIdent: String,
        svar: JaNei,
        begrunnelse: String
    ): GyldighetsResultat? {
        return inTransaction {
            hentBehandling(behandlingId)?.let { behandling ->
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
                                kilde = Grunnlagsopplysning.Saksbehandler(
                                    navIdent,
                                    Tidspunkt.from(klokke.norskKlokke())
                                )
                            )
                        )
                    ),
                    vurdertDato = Tidspunkt(klokke.instant()).toLocalDatetimeNorskTid()
                )

                behandling.lagreGyldighetsproeving(gyldighetsResultat)

                gyldighetsResultat
            }
        }
    }

    override fun lagreGyldighetsproeving(behandlingId: UUID, gyldighetsproeving: GyldighetsResultat) {
        inTransaction {
            hentBehandling(behandlingId)?.lagreGyldighetsproeving(gyldighetsproeving)
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
            hentBehandling(behandlingId)?.lagreKommerBarnetTilgode(kommerBarnetTilgode)
        }
    }

    private fun Foerstegangsbehandling.lagreKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode) {
        this.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .also { behandlingDao.lagreKommerBarnetTilgode(it.id, kommerBarnetTilgode) }
            .also { behandlingDao.lagreStatus(it) }
    }

    override fun settOpprettet(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId)?.tilOpprettet()?.lagreEndring(dryRun)
    }

    override fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean) {
        val behandling = hentFoerstegangsbehandling(behandlingId)?.tilVilkaarsvurdert()

        if (!dryRun) {
            inTransaction {
                behandling?.let {
                    behandlingDao.lagreStatus(it.id, it.status, it.sistEndret)
                }
            }
        }
    }

    override fun settBeregnet(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId)?.tilBeregnet()?.lagreEndring(dryRun)
    }

    override fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId)?.tilFattetVedtak()?.lagreEndring(dryRun)
    }

    override fun settAttestert(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId)?.tilAttestert()?.lagreEndring(dryRun)
    }

    override fun settReturnert(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId)?.tilReturnert()?.lagreEndring(dryRun)
    }

    override fun settIverksatt(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId)?.tilIverksatt()?.lagreEndring(dryRun)
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

    private fun opprettMerknad(sak: Sak, persongalleri: Persongalleri): String? {
        return if (persongalleri.soesken.isEmpty()) {
            null
        } else if (sak.sakType == SakType.BARNEPENSJON) {
            "${persongalleri.soesken.size} søsken"
        } else if (sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            val barnUnder20 = persongalleri.soesken.count { Folkeregisteridentifikator.of(it).getAge() < 20 }

            "$barnUnder20 barn u/20år"
        } else {
            null
        }
    }

    private fun Foerstegangsbehandling?.sjekkEnhet() = this?.let { behandling ->
        listOf(behandling).filterBehandlingerForEnheter(
            featureToggleService,
            Kontekst.get().AppUser
        ).firstOrNull()
    }
}