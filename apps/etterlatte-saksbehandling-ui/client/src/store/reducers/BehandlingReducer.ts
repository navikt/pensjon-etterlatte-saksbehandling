import { createAction, createReducer } from '@reduxjs/toolkit'
import { IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import {
  Beregning,
  BeregningsGrunnlagOMSPostDto,
  BeregningsGrunnlagPostDto,
  OverstyrBeregningGrunnlagPostDTO,
} from '~shared/types/Beregning'
import {
  IBehandlingStatus,
  IBoddEllerArbeidetUtlandet,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IUtenlandstilknytning,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { RevurderingInfo } from '~shared/types/RevurderingInfo'

export const setBehandling = createAction<IDetaljertBehandling>('behandling/set')
export const resetBehandling = createAction('behandling/reset')
export const oppdaterGyldighetsproeving = createAction<IGyldighetResultat>('behandling/gyldighetsproeving')
export const oppdaterVirkningstidspunkt = createAction<Virkningstidspunkt>('behandling/virkningstidspunkt')
export const updateVilkaarsvurdering = createAction<IVilkaarsvurdering | undefined>(
  'behandling/update_vilkaarsvurdering'
)
export const oppdaterKommerBarnetTilgode = createAction<IKommerBarnetTilgode>('behandling/kommerBarnetTilgode')

export const oppdaterBoddEllerArbeidetUtlandet = createAction<IBoddEllerArbeidetUtlandet>(
  'behandling/boddellerarbeidetutlandet'
)
export const oppdaterBeregning = createAction<Beregning>('behandling/beregning')
export const oppdaterBehandlingsstatus = createAction<IBehandlingStatus>('behandling/status')
export const oppdaterUtenlandstilknytning = createAction<IUtenlandstilknytning>('behandling/utenlandstilknytning')
export const oppdaterBeregingsGrunnlag = createAction<BeregningsGrunnlagPostDto>('behandling/beregningsgrunnlag')
export const oppdaterBeregingsGrunnlagOMS = createAction<BeregningsGrunnlagOMSPostDto>(
  'behandling/beregningsgrunnlagOMS'
)
export const oppdaterOverstyrBeregningsGrunnlag =
  createAction<OverstyrBeregningGrunnlagPostDTO>('behandling/overstyrBeregning')
export const oppdaterRevurderingInfo = createAction<RevurderingInfo>('behandling/revurderinginfo')
export const resetBeregning = createAction('behandling/beregning/reset')
export const loggError = createAction<any>('loggError')
export const loggInfo = createAction<any>('loggInfo')

export interface IBehandlingReducer extends IDetaljertBehandling {
  beregningsGrunnlag?: BeregningsGrunnlagPostDto
  beregningsGrunnlagOMS?: BeregningsGrunnlagOMSPostDto
  overstyrBeregning?: OverstyrBeregningGrunnlagPostDTO
  beregning?: Beregning
  vilkårsprøving?: IVilkaarsvurdering
}

const initialState: { behandling: IBehandlingReducer | null } = {
  behandling: null,
}

export const behandlingReducer = createReducer(initialState, (builder) => {
  builder.addCase(setBehandling, (state, action) => {
    state.behandling = action.payload
  })
  builder.addCase(updateVilkaarsvurdering, (state, action) => {
    state.behandling!!.vilkårsprøving = action.payload
  })
  builder.addCase(resetBehandling, (state) => {
    state.behandling = null
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
  builder.addCase(oppdaterBeregning, (state, action) => {
    state.behandling!!.beregning = action.payload
  })
  builder.addCase(oppdaterBehandlingsstatus, (state, action) => {
    state.behandling!!.status = action.payload
  })
  builder.addCase(oppdaterBeregingsGrunnlag, (state, action) => {
    state.behandling!!.beregningsGrunnlag = action.payload
  })
  builder.addCase(oppdaterUtenlandstilknytning, (state, action) => {
    state.behandling!!.utenlandstilknytning = action.payload
  })
  builder.addCase(oppdaterBeregingsGrunnlagOMS, (state, action) => {
    state.behandling!!.beregningsGrunnlagOMS = action.payload
  })
  builder.addCase(oppdaterOverstyrBeregningsGrunnlag, (state, action) => {
    state.behandling!!.overstyrBeregning = action.payload
  })
  builder.addCase(resetBeregning, (state) => {
    state.behandling!!.beregning = undefined
  })
  builder.addCase(oppdaterRevurderingInfo, (state, action) => {
    state.behandling!!.revurderinginfo!!.revurderingInfo = action.payload
  })
})
