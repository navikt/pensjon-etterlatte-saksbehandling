import { behandlingReducer } from './reducers/BehandlingReducer'
import { journalfoeringOppgaveReducer } from './reducers/JournalfoeringOppgaveReducer'
import { menuReducer } from './reducers/MenuReducer'
import { saksbehandlerReducer } from './reducers/SaksbehandlerReducer'
import { configureStore } from '@reduxjs/toolkit'
import type { TypedUseSelectorHook } from 'react-redux'
import { useDispatch, useSelector } from 'react-redux'
import { appReducer } from '~store/reducers/AppconfigReducer'
import { klageReducer } from '~store/reducers/KlageReducer'
import { tilbakekrevingReducer } from '~store/reducers/TilbakekrevingReducer'
import { vedtakReducer } from '~store/reducers/VedtakReducer'
import { sjekklisteReducer } from '~store/reducers/SjekklisteReducer'
import { behandlingsidemenyReducer } from '~store/reducers/BehandlingSidemenyReducer'

const reducer = {
  menuReducer: menuReducer,
  saksbehandlerReducer: saksbehandlerReducer,
  behandlingReducer: behandlingReducer,
  behandlingSidemenyReducer: behandlingsidemenyReducer,
  vedtakReducer: vedtakReducer,
  appReducer: appReducer,
  klageReducer: klageReducer,
  tilbakekrevingReducer: tilbakekrevingReducer,
  journalfoeringOppgaveReducer: journalfoeringOppgaveReducer,
  sjekklisteReducer: sjekklisteReducer,
}
export const store = configureStore({
  reducer,
  devTools: true,
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

export const useAppDispatch: () => AppDispatch = useDispatch
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector
