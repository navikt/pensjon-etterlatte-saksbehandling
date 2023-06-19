package no.nav.etterlatte.behandling.foerstegangsbehandling

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.filterBehandlingerForEnheter
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
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
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.oppgaveny.OppgaveType
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.filterSakerForEnheter
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

interface FoerstegangsbehandlingService {
    fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling?
    fun opprettBehandling(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning
    ): Behandling?

    fun lagreGyldighetsproeving(
        behandlingId: UUID,
        navIdent: String,
        svar: JaNeiMedBegrunnelse
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

class FoerstegangsbehandlingServiceImpl(
    private val oppgaveService: OppgaveServiceNy,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val sakDao: SakDao,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val featureToggleService: FeatureToggleService,
    private val klokke: Clock = utcKlokke()
) : FoerstegangsbehandlingService {
    private val logger = LoggerFactory.getLogger(FoerstegangsbehandlingServiceImpl::class.java)

    fun hentBehandling(id: UUID): Foerstegangsbehandling? =
        (behandlingDao.hentBehandling(id) as? Foerstegangsbehandling)?.sjekkEnhet()

    override fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling? {
        return inTransaction {
            hentBehandling(behandling)
        }
    }

    override fun opprettBehandling(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning
    ): Behandling? {
        logger.info("Starter behandling i sak $sakId")
        val sak = inTransaction {
            requireNotNull(
                sakDao.hentSak(sakId)?.let {
                    listOf(it).filterSakerForEnheter(featureToggleService, Kontekst.get().AppUser).firstOrNull()
                }
            ) {
                "Fant ingen sak med id=$sakId!"
            }
        }
        val harBehandlingerForSak = inTransaction {
            behandlingDao.alleBehandlingerISak(sak.id)
        }

        val harIverksattEllerAttestertBehandling = harBehandlingerForSak.filter { behandling ->
            BehandlingStatus.iverksattEllerAttestert().find { it == behandling.status } != null
        }
        return if (harIverksattEllerAttestertBehandling.isNotEmpty()) {
            revurderingService.opprettRevurdering(
                sakId,
                persongalleri,
                mottattDato,
                Prosesstype.AUTOMATISK,
                Vedtaksloesning.GJENNY,
                "Oppdatert søknad",
                RevurderingAarsak.NY_SOEKNAD
            )
        } else {
            val harBehandlingUnderbehandling = harBehandlingerForSak.filter { behandling ->
                BehandlingStatus.underBehandling().find { it == behandling.status } != null
            }
            opprettFoerstegangsbehandling(harBehandlingUnderbehandling, sak, persongalleri, mottattDato)
        }
    }

    private fun opprettFoerstegangsbehandling(
        harBehandlingUnderbehandling: List<Behandling>,
        sak: Sak,
        persongalleri: Persongalleri,
        mottattDato: String?
    ): Foerstegangsbehandling? {
        return inTransaction {
            harBehandlingUnderbehandling.forEach {
                behandlingDao.lagreStatus(it.id, BehandlingStatus.AVBRUTT, LocalDateTime.now())
            }

            OpprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
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
            }.also { behandling ->
                behandling?.let {
                    grunnlagService.leggInnNyttGrunnlag(it)
                    oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                        referanse = behandling.id.toString(),
                        sakId = sak.id,
                        oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING
                    )
                    behandlingHendelser.sendMeldingForHendelse(it, BehandlingHendelseType.OPPRETTET)
                }
            }
        }
    }

    override fun lagreGyldighetsproeving(
        behandlingId: UUID,
        navIdent: String,
        svar: JaNeiMedBegrunnelse
    ): GyldighetsResultat? {
        return inTransaction {
            hentBehandling(behandlingId)?.let { behandling ->
                val resultat =
                    if (svar.erJa()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
                val gyldighetsResultat = GyldighetsResultat(
                    resultat = resultat,
                    vurderinger = listOf(
                        VurdertGyldighet(
                            navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
                            resultat = resultat,
                            basertPaaOpplysninger = ManuellVurdering(
                                begrunnelse = svar.begrunnelse,
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
                logger.info("behandling ${it.id} i sak: ${it.sak.id} er gyldighetsprøvd. Saktype: ${it.sak.sakType}")
            }
    }

    override fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode) {
        return inTransaction {
            hentBehandling(behandlingId)?.lagreKommerBarnetTilgode(kommerBarnetTilgode)
        }
    }

    private fun Foerstegangsbehandling.lagreKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode) {
        this.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .also { behandlingDao.lagreKommerBarnetTilgode(kommerBarnetTilgode) }
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