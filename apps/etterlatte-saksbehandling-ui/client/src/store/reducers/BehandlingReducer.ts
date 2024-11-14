import { createAction, createReducer } from '@reduxjs/toolkit'
import { IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { Beregning, BeregningsGrunnlagDto, OverstyrBeregningGrunnlagPostDTO } from '~shared/types/Beregning'
import {
  IBehandlingStatus,
  IBoddEllerArbeidetUtlandet,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  ITidligereFamiliepleier,
  IUtlandstilknytning,
  ViderefoertOpphoer,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { RevurderingMedBegrunnelse } from '~shared/types/RevurderingInfo'
import { BrevutfallOgEtterbetaling } from '~components/behandling/brevutfall/Brevutfall'
import { ISendBrev } from '~components/behandling/brevutfall/SkalSendeBrev'
import { IAvkorting } from '~shared/types/IAvkorting'

export const setBehandling = createAction<IDetaljertBehandling>('behandling/set')
export const resetBehandling = createAction('behandling/reset')
export const oppdaterGyldighetsproeving = createAction<IGyldighetResultat>('behandling/gyldighetsproeving')
export const oppdaterVirkningstidspunkt = createAction<Virkningstidspunkt>('behandling/virkningstidspunkt')
export const updateVilkaarsvurdering = createAction<IVilkaarsvurdering | undefined>(
  'behandling/update_vilkaarsvurdering'
)

export const updateBrevutfallOgEtterbetaling = createAction<BrevutfallOgEtterbetaling | undefined>(
  'behandling/update_brevutfallogetterbetaling'
)
export const oppdaterKommerBarnetTilgode = createAction<IKommerBarnetTilgode>('behandling/kommerBarnetTilgode')

export const oppdaterBoddEllerArbeidetUtlandet = createAction<IBoddEllerArbeidetUtlandet>(
  'behandling/boddellerarbeidetutlandet'
)
export const oppdaterSendeBrev = createAction<ISendBrev>('behandling/sendBrev')
export const oppdaterBeregning = createAction<Beregning>('behandling/beregning')
export const oppdaterAvkorting = createAction<IAvkorting>('behandling/avkorting')
export const resetAvkorting = createAction('behandling/avkorting/reset')

export const oppdaterBehandlingsstatus = createAction<IBehandlingStatus>('behandling/status')
export const oppdaterUtlandstilknytning = createAction<IUtlandstilknytning>('behandling/utlandstilknytning')
export const oppdaterViderefoertOpphoer = createAction<ViderefoertOpphoer>('behandling/viderefoert-opphoer')
export const oppdaterBeregningsGrunnlag = createAction<BeregningsGrunnlagDto>('behandling/beregningsgrunnlag')
export const oppdaterOverstyrBeregningsGrunnlag =
  createAction<OverstyrBeregningGrunnlagPostDTO>('behandling/overstyrBeregning')
export const oppdaterRevurderingInfo = createAction<RevurderingMedBegrunnelse>('behandling/revurderinginfo')
export const oppdaterTidligereFamiliepleier = createAction<ITidligereFamiliepleier>(
  'behandling/tidligere-familiepleier'
)
export const resetBeregning = createAction('behandling/beregning/reset')
export const loggError = createAction<any>('loggError')
export const loggInfo = createAction<any>('loggInfo')
export const resetViderefoertOpphoer = createAction('behandling/viderefoert-opphoer/reset')

export interface IBehandlingReducer extends IDetaljertBehandling {
  beregningsGrunnlag?: BeregningsGrunnlagDto
  overstyrBeregning?: OverstyrBeregningGrunnlagPostDTO
  beregning?: Beregning
  vilkaarsvurdering?: IVilkaarsvurdering
  brevutfallOgEtterbetaling?: BrevutfallOgEtterbetaling
  avkorting?: IAvkorting
}

const initialState: { behandling: IBehandlingReducer | null } = {
  behandling: null,
}

export const behandlingReducer = createReducer(initialState, (builder) => {
  builder.addCase(setBehandling, (state, action) => {
    state.behandling = action.payload
  })
  builder.addCase(resetBehandling, (state) => {
    state.behandling = null
  })
  builder.addCase(updateVilkaarsvurdering, (state, action) => {
    state.behandling!!.vilkaarsvurdering = action.payload
  })
  builder.addCase(updateBrevutfallOgEtterbetaling, (state, action) => {
    state.behandling!!.brevutfallOgEtterbetaling = action.payload
  })
  builder.addCase(oppdaterGyldighetsproeving, (state, action) => {
    state.behandling!!.gyldighetsprøving = action.payload
  })
  builder.addCase(oppdaterVirkningstidspunkt, (state, action) => {
    state.behandling!!.virkningstidspunkt = action.payload
  })
  builder.addCase(oppdaterKommerBarnetTilgode, (state, action) => {
    state.behandling!!.kommerBarnetTilgode = action.payload
  })
  builder.addCase(oppdaterBoddEllerArbeidetUtlandet, (state, action) => {
    state.behandling!!.boddEllerArbeidetUtlandet = action.payload
  })
  builder.addCase(oppdaterSendeBrev, (state, action) => {
    state.behandling!!.sendeBrev = action.payload.sendBrev
  })
  builder.addCase(oppdaterBeregning, (state, action) => {
    state.behandling!!.beregning = action.payload
  })
  builder.addCase(oppdaterBehandlingsstatus, (state, action) => {
    state.behandling!!.status = action.payload
  })
  builder.addCase(oppdaterBeregningsGrunnlag, (state, action) => {
    state.behandling!!.beregningsGrunnlag = action.payload
  })
  builder.addCase(oppdaterUtlandstilknytning, (state, action) => {
    state.behandling!!.utlandstilknytning = action.payload
  })
  builder.addCase(oppdaterViderefoertOpphoer, (state, action) => {
    state.behandling!!.viderefoertOpphoer = action.payload
  })
  builder.addCase(oppdaterOverstyrBeregningsGrunnlag, (state, action) => {
    state.behandling!!.overstyrBeregning = action.payload
  })
  builder.addCase(resetBeregning, (state) => {
    state.behandling!!.beregning = undefined
  })
  builder.addCase(oppdaterRevurderingInfo, (state, action) => {
    state.behandling!!.revurderinginfo = action.payload
  })
  builder.addCase(oppdaterAvkorting, (state, action) => {
    state.behandling!!.avkorting = action.payload
  })
  builder.addCase(resetAvkorting, (state) => {
    state.behandling!!.avkorting = undefined
  })
  builder.addCase(resetViderefoertOpphoer, (state) => {
    state.behandling!!.viderefoertOpphoer = null
  })
  builder.addCase(oppdaterTidligereFamiliepleier, (state, action) => {
    state.behandling!!.tidligereFamiliepleier = action.payload
  })
})
