package no.nav.etterlatte.modules.beregning

import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.modules.beregning.anvendttrygdetid.AnvendtTrygdetidPeriodeUtrekning
import no.nav.etterlatte.modules.beregning.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.FaktiskInntekt
import no.nav.etterlatte.modules.beregning.etteroppgjoer.BeregnetEtteroppgjoerResultat
import no.nav.etterlatte.modules.beregning.sanksjon.LagreSanksjon
import java.util.UUID

interface BeregningRepositoryOperasjoner {
    fun hent(behandlingId: UUID): Beregning?

    fun lagreEllerOppdaterBeregning(beregning: Beregning): Beregning

    fun slettOverstyrtBeregningsgrunnlag(behandlingId: UUID)

    fun hentOverstyrBeregning(sakId: SakId): OverstyrBeregning?

    fun opprettOverstyrBeregning(overstyrBeregning: OverstyrBeregning): OverstyrBeregning?

    fun deaktiverOverstyrtBeregning(sakId: SakId)
}

interface BeregningsGrunnlagRepositoryOperasjoner {
    fun finnBeregningsGrunnlag(id: UUID): BeregningsGrunnlag?

    fun lagreBeregningsGrunnlag(beregningsGrunnlag: BeregningsGrunnlag): Boolean

    fun finnOverstyrBeregningGrunnlagForBehandling(id: UUID): List<OverstyrBeregningGrunnlagDao>

    fun lagreOverstyrBeregningGrunnlagForBehandling(
        behandlingId: UUID,
        data: List<OverstyrBeregningGrunnlagDao>,
    )
}

interface AnvendtTrygdetidRepositoryOperasjoner {
    fun lagreAnvendtTrygdetid(
        behandlingId: UUID,
        utrekning: AnvendtTrygdetidPeriodeUtrekning,
    ): Int
}

interface AvkortingRepositoryOperasjoner {
    fun harSakInntektForAar(harInntektForAarDto: InntektsjusteringAvkortingInfoRequest): Boolean

    fun hentAvkorting(behandlingId: UUID): Avkorting?

    fun hentFaktiskInntekt(aarsoppgjoerId: UUID): FaktiskInntekt?

    fun lagreAvkorting(
        behandlingId: UUID,
        sakId: SakId,
        avkorting: Avkorting,
    )

    fun slettForBehandling(behandling: UUID)

    fun hentAlleAarsoppgjoer(sakId: SakId): List<AarsoppgjoerLoepende>

    fun hentAlleBehandlingerMedAarsoppgjoer(sakId: SakId): List<UUID>

    fun hentAlleAarsoppgjoer(behandlinger: List<UUID>): List<AarsoppgjoerLoepende>
}

interface EtteroppgjoerRepositoryOperasjoner {
    fun lagreEtteroppgjoerResultat(resultat: BeregnetEtteroppgjoerResultat)

    fun hentEtteroppgjoerResultat(
        aar: Int,
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
    ): BeregnetEtteroppgjoerResultat?
}

interface SanksjonRepositoryOperasjoner {
    fun hentSanksjon(behandlingId: UUID): List<Sanksjon>

    fun opprettSanksjon(
        behandlingId: UUID,
        sakId: SakId,
        saksbehandlerIdent: String,
        sanksjon: LagreSanksjon,
    )

    fun opprettSanksjonFraKopi(
        behandlingId: UUID,
        sakId: SakId,
        sanksjon: Sanksjon,
    )

    fun oppdaterSanksjon(
        sanksjon: LagreSanksjon,
        saksbehandlerIdent: String,
    )

    fun slettSanksjon(sanksjonId: UUID): Int

    fun hentSanksjonMedId(sanksjonId: UUID): Sanksjon?
}
