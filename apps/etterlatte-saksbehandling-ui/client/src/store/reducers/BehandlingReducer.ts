import { createAction, createReducer } from '@reduxjs/toolkit'
import { IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { Beregning } from '~shared/types/Beregning'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IDetaljertBehandling,
  IKommerBarnetTilgode,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: '',
  sak: 0,
  status: IBehandlingStatus.OPPRETTET, //test
  saksbehandlerId: '',
  attestant: '',
  vilkårsprøving: undefined,
  gyldighetsprøving: undefined,
  kommerBarnetTilgode: null,
  beregning: undefined,
  fastsatt: false,
  soeknadMottattDato: '',
  virkningstidspunkt: null,
  hendelser: [],
  familieforhold: undefined,
  behandlingType: IBehandlingsType.FØRSTEGANGSBEHANDLING,
  søker: undefined,
}

export const addBehandling = createAction<IDetaljertBehandling>('behandling/add')
export const resetBehandling = createAction('behandling/reset')
export const oppdaterVirkningstidspunkt = createAction<Virkningstidspunkt>('behandling/virkningstidspunkt')
export const updateVilkaarsvurdering = createAction<IVilkaarsvurdering>('behandling/update_vilkaarsvurdering')
export const oppdaterKommerBarnetTilgode = createAction<IKommerBarnetTilgode>('behandling/kommerBarnetTilgode')
export const oppdaterBeregning = createAction<Beregning>('behandling/beregning')
export const resetBeregning = createAction('behandling/beregning/reset')
export const loggError = createAction<any>('loggError')
export const loggInfo = createAction<any>('loggInfo')

export interface IBehandlingReducer {
  behandling: IDetaljertBehandling
}
const initialState: IBehandlingReducer = { behandling: detaljertBehandlingInitialState }

export const behandlingReducer = createReducer(initialState, (builder) => {
  builder.addCase(addBehandling, (state, action) => {
    state.behandling = { ...action.payload, vilkårsprøving: undefined }
    state.behandling.behandlingType = action.payload.behandlingType ?? IBehandlingsType.FØRSTEGANGSBEHANDLING // Default til behandlingstype hvis null
  })
  builder.addCase(updateVilkaarsvurdering, (state, action) => {
    state.behandling.vilkårsprøving = action.payload
  })
  builder.addCase(resetBehandling, (state) => {
    state.behandling = detaljertBehandlingInitialState
  })
  builder.addCase(oppdaterVirkningstidspunkt, (state, action) => {
    state.behandling.virkningstidspunkt = action.payload
  })
  builder.addCase(oppdaterKommerBarnetTilgode, (state, action) => {
    state.behandling.kommerBarnetTilgode = action.payload
  })
  builder.addCase(oppdaterBeregning, (state, action) => {
    state.behandling.beregning = action.payload
  })
  builder.addCase(resetBeregning, (state) => {
    state.behandling.beregning = detaljertBehandlingInitialState.beregning
  })
})
