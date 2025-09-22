package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.GrunnlagUtils.opplysningsbehov
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.hentAlder
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class BehandlingFactory(
    private val oppgaveService: OppgaveService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val gyldighetsproevingService: GyldighetsproevingService,
    private val sakService: SakService,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val behandlingInfoService: BehandlingInfoService,
    private val tilgangsService: OppdaterTilgangService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /*
     * Brukes av frontend for å kunne opprette sak og behandling for en Gosys-oppgave.
     */
    fun opprettSakOgBehandlingForOppgave(
        request: NyBehandlingRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Behandling {
        logger.info("Oppretter sak og behandling for persongalleri: ${request.persongalleri}, saktype ${request.sakType}")
        val soeker = request.persongalleri.soeker

        val sak = inTransaction { sakService.finnEllerOpprettSakMedGrunnlag(soeker, request.sakType) }

        if (request.enhet != null && sak.enhet != request.enhet) {
            val baOmSpesialEnhet = Enheter.erSpesialTilgangsEnheter(request.enhet!!)
            if (baOmSpesialEnhet) {
                throw BaOmSpesialEnhet()
            } else {
                logger.warn("Enheten fra requesten ble ulik enn opprett/finn sak sa, ba ikke om spesialenhet. Ba om enhet ${request.enhet}")
                if (Enheter.erSpesialTilgangsEnheter(sak.enhet)) {
                    sikkerLogg.info("Satt spesisalenhet: ${sak.enhet} for sakid: ${sak.id} ")
                }
            }
        }

        val behandling =
            inTransaction {
                opprettBehandling(
                    sak.id,
                    request.persongalleri,
                    request.mottattDato,
                    request.kilde ?: Vedtaksloesning.GJENNY,
                    request = hentDataForOpprettBehandling(sak.id),
                    opprinnelse = BehandlingOpprinnelse.JOURNALFOERING,
                ).also {
                    if (request.kilde == Vedtaksloesning.GJENOPPRETTA) {
                        oppgaveService
                            .hentOppgaverForSak(sak.id, OppgaveType.GJENOPPRETTING_ALDERSOVERGANG)
                            .find { !it.erAvsluttet() }
                            ?.let {
                                oppgaveService.ferdigstillOppgave(it.id, brukerTokenInfo)
                            }
                    }
                }
            }.also { it.sendMeldingForHendelse() }
                .behandling

        val gyldighetsvurdering =
            GyldighetsResultat(
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
                emptyList(),
                Tidspunkt.now().toLocalDatetimeUTC(),
            )

        gyldighetsproevingService.lagreGyldighetsproeving(behandling.id, gyldighetsvurdering)

        val mottattDato = LocalDateTime.parse(request.mottattDato)
        val kilde = Grunnlagsopplysning.Privatperson(soeker, mottattDato.toTidspunkt())

        val opplysninger =
            mutableListOf(
                lagOpplysning(Opplysningstype.SPRAAK, kilde, request.spraak.toJsonNode()),
                lagOpplysning(
                    Opplysningstype.SOEKNAD_MOTTATT_DATO,
                    kilde,
                    SoeknadMottattDato(mottattDato).toJsonNode(),
                ),
            )
        if (request.kilde?.foerstOpprettaIPesys() == true) {
            opplysninger.add(lagOpplysning(Opplysningstype.FORELDRELOES, kilde, request.foreldreloes.toJsonNode()))
            opplysninger.add(lagOpplysning(Opplysningstype.UFOERE, kilde, request.ufoere.toJsonNode()))
        }

        grunnlagService.lagreNyeSaksopplysninger(sak.id, behandling.id, opplysninger)

        return behandling
    }

    fun opprettBehandling(
        sakId: SakId,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning,
        request: DataHentetForOpprettBehandling,
        opprinnelse: BehandlingOpprinnelse,
    ): BehandlingOgOppgave {
        logger.info("Starter behandling i sak $sakId")
        val prosessType = Prosesstype.MANUELL
        val harBehandlingUnderbehandling =
            request.alleBehandlingerISak.filter { behandling ->
                BehandlingStatus.underBehandling().find { it == behandling.status } != null
            }
        if (harBehandlingUnderbehandling.isNotEmpty()) {
            throw UgyldigForespoerselException(
                "HAR_AAPEN_BEHANDLING",
                "Sak $sakId har allerede en åpen " +
                    "behandling. Denne må avbrytes eller ferdigbehandles før ny behandling kan opprettes.",
            )
        }
        return if (request.harIverksattBehandling()) {
            if (kilde == Vedtaksloesning.PESYS || kilde == Vedtaksloesning.GJENOPPRETTA) {
                throw ManuellMigreringHarEksisterendeIverksattBehandling()
            }
            val forrigeBehandling = request.iverkSattebehandlinger().maxBy { it.behandlingOpprettet }
            revurderingService
                .opprettRevurdering(
                    sakId = sakId,
                    persongalleri = persongalleri,
                    forrigeBehandling = forrigeBehandling,
                    mottattDato = mottattDato,
                    prosessType = Prosesstype.MANUELL,
                    kilde = kilde,
                    revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                    virkningstidspunkt = null,
                    begrunnelse = null,
                    saksbehandlerIdent = null,
                    frist = null,
                    opprinnelse = opprinnelse,
                ).oppdater()
                .let { BehandlingOgOppgave(it, null) }
        } else {
            val behandling =
                opprettFoerstegangsbehandling(
                    harBehandlingUnderbehandling,
                    request.sak,
                    mottattDato,
                    kilde,
                    prosessType,
                    opprinnelse = opprinnelse,
                )
            runBlocking {
                grunnlagService.opprettGrunnlag(
                    behandling.id,
                    opplysningsbehov(behandling.sak, persongalleri),
                )
            }

            val oppgave =
                opprettOppgaveForFoerstegangsbehandling(
                    referanse = behandling.id.toString(),
                    sakId = request.sak.id,
                    oppgaveKilde =
                        if (kilde == Vedtaksloesning.GJENOPPRETTA) {
                            OppgaveKilde.GJENOPPRETTING
                        } else {
                            OppgaveKilde.BEHANDLING
                        },
                    merknad =
                        when (kilde) {
                            Vedtaksloesning.GJENOPPRETTA -> "Manuell gjenopprettelse av opphørt sak i Pesys"
                            else ->
                                opprettMerknad(
                                    request.sak,
                                    persongalleri,
                                    behandling.id,
                                )
                        },
                    gruppeId = persongalleri.avdoed.firstOrNull(),
                    soeknadMottattDato = behandling.soeknadMottattDato,
                )

            val grunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(sakId)
            tilgangsService.haandtergraderingOgEgenAnsatt(sakId, persongalleri, grunnlag)

            return BehandlingOgOppgave(behandling, oppgave) {
                behandlingHendelser.sendMeldingForHendelseStatistikk(
                    behandling.toStatistikkBehandling(persongalleri),
                    BehandlingHendelseType.OPPRETTET,
                )
            }
        }
    }

    fun opprettOmgjoeringAvslag(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        omgjoeringRequest: OmgjoeringRequest,
    ): Behandling {
        val behandlingerForOmgjoering =
            inTransaction {
                val sak = sakService.finnSak(sakId) ?: throw GenerellIkkeFunnetException()
                val behandlingerISak = behandlingDao.hentBehandlingerForSak(sakId)
                val foerstegangsbehandlinger =
                    behandlingerISak.filter { it.type == BehandlingType.FØRSTEGANGSBEHANDLING }
                if (foerstegangsbehandlinger.isEmpty()) {
                    throw AvslagOmgjoering.IngenFoerstegangsbehandling()
                }
                val foerstegangsbehandlingerIkkeAvslaattAvbrutt =
                    foerstegangsbehandlinger.filter {
                        it.status !in
                            listOf(
                                BehandlingStatus.AVBRUTT,
                                BehandlingStatus.AVSLAG,
                            )
                    }
                if (foerstegangsbehandlingerIkkeAvslaattAvbrutt.isNotEmpty()) {
                    throw AvslagOmgjoering.FoerstegangsbehandlingFeilStatus(
                        sakId,
                        foerstegangsbehandlingerIkkeAvslaattAvbrutt.first().id,
                    )
                }
                if (behandlingerISak.any { it.status.aapenBehandling() }) {
                    throw AvslagOmgjoering.HarAapenBehandling()
                }

                val omgjoeringsOppgaver = oppgaveService.hentOppgaverForSak(sakId, OppgaveType.OMGJOERING)
                val klageId =
                    if (omgjoeringRequest.omgjoeringsOppgaveId != null) {
                        val omgjoeringsOppgave =
                            omgjoeringsOppgaver.find {
                                it.erIkkeAvsluttet() &&
                                    it.id == omgjoeringRequest.omgjoeringsOppgaveId &&
                                    it.saksbehandler?.ident == saksbehandler.ident
                            }
                        if (omgjoeringsOppgave == null) {
                            throw AvslagOmgjoering.HarIkkeOmgjoeringsoppgaveUnderBehandling()
                        }
                        omgjoeringsOppgave.referanse
                    } else {
                        if (omgjoeringsOppgaver.any { !it.erAvsluttet() }) throw AvslagOmgjoering.AapenOmgjoeringISak()
                        null
                    }

                val sisteAvslaatteBehandling =
                    behandlingerISak
                        .filter { it.status == BehandlingStatus.AVSLAG }
                        .maxByOrNull { it.behandlingOpprettet }

                val foerstegangsbehandlingViOmgjoerer =
                    foerstegangsbehandlinger.maxBy { it.behandlingOpprettet }

                val nyFoerstegangsbehandling =
                    opprettFoerstegangsbehandling(
                        behandlingerUnderBehandling = emptyList(),
                        sak = sak,
                        mottattDato = foerstegangsbehandlingViOmgjoerer.mottattDato().toString(),
                        kilde = Vedtaksloesning.GJENNY,
                        prosessType = Prosesstype.MANUELL,
                        relatertBehandlingsId = klageId,
                        opprinnelse = BehandlingOpprinnelse.SAKSBEHANDLER,
                    )

                if (omgjoeringRequest.erSluttbehandlingUtland) {
                    behandlingInfoService.lagreErOmgjoeringSluttbehandlingUtland(nyFoerstegangsbehandling)
                }

                if (omgjoeringRequest.skalKopiere && sisteAvslaatteBehandling != null) {
                    kopierFoerstegangsbehandlingOversikt(sisteAvslaatteBehandling, nyFoerstegangsbehandling.id)
                } else {
                    val nyGyldighetsproeving =
                        GyldighetsResultat(
                            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
                            emptyList(),
                            Tidspunkt.now().toLocalDatetimeUTC(),
                        )

                    behandlingDao.lagreGyldighetsproeving(nyFoerstegangsbehandling.id, nyGyldighetsproeving)
                }

                if (omgjoeringRequest.omgjoeringsOppgaveId != null) {
                    oppgaveService.ferdigstillOppgave(omgjoeringRequest.omgjoeringsOppgaveId, saksbehandler)
                }

                val merknad =
                    if (omgjoeringRequest.omgjoeringsOppgaveId ==
                        null
                    ) {
                        "Omgjøring av førstegangsbehandling"
                    } else {
                        "Omgjøring på grunn av klage"
                    }
                val oppgave =
                    opprettOppgaveForFoerstegangsbehandling(
                        referanse = nyFoerstegangsbehandling.id.toString(),
                        sakId = nyFoerstegangsbehandling.sak.id,
                        oppgaveKilde = OppgaveKilde.BEHANDLING,
                        merknad = merknad,
                        gruppeId = null,
                        soeknadMottattDato = nyFoerstegangsbehandling.soeknadMottattDato,
                    )
                oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler.ident)

                OmgjoerBehandling(nyFoerstegangsbehandling, sisteAvslaatteBehandling, foerstegangsbehandlingViOmgjoerer)
            }

        val persongalleri = grunnlagService.hentPersongalleri(sakId)!!

        inTransaction {
            runBlocking {
                grunnlagService.opprettGrunnlag(
                    behandlingerForOmgjoering.nyFoerstegangsbehandling.id,
                    opplysningsbehov(behandlingerForOmgjoering.nyFoerstegangsbehandling.sak, persongalleri),
                )
            }
        }

        if (omgjoeringRequest.skalKopiere && behandlingerForOmgjoering.sisteAvslaatteBehandling != null) {
            // Dette må skje etter at grunnlag er lagt inn da det trengs i kopiering
            inTransaction {
                vilkaarsvurderingService.kopierVilkaarsvurdering(
                    behandlingId = behandlingerForOmgjoering.nyFoerstegangsbehandling.id,
                    kopierFraBehandling = behandlingerForOmgjoering.sisteAvslaatteBehandling.id,
                    brukerTokenInfo = saksbehandler,
                )
            }
        }

        behandlingHendelser.sendMeldingForHendelseStatistikk(
            behandlingerForOmgjoering.nyFoerstegangsbehandling.toStatistikkBehandling(persongalleri),
            BehandlingHendelseType.OPPRETTET,
        )
        return behandlingerForOmgjoering.nyFoerstegangsbehandling
    }

    private fun opprettOppgaveForFoerstegangsbehandling(
        referanse: String,
        sakId: SakId,
        oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
        soeknadMottattDato: LocalDateTime?,
        merknad: String?,
        gruppeId: String?,
    ): OppgaveIntern {
        oppgaveService.avbrytAapneOppgaverMedReferanse(referanse)

        return oppgaveService.opprettOppgave(
            referanse = referanse,
            sakId = sakId,
            kilde = oppgaveKilde,
            type = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = merknad,
            gruppeId = gruppeId,
            frist = soeknadMottattDato?.plusMonths(1L)?.toNorskTidspunkt(),
        )
    }

    private fun kopierFoerstegangsbehandlingOversikt(
        sisteAvslaatteBehandling: Behandling,
        nyFoerstegangsbehandlingId: UUID,
    ) {
        sisteAvslaatteBehandling.kommerBarnetTilgode?.let {
            kommerBarnetTilGodeService.lagreKommerBarnetTilgode(it.copy(behandlingId = nyFoerstegangsbehandlingId))
        }
        sisteAvslaatteBehandling.virkningstidspunkt?.let {
            behandlingDao.lagreNyttVirkningstidspunkt(
                nyFoerstegangsbehandlingId,
                it,
            )
        }
        sisteAvslaatteBehandling.utlandstilknytning?.let {
            behandlingDao.lagreUtlandstilknytning(
                nyFoerstegangsbehandlingId,
                it,
            )
        }
        sisteAvslaatteBehandling
            .gyldighetsproeving()
            ?.let { behandlingDao.lagreGyldighetsproeving(nyFoerstegangsbehandlingId, it) }

        sisteAvslaatteBehandling.opphoerFraOgMed?.let { behandlingDao.lagreOpphoerFom(nyFoerstegangsbehandlingId, it) }

        sisteAvslaatteBehandling.boddEllerArbeidetUtlandet?.let {
            behandlingDao.lagreBoddEllerArbeidetUtlandet(
                nyFoerstegangsbehandlingId,
                it,
            )
        }
    }

    private fun opprettMerknad(
        sak: Sak,
        persongalleri: Persongalleri,
        behandlingId: UUID,
    ): String? =
        if (persongalleri.soesken.isEmpty()) {
            null
        } else if (sak.sakType == SakType.BARNEPENSJON) {
            "${persongalleri.soesken.size} søsken"
        } else if (sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            val grunnlag: Grunnlag? =
                grunnlagService
                    .hentOpplysningsgrunnlag(behandlingId)

            val avdoede = grunnlag?.hentAvdoede()?.firstOrNull()
            val barn = avdoede?.hentAvdoedesbarn()?.verdi?.avdoedesBarn

            val barnUnder20 = barn?.count { it.foedselsdato?.hentAlder()?.let { it < 20 } ?: false }

            "$barnUnder20 barn u/20år"
        } else {
            null
        }

    internal fun hentDataForOpprettBehandling(sakId: SakId): DataHentetForOpprettBehandling {
        val sak =
            krevIkkeNull(sakService.finnSak(sakId)) {
                "Fant ingen sak med id=$sakId!"
            }

        val harBehandlingerForSak =
            behandlingDao.hentBehandlingerForSak(sak.id)

        return DataHentetForOpprettBehandling(
            sak = sak,
            alleBehandlingerISak = harBehandlingerForSak,
        )
    }

    fun finnGjeldendeEnhet(
        persongalleri: Persongalleri,
        sakType: SakType,
    ) = sakService.finnGjeldendeEnhet(persongalleri.soeker, sakType)

    private fun opprettFoerstegangsbehandling(
        behandlingerUnderBehandling: List<Behandling>,
        sak: Sak,
        mottattDato: String?,
        kilde: Vedtaksloesning,
        prosessType: Prosesstype,
        opprinnelse: BehandlingOpprinnelse,
        relatertBehandlingsId: String? = null,
    ): Behandling {
        if (behandlingerUnderBehandling.isNotEmpty()) {
            throw InternfeilException(
                "Det skal ikke være mulig å komme til opprettelse av førstegangsbehandling med " +
                    "åpne behandlinger i saken.",
            )
        }

        return OpprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak.id,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
            vedtaksloesning = kilde,
            prosesstype = prosessType,
            sendeBrev = true,
            relatertBehandlingId = relatertBehandlingsId,
            opprinnelse = opprinnelse,
        ).let { opprettBehandling ->
            behandlingDao.opprettBehandling(opprettBehandling)
            hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

            logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

            behandlingDao.hentBehandling(opprettBehandling.id)
                ?: throw InternfeilException(
                    "Behandlingen vi akkurat opprettet finnes ikke i databasen. Id burde være " +
                        "${opprettBehandling.id}, i sak ${opprettBehandling.sakId}",
                )
        }
    }
}

