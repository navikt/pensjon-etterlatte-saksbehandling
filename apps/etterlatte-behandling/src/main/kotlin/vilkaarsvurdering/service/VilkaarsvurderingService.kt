package no.nav.etterlatte.vilkaarsvurdering.service

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.erPaaNyttRegelverk
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
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
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingRepositoryWrapper
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar1967
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar2024
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.OmstillingstoenadVilkaar
import org.slf4j.LoggerFactory
import java.util.UUID

class VirkningstidspunktIkkeSattException(
    behandlingId: UUID,
) : RuntimeException("Virkningstidspunkt ikke satt for behandling $behandlingId")

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepositoryWrapper: VilkaarsvurderingRepositoryWrapper,
    private val behandlingService: BehandlingService,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingStatus: BehandlingStatusService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentVilkaarsvurdering(behandlingId: UUID): Vilkaarsvurdering? = vilkaarsvurderingRepositoryWrapper.hent(behandlingId)

    fun erMigrertYrkesskadefordel(behandlingId: UUID): Boolean {
        val hentBehandling = behandlingService.hentBehandling(behandlingId)!!
        return vilkaarsvurderingRepositoryWrapper.hentMigrertYrkesskadefordel(hentBehandling.id, hentBehandling.sak.id)
    }

    fun harRettUtenTidsbegrensning(behandlingId: UUID): Boolean =
        vilkaarsvurderingRepositoryWrapper
            .hent(behandlingId)
            ?.vilkaar
            ?.filter { it.hovedvilkaar.type == VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING }
            ?.any { it.hovedvilkaar.resultat == Utfall.OPPFYLT }
            ?: false

    fun hentBehandlingensGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo).second

    fun oppdaterTotalVurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        resultat: VilkaarsvurderingResultat,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)
            val virkningstidspunkt =
                behandling.virkningstidspunkt?.dato?.atDay(1)
                    ?: throw IllegalStateException("Virkningstidspunkt må være satt for å sette en vurdering")
            val vilkaarsvurdering =
                vilkaarsvurderingRepositoryWrapper.lagreVilkaarsvurderingResultat(
                    behandlingId = behandlingId,
                    virkningstidspunkt = virkningstidspunkt,
                    resultat = resultat,
                )
            if (vilkaarsvurdering.grunnlagVersjon != grunnlag.metadata.versjon) {
                vilkaarsvurderingRepositoryWrapper.oppdaterGrunnlagsversjon(behandlingId, grunnlag.metadata.versjon)
            }
            behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo, false)
            VilkaarsvurderingMedBehandlingGrunnlagsversjon(vilkaarsvurdering, grunnlag.metadata.versjon)
        }

    fun slettTotalVurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vilkaarsvurdering {
        behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, true)
        val vilkaarsvurdering = vilkaarsvurderingRepositoryWrapper.slettVilkaarsvurderingResultat(behandlingId)
        behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, false)
        return vilkaarsvurdering
    }

    fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val vilkaarsvurdering = vilkaarsvurderingRepositoryWrapper.hent(behandlingId)
            if (vilkaarsvurdering?.resultat != null) {
                throw VilkaarsvurderingTilstandException(
                    "Kan ikke endre et vilkår (${vurdertVilkaar.vilkaarId}) på en vilkårsvurdering som har et resultat. ",
                    vilkaarsvurdering,
                )
            }
            vilkaarsvurderingRepositoryWrapper.oppdaterVurderingPaaVilkaar(behandlingId, vurdertVilkaar)
        }

    fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vilkaarId: UUID,
    ): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val vilkaarsvurdering = vilkaarsvurderingRepositoryWrapper.hent(behandlingId)
            if (vilkaarsvurdering?.resultat != null) {
                throw VilkaarsvurderingTilstandException(
                    "Kan ikke slette et vilkår ($vilkaarId) på en vilkårsvurdering som har et resultat. ",
                    vilkaarsvurdering,
                )
            }

            vilkaarsvurderingRepositoryWrapper.slettVilkaarResultat(behandlingId, vilkaarId)
        }

    private fun validerTidligereVilkaarsvurdering(tidligereVilkaarsvurdering: Vilkaarsvurdering) {
        if (tidligereVilkaarsvurdering.resultat == null) {
            throw VilkaarsvurderingValideringException(
                "Mangler resultat for vilkårsvurdering",
                vilkaarvurdering = tidligereVilkaarsvurdering,
            )
        }

        if (tidligereVilkaarsvurdering.vilkaar.isEmpty()) {
            throw VilkaarsvurderingValideringException("Mangler vilkår for vilkårsvurdering", vilkaarvurdering = tidligereVilkaarsvurdering)
        }

        if (tidligereVilkaarsvurdering.vilkaar.any { it.vurdering == null }) {
            throw VilkaarsvurderingValideringException(
                "Mangler vurdering for delvilkår i vilkårsvurdering",
                vilkaarvurdering = tidligereVilkaarsvurdering,
            )
        }
    }

    fun kopierVilkaarsvurdering(
        behandlingId: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        kopierResultat: Boolean = true,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            logger.info("Oppretter og kopierer vilkårsvurdering for $behandlingId fra $kopierFraBehandling")
            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)
            val tidligereVilkaarsvurdering =
                vilkaarsvurderingRepositoryWrapper.hent(kopierFraBehandling)
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
                            kopierteVilkaar = tidligereVilkaarsvurdering.vilkaar.kopier(),
                            behandling = behandling,
                            virkningstidspunkt = virkningstidspunkt,
                        )
                }

            val nyVilkaarsvurdering =
                vilkaarsvurderingRepositoryWrapper.kopierVilkaarsvurdering(
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
                VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                    vilkaarsvurderingRepositoryWrapper.slettVilkaarsvurderingResultat(nyVilkaarsvurdering.behandlingId),
                    grunnlag.metadata.versjon,
                )
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
            vilkaarsvurderingRepositoryWrapper.hent(behandlingId)?.let {
                throw IllegalArgumentException("Vilkårsvurdering finnes allerede for behandling $behandlingId")
            }

            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)

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
                        val sisteIverksatteBehandling = behandlingService.hentSisteIverksatte(behandling.sak.id)!!
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
            vilkaarsvurderingRepositoryWrapper.hent(behandlingId)
                ?: throw IllegalStateException("Vilkårsvurderingen eksisterer ikke")
        vilkaarsvurderingRepositoryWrapper.slettVilkaarvurdering(behandlingId, vilkaarsvurdering.id)
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

        return vilkaarsvurderingRepositoryWrapper.opprettVilkaarsvurdering(
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
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    BehandlingType.REVURDERING,
                    ->
                        OmstillingstoenadVilkaar.inngangsvilkaar()
                }
        }

    fun sjekkGyldighetOgOppdaterBehandlingStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val behandling = oppdaterGrunnlagsversjon(behandlingId, brukerTokenInfo)
            val vilkaarsvurdering =
                vilkaarsvurderingRepositoryWrapper.hent(behandlingId)
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

    private fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Behandling {
        val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)
        vilkaarsvurderingRepositoryWrapper.oppdaterGrunnlagsversjon(
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

    private fun hentDataForVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pair<Behandling, Grunnlag> {
        val behandling = behandlingService.hentBehandling(behandlingId)!!
        val grunnlag = runBlocking { grunnlagKlient.hentGrunnlagForBehandling(behandlingId, brukerTokenInfo) }
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
            return OmstillingstoenadVilkaar.inngangsvilkaar()
        }
        return if (behandling.virkningstidspunkt!!.erPaaNyttRegelverk()) {
            BarnepensjonVilkaar2024.inngangsvilkaar()
        } else {
            BarnepensjonVilkaar1967.inngangsvilkaar()
        }
    }
}

class BehandlingstilstandException(
    message: String? = null,
    e: Exception? = null,
) : IllegalStateException(message, e)

class VilkaarsvurderingValideringException(
    detail: String,
    vilkaarvurdering: Vilkaarsvurdering,
) : UgyldigForespoerselException(
        code = "UGYLDIG_TILSTAND_VILKAARSVURDERING_KOPIER",
        detail = detail,
        meta =
            mapOf(
                "vilkaarvurderingId" to vilkaarvurdering.id,
                "behandlingId" to vilkaarvurdering.behandlingId,
            ),
    )

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
