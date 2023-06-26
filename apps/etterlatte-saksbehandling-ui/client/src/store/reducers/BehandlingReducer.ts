import { createAction, createReducer } from '@reduxjs/toolkit'
import { IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { Beregning, BeregningsGrunnlagPostDto } from '~shared/types/Beregning'
import {
  IBehandlingStatus,
  IBoddEllerArbeidetUtlandet,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IUtenlandstilsnitt,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { VedtakSammendrag } from "~components/vedtak/typer";

export const addBehandling = createAction<IDetaljertBehandling>('behandling/add')
export const resetBehandling = createAction('behandling/reset')
export const oppdaterGyldighetsproeving = createAction<IGyldighetResultat>('behandling/gyldighetsprøving')
export const oppdaterVirkningstidspunkt = createAction<Virkningstidspunkt>('behandling/virkningstidspunkt')
export const updateVilkaarsvurdering = createAction<IVilkaarsvurdering>('behandling/update_vilkaarsvurdering')
export const updateVedtakSammendrag = createAction<VedtakSammendrag>('behandling/update_vedtaksammendrag')
export const oppdaterKommerBarnetTilgode = createAction<IKommerBarnetTilgode>('behandling/kommerBarnetTilgode')
export const oppdaterUtenlandstilsnitt = createAction<IUtenlandstilsnitt>('behandling/utenlandstilsnitt')
export const oppdaterBoddEllerArbeidetUtlandet = createAction<IBoddEllerArbeidetUtlandet>(
  'behandling/boddellerarbeidetutlandet'
)
export const oppdaterBeregning = createAction<Beregning>('behandling/beregning')
export const oppdaterBehandlingsstatus = createAction<IBehandlingStatus>('behandling/status')
export const oppdaterBeregingsGrunnlag = createAction<BeregningsGrunnlagPostDto>('behandling/beregningsgrunnlag')
export const resetBeregning = createAction('behandling/beregning/reset')
export const loggError = createAction<any>('loggError')
export const loggInfo = createAction<any>('loggInfo')

export interface IBehandlingReducer extends IDetaljertBehandling {
  beregningsGrunnlag?: BeregningsGrunnlagPostDto
  beregning?: Beregning
  vilkårsprøving?: IVilkaarsvurdering
  vedtak?: VedtakSammendrag
}
const initialState: { behandling: IBehandlingReducer | null } = { behandling: null }

export const behandlingReducer = createReducer(initialState, (builder) => {
  builder.addCase(addBehandling, (state, action) => {
    state.behandling = action.payload
  })
  builder.addCase(updateVilkaarsvurdering, (state, action) => {
    state.behandling!!.vilkårsprøving = action.payload
  })
  builder.addCase(updateVedtakSammendrag, (state, action) => {
    state.behandling!!.vedtak = action.payload
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
  builder.addCase(resetBeregning, (state) => {
    state.behandling!!.beregning = undefined
  })
})
