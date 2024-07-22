package no.nav.etterlatte.vilkaarsvurdering

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
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
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.kopier
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar1967
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar2024
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.OmstillingstoenadVilkaar
import org.slf4j.LoggerFactory
import java.util.UUID

class VirkningstidspunktIkkeSattException(
    behandlingId: UUID,
) : RuntimeException("Virkningstidspunkt ikke satt for behandling $behandlingId")

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingService::class.java)

    fun hentVilkaarsvurdering(behandlingId: UUID): Vilkaarsvurdering? = vilkaarsvurderingRepository.hent(behandlingId)

    suspend fun erMigrertYrkesskadefordel(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Boolean =
        vilkaarsvurderingRepository.hentMigrertYrkesskadefordel(
            behandlingKlient.hentBehandling(behandlingId, bruker).sak,
        )

    fun harRettUtenTidsbegrensning(behandlingId: UUID): Boolean =
        vilkaarsvurderingRepository
            .hent(behandlingId)
            ?.vilkaar
            ?.filter { it.hovedvilkaar.type == VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING }
            ?.any { it.hovedvilkaar.resultat == Utfall.OPPFYLT }
            ?: false

    suspend fun hentBehandlingensGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo).second

    suspend fun oppdaterTotalVurdering(
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
                vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
                    behandlingId = behandlingId,
                    virkningstidspunkt = virkningstidspunkt,
                    resultat = resultat,
                )
            if (vilkaarsvurdering.grunnlagVersjon != grunnlag.metadata.versjon) {
                vilkaarsvurderingRepository.oppdaterGrunnlagsversjon(behandlingId, grunnlag.metadata.versjon)
            }
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)
            VilkaarsvurderingMedBehandlingGrunnlagsversjon(vilkaarsvurdering, grunnlag.metadata.versjon)
        }

    suspend fun slettTotalVurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vilkaarsvurdering {
        if (behandlingKlient.settBehandlingStatusOpprettet(behandlingId, brukerTokenInfo, false)) {
            val vilkaarsvurdering = vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)
            behandlingKlient.settBehandlingStatusOpprettet(behandlingId, brukerTokenInfo, true)

            return vilkaarsvurdering
        }

        throw BehandlingstilstandException()
    }

    suspend fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val vilkaarsvurdering = vilkaarsvurderingRepository.hent(behandlingId)
            if (vilkaarsvurdering?.resultat != null) {
                throw VilkaarsvurderingTilstandException(
                    "Kan ikke endre et vilkår (${vurdertVilkaar.vilkaarId}) på en vilkårsvurdering som har et resultat. " +
                        "Vilkårsvurderingid: ${vilkaarsvurdering.id} Behandlingid: ${vilkaarsvurdering.behandlingId}",
                )
            }
            vilkaarsvurderingRepository.lagreVilkaarResultat(behandlingId, vurdertVilkaar)
        }

    suspend fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vilkaarId: UUID,
    ): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val vilkaarsvurdering = vilkaarsvurderingRepository.hent(behandlingId)
            if (vilkaarsvurdering?.resultat != null) {
                throw VilkaarsvurderingTilstandException(
                    "Kan ikke slette et vilkår ($vilkaarId) på en vilkårsvurdering som har et resultat. " +
                        "Vilkårsvurderingid: ${vilkaarsvurdering.id} Behandlingid: ${vilkaarsvurdering.behandlingId}",
                )
            }

            vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)
        }

    suspend fun kopierVilkaarsvurdering(
        behandlingId: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        kopierResultat: Boolean = true,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            logger.info("Oppretter og kopierer vilkårsvurdering for $behandlingId fra $kopierFraBehandling")
            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)
            val tidligereVilkaarsvurdering =
                vilkaarsvurderingRepository.hent(kopierFraBehandling)
                    ?: throw NullPointerException("Fant ikke vilkårsvurdering fra behandling $kopierFraBehandling")

            val virkningstidspunkt =
                behandling.virkningstidspunkt ?: throw VirkningstidspunktIkkeSattException(behandlingId)

            val vilkaar =
                when {
                    behandling.revurderingsaarsak == Revurderingaarsak.REGULERING ->
                        tidligereVilkaarsvurdering.vilkaar.kopier()
                    else ->
                        oppdaterVilkaar(
                            kopierteVilkaar = tidligereVilkaarsvurdering.vilkaar.kopier(),
                            behandling = behandling,
                            virkningstidspunkt = virkningstidspunkt,
                        )
                }

            val nyVilkaarsvurdering =
                vilkaarsvurderingRepository.kopierVilkaarsvurdering(
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
                    behandling.revurderingsaarsak != Revurderingaarsak.REGULERING &&
                        nyVilkaarsvurdering.vilkaar.any { v -> v.vurdering == null }
                )
            ) {
                VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                    vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(nyVilkaarsvurdering.behandlingId),
                    grunnlag.metadata.versjon,
                )
            } else {
                behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)
                VilkaarsvurderingMedBehandlingGrunnlagsversjon(nyVilkaarsvurdering, grunnlag.metadata.versjon)
            }
        }

    // Her legges det til nye vilkår og det filtreres bort vilkår som ikke lenger er aktuelle.
    // Oppdatering av vilkår med endringer er ennå ikke støttet.
    private fun oppdaterVilkaar(
        kopierteVilkaar: List<Vilkaar>,
        behandling: DetaljertBehandling,
        virkningstidspunkt: Virkningstidspunkt,
    ): List<Vilkaar> {
        val gjeldendeVilkaarForVirkningstidspunkt =
            finnVilkaarForNyVilkaarsvurdering(
                virkningstidspunkt,
                behandling.behandlingType,
                behandling.sakType,
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

    suspend fun opprettVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        kopierVedRevurdering: Boolean = true,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            vilkaarsvurderingRepository.hent(behandlingId)?.let {
                throw IllegalArgumentException("Vilkårsvurdering finnes allerede for behandling $behandlingId")
            }

            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)

            val virkningstidspunkt =
                behandling.virkningstidspunkt
                    ?: throw VirkningstidspunktIkkeSattException(behandlingId)

            logger.info(
                "Oppretter vilkårsvurdering for behandling ($behandlingId) med sakType ${behandling.sakType} og " +
                    "behandlingType ${behandling.behandlingType}",
            )

            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                        opprettNyVilkaarsvurdering(grunnlag, virkningstidspunkt, behandling, behandlingId),
                        grunnlag.metadata.versjon,
                    )
                }

                BehandlingType.REVURDERING -> {
                    if (kopierVedRevurdering) {
                        logger.info("Kopierer vilkårsvurdering for behandling $behandlingId fra forrige behandling")
                        val sisteIverksatteBehandling =
                            behandlingKlient.hentSisteIverksatteBehandling(
                                behandling.sak,
                                brukerTokenInfo,
                            )
                        VilkaarsvurderingMedBehandlingGrunnlagsversjon(
                            kopierVilkaarsvurdering(behandlingId, sisteIverksatteBehandling.id, brukerTokenInfo).vilkaarsvurdering,
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

    suspend fun slettVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = if (behandlingKlient.settBehandlingStatusOpprettet(behandlingId, brukerTokenInfo, false)) {
        val vilkaarsvurdering =
            vilkaarsvurderingRepository.hent(behandlingId)
                ?: throw IllegalStateException("Vilkårsvurdering eksisterer ikke")
        vilkaarsvurderingRepository.slettVilkaarvurdering(vilkaarsvurdering.id)
        behandlingKlient.settBehandlingStatusOpprettet(behandlingId, brukerTokenInfo, true)
    } else {
        throw BehandlingstilstandException()
    }

    private fun opprettNyVilkaarsvurdering(
        grunnlag: Grunnlag,
        virkningstidspunkt: Virkningstidspunkt,
        behandling: DetaljertBehandling,
        behandlingId: UUID,
    ): Vilkaarsvurdering {
        val vilkaar =
            finnVilkaarForNyVilkaarsvurdering(
                virkningstidspunkt,
                behandling.behandlingType,
                behandling.sakType,
            )

        return vilkaarsvurderingRepository.opprettVilkaarsvurdering(
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

    suspend fun sjekkGyldighetOgOppdaterBehandlingStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val behandling = oppdaterGrunnlagsversjon(behandlingId, brukerTokenInfo)
            val vilkaarsvurdering =
                vilkaarsvurderingRepository.hent(behandlingId)
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
                behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)
            } else {
                false
            }
        }

    private suspend fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling {
        val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)
        vilkaarsvurderingRepository.oppdaterGrunnlagsversjon(
            behandlingId = behandlingId,
            grunnlagVersjon = grunnlag.metadata.versjon,
        )
        return behandling
    }

    private suspend fun <T> tilstandssjekkFoerKjoering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        block: suspend () -> T,
    ): T {
        val kanVilkaarsvurdere = behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)

        if (!kanVilkaarsvurdere) {
            throw BehandlingstilstandException()
        }

        return block()
    }

    private suspend fun hentDataForVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pair<DetaljertBehandling, Grunnlag> =
        coroutineScope {
            val behandling = async { behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo) }
            val grunnlag = async { grunnlagKlient.hentGrunnlagForBehandling(behandlingId, brukerTokenInfo) }

            Pair(behandling.await(), grunnlag.await())
        }
}

class BehandlingstilstandException : IllegalStateException()

class VilkaarsvurderingTilstandException(
    message: String,
) : IllegalStateException(message)

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