data class OmgjoerBehandling(
    val nyFoerstegangsbehandling: Behandling,
    val sisteAvslaatteBehandling: Behandling?,
    val foerstegangsbehandlingViOmgjoerer: Behandling,
)

data class BehandlingOgOppgave(
    val behandling: Behandling,
    val oppgave: OppgaveIntern?,
    val sendMeldingForHendelse: () -> Unit = {},
)

class ManuellMigreringHarEksisterendeIverksattBehandling :
    UgyldigForespoerselException(
        code = "MANUELL_MIGRERING_EKSISTERENDE_IVERKSATT",
        detail = "Det eksisterer allerede en sak med en iverksatt behandling for angitt søker",
    )

class BaOmSpesialEnhet :
    UgyldigForespoerselException(
        code = "BA_OM_SPESIAL_ENHET_MAA_GJORES_MANUELT",
        detail =
            "Den forespurte enheten kan ikke settes, opprett sak i porten. " +
                "Det må gjøres en manuel avgjørelse her da systemene ikke er enig.",
    )

sealed class AvslagOmgjoering {
    class IngenFoerstegangsbehandling :
        UgyldigForespoerselException(
            "INGEN_FOERSTEGANGSBEHANDLING",
            "Fant ingen førstegangsbehandling i saken der førstegangsbehandling skulle omgjøres",
        )

