import { createAction, createReducer } from '@reduxjs/toolkit'
import { IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { Beregning, BeregningsGrunnlagData } from '~shared/types/Beregning'
import {
  IBehandlingStatus,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IUtenlandstilsnitt,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'

export const addBehandling = createAction<IDetaljertBehandling>('behandling/add')
export const resetBehandling = createAction('behandling/reset')
export const oppdaterGyldighetsproeving = createAction<IGyldighetResultat>('behandling/gyldighetsprøving')
export const oppdaterVirkningstidspunkt = createAction<Virkningstidspunkt>('behandling/virkningstidspunkt')
export const updateVilkaarsvurdering = createAction<IVilkaarsvurdering>('behandling/update_vilkaarsvurdering')
export const oppdaterKommerBarnetTilgode = createAction<IKommerBarnetTilgode>('behandling/kommerBarnetTilgode')
export const oppdaterUtenlandstilsnitt = createAction<IUtenlandstilsnitt>('behandling/utenlandstilsnitt')
export const oppdaterBeregning = createAction<Beregning>('behandling/beregning')
export const oppdaterBehandlingsstatus = createAction<IBehandlingStatus>('behandling/status')
export const oppdaterBeregingsGrunnlag = createAction<BeregningsGrunnlagData>('behandling/beregningsgrunnlag')
export const resetBeregning = createAction('behandling/beregning/reset')
export const loggError = createAction<any>('loggError')
export const loggInfo = createAction<any>('loggInfo')

export interface IBehandlingReducer extends IDetaljertBehandling {
  beregningsGrunnlag?: BeregningsGrunnlagData
  beregning?: Beregning
  vilkårsprøving?: IVilkaarsvurdering
}
const initialState: { behandling: IBehandlingReducer | null } = { behandling: null }

export const behandlingReducer = createReducer(initialState, (builder) => {
  builder.addCase(addBehandling, (state, action) => {
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
  builder.addCase(oppdaterUtenlandstilsnitt, (state, action) => {
    state.behandling!!.utenlandstilsnitt = action.payload
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
  builder.addCase(resetBeregning, (state) => {
    state.behandling!!.beregning = undefined
  })
})
