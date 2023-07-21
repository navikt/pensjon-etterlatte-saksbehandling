package no.nav.etterlatte.behandling

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.filterSakerForEnheter
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BehandlingFactory(
    private val oppgaveService: OppgaveServiceNy,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val sakDao: SakDao,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val featureToggleService: FeatureToggleService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    fun opprettBehandling(
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
                harIverksattEllerAttestertBehandling.maxBy { it.behandlingOpprettet }.id,
                mottattDato,
                Prosesstype.AUTOMATISK,
                Vedtaksloesning.GJENNY,
                "Oppdatert søknad",
                RevurderingAarsak.NY_SOEKNAD,
                begrunnelse = null // TODO
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
    ): Behandling? {
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

                behandlingDao.hentBehandling(opprettBehandling.id)?.sjekkEnhet()
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
    private fun Behandling?.sjekkEnhet() = this?.let { behandling ->
        listOf(behandling).filterBehandlingerForEnheter(
            featureToggleService,
            Kontekst.get().AppUser
        ).firstOrNull()
    }
}