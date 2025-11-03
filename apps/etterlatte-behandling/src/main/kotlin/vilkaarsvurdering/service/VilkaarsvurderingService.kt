package no.nav.etterlatte.vilkaarsvurdering.service

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.erPaaNyttRegelverk
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingMedBehandlingGrunnlagsversjon
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.kopier
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.VilkaartypePair
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingDao
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar1967
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar2024
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.OmstillingstoenadVilkaar
import org.slf4j.LoggerFactory
import java.util.UUID

class VirkningstidspunktIkkeSattException(
    behandlingId: UUID,
) : RuntimeException("Virkningstidspunkt ikke satt for behandling $behandlingId")

data class PeriodisertVilkaarsvurdering(
    val vilkaarsvurdering: Vilkaarsvurdering,
    val peroide: Periode
) {

    init {

    }
}


class VilkaarsvurderingService(
    private val repository: VilkaarsvurderingDao,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val behandlingStatus: BehandlingStatusService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Deprecated("Bad")
    fun hentVilkaarsvurdering(behandlingId: UUID): Vilkaarsvurdering? = repository.hent(behandlingId)

    fun hentVilkaarsvurderingPerioder(behandlingId: UUID): List<PeriodisertVilkaarsvurdering> {

    }


    fun erMigrertYrkesskadefordel(behandlingId: UUID): Boolean {
        val hentBehandling = behandlingService.hentBehandling(behandlingId)!!
        return repository.hentMigrertYrkesskadefordel(hentBehandling.sak.id)
    }

    fun harRettUtenTidsbegrensning(behandlingId: UUID): Boolean =
        repository
            .hent(behandlingId)
            ?.vilkaar
            ?.filter { it.hovedvilkaar.type == VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING }
            ?.any { it.hovedvilkaar.resultat == Utfall.OPPFYLT }
            ?: false

    fun hentBehandlingensGrunnlag(behandlingId: UUID): Grunnlag = hentDataForVilkaarsvurdering(behandlingId).second

    fun oppdaterTotalVurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        resultat: VilkaarsvurderingResultat,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId)
            val vilkaarsvurdering = hentVilkaarsvurdering(behandlingId) ?: throw VilkaarsvurderingIkkeFunnet()
            val virkningstidspunkt =
                behandling.virkningstidspunkt?.dato?.atDay(1)
                    ?: throw IllegalStateException("Virkningstidspunkt må være satt for å sette en vurdering")
            val oppdatertVilkaarsvurdering =
                repository.lagreVilkaarsvurderingResultat(
                    behandlingId = behandlingId,
                    vilkaarsvurderingId = vilkaarsvurdering.id,
                    virkningstidspunkt = virkningstidspunkt,
                    resultat = resultat,
                )
            if (vilkaarsvurdering.grunnlagVersjon != grunnlag.metadata.versjon) {
                repository.oppdaterGrunnlagsversjon(behandlingId, grunnlag.metadata.versjon)
            }
            behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo, false)
            VilkaarsvurderingMedBehandlingGrunnlagsversjon(oppdatertVilkaarsvurdering, grunnlag.metadata.versjon)
        }

    fun slettTotalVurdering(
        behandlingId: UUID,
        vilkaarsvurderingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vilkaarsvurdering {
        behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, true)
        val vilkaarsvurdering = repository.slettVilkaarsvurderingResultat(behandlingId)
        behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, false)
        return vilkaarsvurdering
    }

    fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val vilkaarsvurdering = repository.hent(behandlingId)
            if (vilkaarsvurdering?.resultat != null) {
                throw VilkaarsvurderingTilstandException(
                    "Kan ikke endre et vilkår (${vurdertVilkaar.vilkaarId}) på en vilkårsvurdering som har et resultat. ",
                    vilkaarsvurdering,
                )
            }
            repository.oppdaterVurderingPaaVilkaar(behandlingId, vurdertVilkaar)
        }

    fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vilkaarId: UUID,
    ): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val vilkaarsvurdering = repository.hent(behandlingId)
            if (vilkaarsvurdering?.resultat != null) {
                throw VilkaarsvurderingTilstandException(
                    "Kan ikke slette et vilkår ($vilkaarId) på en vilkårsvurdering som har et resultat. ",
                    vilkaarsvurdering,
                )
            }

            repository.slettVilkaarResultat(behandlingId, vilkaarId)
        }

    private fun validerTidligereVilkaarsvurdering(tidligereVilkaarsvurdering: Vilkaarsvurdering) {
        if (tidligereVilkaarsvurdering.resultat == null) {
            logger.warn("Mangler resultat for vilkårsvurdering")
        }

        if (tidligereVilkaarsvurdering.vilkaar.isEmpty()) {
            logger.warn("Mangler vilkår for vilkårsvurdering")
        }

        if (tidligereVilkaarsvurdering.vilkaar.any { it.vurdering == null }) {
            logger.warn("Mangler vurdering for delvilkår i vilkårsvurdering")
        }
    }

    fun kopierVilkaarsvurdering(
        behandlingId: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        kopierResultat: Boolean = true,
        kopierKunVilkaarGjeldendeAvdoede: Boolean = false,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            logger.info("Oppretter og kopierer vilkårsvurdering for $behandlingId fra $kopierFraBehandling")
            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId)
            val tidligereVilkaarsvurdering =
                repository.hent(kopierFraBehandling)
                    ?: throw NullPointerException("Fant ikke vilkårsvurdering fra behandling $kopierFraBehandling")

            val virkningstidspunkt =
                behandling.virkningstidspunkt ?: throw VirkningstidspunktIkkeSattException(behandlingId)

            validerTidligereVilkaarsvurdering(tidligereVilkaarsvurdering)

            val vilkaar =
                when {
                    behandling.revurderingsaarsak() == Revurderingaarsak.REGULERING ->
                        tidligereVilkaarsvurdering.vilkaar.kopier()

                    else ->
                        oppdaterVilkaar(
                            kopierteVilkaar = tidligereVilkaarsvurdering.vilkaar.kopier(kopierKunVilkaarGjeldendeAvdoede),
                            behandling = behandling,
                            virkningstidspunkt = virkningstidspunkt,
                        )
                }

            val nyVilkaarsvurdering =
                repository.kopierVilkaarsvurdering(
                    nyVilkaarsvurdering =
                        Vilkaarsvurdering(
                            behandlingId = behandlingId,
                            grunnlagVersjon = grunnlag.metadata.versjon,
                            virkningstidspunkt = virkningstidspunkt.dato,
                            vilkaar = vilkaar,
                            resultat = tidligereVilkaarsvurdering.resultat,
                        ),
                    kopiertFraId = tidligereVilkaarsvurdering.id,
                )

            // TODO denne har blitt rimelig vanskelig å skjønne seg helt på - bør skrives om
            // Hvis minst ett av vilkårene mangler vurdering - slett vilkårsvurderingresultat
            if (!kopierResultat ||
                (
                    (
                        behandling.prosesstype != Prosesstype.AUTOMATISK ||
                            // TODO denne kan fjernes når aktivitetsplikt ikke bruker Prosesstype.AUTOMATISK
                            behandling.revurderingsaarsak() == Revurderingaarsak.AKTIVITETSPLIKT
                    ) &&
                        nyVilkaarsvurdering.vilkaar.any { v -> v.vurdering == null }
                )
            ) {
                if (behandling.revurderingsaarsak() == Revurderingaarsak.ETTEROPPGJOER) {
                    behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo, dryRun = false)
                    VilkaarsvurderingMedBehandlingGrunnlagsversjon(nyVilkaarsvurdering, grunnlag.metadata.versjon)
                } else {
                    VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                        repository.slettVilkaarsvurderingResultat(nyVilkaarsvurdering.behandlingId),
                        grunnlag.metadata.versjon,
                    )
                }
            } else {
                behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo, dryRun = false)
                VilkaarsvurderingMedBehandlingGrunnlagsversjon(nyVilkaarsvurdering, grunnlag.metadata.versjon)
            }
        }

    // Her legges det til nye vilkår og det filtreres bort vilkår som ikke lenger er aktuelle.
    // Oppdatering av vilkår med endringer er ennå ikke støttet.
    private fun oppdaterVilkaar(
        kopierteVilkaar: List<Vilkaar>,
        behandling: Behandling,
        virkningstidspunkt: Virkningstidspunkt,
    ): List<Vilkaar> {
        val gjeldendeVilkaarForVirkningstidspunkt =
            finnVilkaarForNyVilkaarsvurdering(
                virkningstidspunkt,
                behandling.type,
                behandling.sak.sakType,
            )

        val nyeHovedvilkaarTyper =
            gjeldendeVilkaarForVirkningstidspunkt
                .map { it.hovedvilkaar.type }
                .subtract(kopierteVilkaar.map { it.hovedvilkaar.type }.toSet())

        val slettetHovedvilkaarTyper =
            kopierteVilkaar
                .map { it.hovedvilkaar.type }
                .subtract(gjeldendeVilkaarForVirkningstidspunkt.map { it.hovedvilkaar.type }.toSet())

        val nyeVilkaar =
            gjeldendeVilkaarForVirkningstidspunkt
                .filter { it.hovedvilkaar.type in nyeHovedvilkaarTyper }

        return kopierteVilkaar.filterNot { it.hovedvilkaar.type in slettetHovedvilkaarTyper } + nyeVilkaar
    }

    fun opprettVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        kopierVedRevurdering: Boolean = true,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            repository.hent(behandlingId)?.let {
                throw IllegalArgumentException("Vilkårsvurdering finnes allerede for behandling $behandlingId")
            }

            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId)

            val virkningstidspunkt =
                behandling.virkningstidspunkt
                    ?: throw VirkningstidspunktIkkeSattException(behandlingId)

            logger.info(
                "Oppretter vilkårsvurdering for behandling ($behandlingId) med sakType ${behandling.sak.sakType} og " +
                    "behandlingType ${behandling.type}",
            )

            when (behandling.type) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                        opprettNyVilkaarsvurdering(grunnlag, virkningstidspunkt, behandling, behandlingId),
                        grunnlag.metadata.versjon,
                    )
                }

                BehandlingType.REVURDERING -> {
                    if (kopierVedRevurdering) {
                        logger.info("Kopierer vilkårsvurdering for behandling $behandlingId fra forrige behandling")
                        val sisteIverksatteBehandling = behandlingService.hentSisteIverksatteBehandling(behandling.sak.id)!!
                        VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                            kopierVilkaarsvurdering(
                                behandlingId,
                                sisteIverksatteBehandling.id,
                                brukerTokenInfo,
                            ).vilkaarsvurdering,
                            grunnlag.metadata.versjon,
                        )
                    } else {
                        VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                            opprettNyVilkaarsvurdering(grunnlag, virkningstidspunkt, behandling, behandlingId),
                            grunnlag.metadata.versjon,
                        )
                    }
                }
            }
        }

    fun slettVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, true)
        val vilkaarsvurdering =
            repository.hent(behandlingId)
                ?: throw IllegalStateException("Vilkårsvurderingen eksisterer ikke")
        repository.slettVilkaarvurdering(vilkaarsvurdering.id)
        behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, false)
    }

    private fun opprettNyVilkaarsvurdering(
        grunnlag: Grunnlag,
        virkningstidspunkt: Virkningstidspunkt,
        behandling: Behandling,
        behandlingId: UUID,
    ): Vilkaarsvurdering {
        val vilkaar =
            finnVilkaarForNyVilkaarsvurdering(
                virkningstidspunkt,
                behandling.type,
                behandling.sak.sakType,
            )

        return repository.opprettVilkaarsvurdering(
            Vilkaarsvurdering(
                behandlingId = behandlingId,
                vilkaar = vilkaar,
                virkningstidspunkt = virkningstidspunkt.dato,
                grunnlagVersjon = grunnlag.metadata.versjon,
            ),
        )
    }

    private fun finnVilkaarForNyVilkaarsvurdering(
        virkningstidspunkt: Virkningstidspunkt,
        behandlingType: BehandlingType,
        sakType: SakType,
    ): List<Vilkaar> =
        when (sakType) {
            SakType.BARNEPENSJON ->
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    BehandlingType.REVURDERING,
                    ->
                        if (virkningstidspunkt.erPaaNyttRegelverk()) {
                            BarnepensjonVilkaar2024.inngangsvilkaar()
                        } else {
                            BarnepensjonVilkaar1967.inngangsvilkaar()
                        }
                }

            SakType.OMSTILLINGSSTOENAD ->
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> OmstillingstoenadVilkaar.inngangsvilkaar()
                    BehandlingType.REVURDERING -> OmstillingstoenadVilkaar.loependevilkaar()
                }
        }

    fun sjekkGyldighetOgOppdaterBehandlingStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val behandling = oppdaterGrunnlagsversjon(behandlingId)
            val vilkaarsvurdering =
                repository.hent(behandlingId)
                    ?: throw VilkaarsvurderingIkkeFunnet()

            val virkningstidspunktFraBehandling =
                behandling.virkningstidspunkt?.dato
                    ?: throw BehandlingVirkningstidspunktIkkeFastsatt()

            if (vilkaarsvurdering.resultat == null) {
                throw VilkaarsvurderingManglerResultat()
            }

            if (vilkaarsvurdering.virkningstidspunkt != virkningstidspunktFraBehandling) {
                throw VirkningstidspunktSamsvarerIkke()
            }

            // Dersom forrige steg (oversikt) har blitt endret vil statusen være OPPRETTET. Når man trykker videre
            // fra vilkårsvurdering skal denne validere tilstand og sette status VILKAARSVURDERT.
            if (behandling.status in listOf(BehandlingStatus.OPPRETTET)) {
                behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo, false)
                true
            } else {
                false
            }
        }

    private fun oppdaterGrunnlagsversjon(behandlingId: UUID): Behandling {
        val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId)
        repository.oppdaterGrunnlagsversjon(
            behandlingId = behandlingId,
            grunnlagVersjon = grunnlag.metadata.versjon,
        )
        return behandling
    }

    private fun <T> tilstandssjekkFoerKjoering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        block: () -> T,
    ): T {
        try {
            behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo)
        } catch (e: Exception) {
            throw BehandlingstilstandException(e.message, e) // Denne er liksom vekk men catches i vv routes
        }
        return block()
    }

    private fun hentDataForVilkaarsvurdering(behandlingId: UUID): Pair<Behandling, Grunnlag> {
        val behandling = behandlingService.hentBehandling(behandlingId)!!
        val grunnlag = grunnlagService.hentOpplysningsgrunnlag(behandlingId)!!
        return Pair(behandling, grunnlag)
    }

    fun hentVilkaartyper(behandlingId: UUID) =
        finnRelevanteTyper(behandlingId)
            .map { it.hovedvilkaar }
            .map { it.type }
            .map { VilkaartypePair(name = it.name, tittel = it.tittel) }

    private fun finnRelevanteTyper(behandlingId: UUID): List<Vilkaar> {
        val behandling = behandlingService.hentBehandling(behandlingId)!!
        if (behandling.sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            return when (behandling.type) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> OmstillingstoenadVilkaar.inngangsvilkaar()
                BehandlingType.REVURDERING -> OmstillingstoenadVilkaar.loependevilkaar()
            }
        }
        return if (behandling.virkningstidspunkt!!.erPaaNyttRegelverk()) {
            BarnepensjonVilkaar2024.inngangsvilkaar()
        } else {
            BarnepensjonVilkaar1967.inngangsvilkaar()
        }
    }

    fun finnBehandlingMedVilkaarsvurderingForSammeAvdoede(behandlingId: UUID): UUID? {
        val gjeldendeBehandling =
            behandlingService.hentBehandling(behandlingId) ?: throw BehandlingIkkeFunnet(behandlingId)
        if (gjeldendeBehandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) {
            logger.info("Støtter ikke å kopiere vilkår i vilkårsvurdering for annet enn førstegangsbehandling")
            return null
        }

        val gjeldendeVilkaarsvurdering = hentVilkaarsvurdering(behandlingId)
        if (gjeldendeVilkaarsvurdering?.resultat != null) {
            logger.info("Støtter ikke å kopiere vilkår i vilkårsvurdering som har utfylt resultat")
            return null
        }

        val avdoedeForGjeldendeBehandling: List<Folkeregisteridentifikator> =
            grunnlagService
                .hentPersongalleri(gjeldendeBehandling.sak.id)!!
                .avdoed
                .map { Folkeregisteridentifikator.of(it) }

        if (avdoedeForGjeldendeBehandling.isEmpty()) {
            logger.info("Ingen avdøde funnet for gjeldende behandling $behandlingId, det er ikke aktuelt å kopiere vilkår")
            return null
        }

        logger.info("${avdoedeForGjeldendeBehandling.size} avdøde er funnet for gjeldende behandling $behandlingId")
        sikkerlogger().info(
            "Avdøde ${avdoedeForGjeldendeBehandling.joinToString(", ")} er funnet for gjeldende behandling $behandlingId",
        )

        return behandlingerMedVilkaarsvurderingForAvdoede(
            avdoedeForGjeldendeBehandling.toSet(),
            gjeldendeBehandling.sak.id,
        ).firstOrNull { it != behandlingId }
    }

    private fun behandlingerMedVilkaarsvurderingForAvdoede(
        avdoedeForGjeldendeBehandling: Set<Folkeregisteridentifikator>,
        gjeldendeSak: SakId,
    ): List<UUID> {
        logger.info("Henter saker med felles avdøde fra grunnlag")
        val kandidatSakerForAvdoede: Set<SakId> =
            avdoedeForGjeldendeBehandling
                .flatMap { avdoed ->
                    grunnlagService
                        .hentSakerOgRoller(Folkeregisteridentifikator.of(avdoed.value))
                        .sakiderOgRoller
                        .filter { it.sakId != gjeldendeSak && it.rolle == Saksrolle.AVDOED }
                        .map { it.sakId }
                }.toSet()

        if (kandidatSakerForAvdoede.isEmpty()) {
            logger.info("Fant ingen saker med felles avdøde fra grunnlag")
            return emptyList()
        }

        logger.info("Fant ${kandidatSakerForAvdoede.size} saker med felles avdøde fra grunnlag")

        val aktuelleBehandlinger =
            kandidatSakerForAvdoede.mapNotNull { sakId ->
                val avdoedeForKandidatSak =
                    grunnlagService
                        .hentPersongalleri(sakId)!!
                        .avdoed
                        .map { Folkeregisteridentifikator.of(it) }
                        .toSet()

                // Det er et kriterie at saken som skal brukes som utgangspunkt for å kopiere vilkår har akkurat de
                // samme avdøde som er i gjeldende sak - dette fordi vilkårsvurderingen potensielt kan inneholde
                // vurderinger som angår alle avdøde i persongalleriet
                val sammeAvdoede = avdoedeForKandidatSak == avdoedeForGjeldendeBehandling

                if (sammeAvdoede) {
                    behandlingService
                        .hentBehandlingerForSak(sakId)
                        .asSequence()
                        .filter { it.sak.sakType == SakType.BARNEPENSJON }
                        // Kun behandlinger som har vilkårsvurdering er aktuelt
                        .filter { it.status !in listOf(BehandlingStatus.AVBRUTT, BehandlingStatus.OPPRETTET) }
                        .sortedByDescending { it.behandlingOpprettet }
                        .firstOrNull()
                } else {
                    logger.info("Avdøde for gjeldende behandling og kandidat saker matchet ikke (se sikkerlogg)")
                    sikkerlogger().info(
                        "Avdøde for gjeldende behandling og kandidat saker matchet ikke " +
                            "(avdoedeForKandidatSak=$avdoedeForKandidatSak, " +
                            "avdoedeForGjeldendeBehandling=$avdoedeForGjeldendeBehandling)",
                    )
                    null
                }
            }

        logger.info("Fant ${aktuelleBehandlinger.size} aktuelle behandlinger som har vilkårsvurdering")
        if (aktuelleBehandlinger.isEmpty()) {
            return emptyList()
        }

        return aktuelleBehandlinger.mapNotNull { behandling ->
            val vilkaarsvurdering = repository.hent(behandling.id)
            vilkaarsvurdering?.behandlingId
        }
    }

    fun kopierVilkaarForAvdoede(
        behandlingId: UUID,
        kildeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vilkaarsvurdering {
        logger.info("Kopierer avdødes vilkår fra behandling $kildeBehandlingId til behandling $behandlingId")
        slettVilkaarsvurdering(behandlingId, brukerTokenInfo)

        // Validere at vi faktisk kan kopiere vilkår fra denne behandlingen
        val gyldigBehandling = finnBehandlingMedVilkaarsvurderingForSammeAvdoede(behandlingId)
        if (gyldigBehandling != kildeBehandlingId) {
            logger.info("Behandling som er kandidat for å kopiere vilkår fra er ikke samme som kildeBehandlingId")
            throw KanIkkeKopiereVilkaarForSammeAvdoedeFraBehandling()
        }

        val nyVilkaarsvurderingMedKopierteVilkaarForAvdoedes =
            kopierVilkaarsvurdering(
                behandlingId = behandlingId,
                kopierFraBehandling = kildeBehandlingId,
                brukerTokenInfo = brukerTokenInfo,
                kopierResultat = false,
                kopierKunVilkaarGjeldendeAvdoede = true,
            )

        logger.info(
            "Antall vilkår som er kopiert er ${
                nyVilkaarsvurderingMedKopierteVilkaarForAvdoedes.vilkaarsvurdering.vilkaar.count {
                    it.kopiertFraVilkaarId != null
                }
            } av totalt ${nyVilkaarsvurderingMedKopierteVilkaarForAvdoedes.vilkaarsvurdering.vilkaar.count()}",
        )

        // Sett tilbake status (vilkårsvurdering er ikke oppfylt da kopiering her kun tar noen vilkår og ikke totalvurdering)
        behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, dryRun = false)

        logger.info("Ny vilkårsvurdering med kopierte vilkår er opprettet")
        return nyVilkaarsvurderingMedKopierteVilkaarForAvdoedes.vilkaarsvurdering
    }
}