    class FoerstegangsbehandlingFeilStatus(
        sakId: SakId,
        behandlingId: UUID,
    ) : UgyldigForespoerselException(
            "FOERSTEGANGSBEHANDLING_UGYLDIG_STATUS",
            "Kan ikke omgjøre førstegangsbehandling i sak $sakId, siden $behandlingId er en " +
                "førstegangsbehandling i saken som ikke er avslått eller avbrutt",
        )

    class HarAapenBehandling :
        UgyldigForespoerselException(
            "HAR_AAPEN_BEHANDLING",
            "For å omgjøre en førstegangsbehandling må alle andre behandlinger i saken være lukket.",
        )

    class HarIkkeOmgjoeringsoppgaveUnderBehandling :
        UgyldigForespoerselException(
            "HAR_IKKE_OMGJOERINGS_OPPGAVE_UNDER_BEHANDLING",
            "Du har ikke omgjøringsoppgaven for avslaget tilordnet deg, plukk oppgaven og prøv igjen",
        )

    class AapenOmgjoeringISak :
        UgyldigForespoerselException(
            "AAPEN_OMGJOERING_I_SAK",
            "Det ligger en åpen omgjøringsoppgave i saken, omgjøring av førstegangsbehandling skal gjøres fra omgjøringsoppgaven",
        )
}

fun Vedtaksloesning.foerstOpprettaIPesys() = this == Vedtaksloesning.PESYS || this == Vedtaksloesning.GJENOPPRETTA

data class DataHentetForOpprettBehandling(
    val sak: Sak,
    val alleBehandlingerISak: List<Behandling>,
) {
    fun harIverksattBehandling() =
        alleBehandlingerISak.any { behandling ->
            BehandlingStatus.IVERKSATT == behandling.status
        }

    fun iverkSattebehandlinger() = alleBehandlingerISak.filter { BehandlingStatus.IVERKSATT == it.status }
}
