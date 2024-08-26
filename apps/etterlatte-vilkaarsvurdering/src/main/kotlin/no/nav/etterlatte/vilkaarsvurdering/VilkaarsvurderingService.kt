package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.erPaaNyttRegelverk
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingDto
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar1967
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar2024
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.OmstillingstoenadVilkaar
import org.slf4j.LoggerFactory
import vilkaarsvurdering.OppdaterVurdertVilkaar
import vilkaarsvurdering.Vilkaarsvurdering
import java.util.UUID

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val grunnlagKlient: GrunnlagKlient, // TODO: slett
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentVilkaarsvurdering(behandlingId: UUID): Vilkaarsvurdering? = vilkaarsvurderingRepository.hent(behandlingId)

    fun erMigrertYrkesskadefordel(sakId: Long): Boolean = vilkaarsvurderingRepository.hentMigrertYrkesskadefordel(sakId = sakId)

    fun harRettUtenTidsbegrensning(behandlingId: UUID): Boolean =
        vilkaarsvurderingRepository
            .hent(behandlingId)
            ?.vilkaar
            ?.filter { it.hovedvilkaar.type == VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING }
            ?.any { it.hovedvilkaar.resultat == Utfall.OPPFYLT }
            ?: false

    fun oppdaterTotalVurdering(vurdertVilkaar: VurdertVilkaarsvurderingDto): Vilkaarsvurdering =
        vilkaarsvurderingRepository.lagreVilkaarsvurderingResultatvanlig(
            vurdertVilkaar.virkningstidspunkt,
            vurdertVilkaar.resultat,
            vurdertVilkaar.vilkaarsvurdering,
        )

    fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering =
        vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)

    fun oppdaterVurderingPaaVilkaar(oppdatertvilkaar: OppdaterVurdertVilkaar): Vilkaarsvurdering =
        vilkaarsvurderingRepository.oppdaterVurderingPaaVilkaar(oppdatertvilkaar.behandlingId, oppdatertvilkaar.vurdertVilkaar)

    fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering {
        vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)
        return vilkaarsvurderingRepository.hent(behandlingId)!!
    }

    fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        tidligereVilkaarsvurderingId: UUID,
    ): Vilkaarsvurdering =
        vilkaarsvurderingRepository.kopierVilkaarsvurdering(
            nyVilkaarsvurdering = nyVilkaarsvurdering,
            kopiertFraId = tidligereVilkaarsvurderingId,
        )

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

    fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering =
        vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

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

    fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagsversjon: Long,
    ) {
        vilkaarsvurderingRepository.oppdaterGrunnlagsversjon(
            behandlingId = behandlingId,
            grunnlagVersjon = grunnlagsversjon,
        )
    }

    suspend fun hentVilkaartyper(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = finnRelevanteTyper(behandlingId, bruker)
        .map { it.hovedvilkaar }
        .map { it.type }
        .map { VilkaartypePair(name = it.name, tittel = it.tittel) }

    private suspend fun finnRelevanteTyper(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): List<Vilkaar> {
        val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
        if (behandling.sakType == SakType.OMSTILLINGSSTOENAD) {
            return OmstillingstoenadVilkaar.inngangsvilkaar()
        }
        return if (behandling.virkningstidspunkt!!.erPaaNyttRegelverk()) {
            BarnepensjonVilkaar2024.inngangsvilkaar()
        } else {
            BarnepensjonVilkaar1967.inngangsvilkaar()
        }
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