class BehandlingstilstandException(
    message: String? = null,
    e: Exception? = null,
) : IllegalStateException(message, e)

class VilkaarsvurderingTilstandException(
    detail: String,
    vilkaarvurdering: Vilkaarsvurdering,
) : UgyldigForespoerselException(
        code = "UGYLDIG_TILSTAND_VILKAARSVURDERING",
        detail = detail,
        meta =
            mapOf(
                "vilkaarvurderingId" to vilkaarvurdering.id,
                "behandlingId" to vilkaarvurdering.behandlingId,
            ),
    )

class VilkaarsvurderingIkkeFunnet :
    IkkeFunnetException(
        code = "VILKAARSVURDERING_IKKE_FUNNET",
        detail = "Vilkårsvurdering ikke funnet",
    )

class VilkaarsvurderingManglerResultat :
    UgyldigForespoerselException(
        code = "VILKAARSVURDERING_MANGLER_RESULTAT",
        detail = "Vilkårsvurderingen har ikke et resultat",
    )

class VirkningstidspunktSamsvarerIkke :
    UgyldigForespoerselException(
        code = "VILKAARSVURDERING_VIRKNINGSTIDSPUNKT_SAMSVARER_IKKE",
        detail = "Vilkårsvurderingen har et virkningstidspunkt som ikke samsvarer med behandling",
    )

class BehandlingVirkningstidspunktIkkeFastsatt :
    UgyldigForespoerselException(
        code = "VILKAARSVURDERING_VIRKNINGSTIDSPUNKT_IKKE_SATT",
        detail = "Virkningstidspunkt for behandlingen er ikke satt",
    )

class KanIkkeKopiereVilkaarForSammeAvdoedeFraBehandling :
    UgyldigForespoerselException(
        code = "VILKAARSVURDERING_KAN_IKKE_KOPIERE_VILKAAR",
        detail = "Kan ikke kopiere vilkår fra behandling",
    )

class BehandlingIkkeFunnet(
    behandlingId: UUID,
) : IkkeFunnetException(
        code = "BEHANDLING_IKKE_FUNNET",
        detail = "Behandling $behandlingId ikke funnet",
    )
