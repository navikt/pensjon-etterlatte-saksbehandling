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

const reducer = {
  menuReducer: menuReducer,
  saksbehandlerReducer: saksbehandlerReducer,
  behandlingReducer: behandlingReducer,
  appReducer: appReducer,
  klageReducer: klageReducer,
  tilbakekrevingReducer: tilbakekrevingReducer,
  journalfoeringOppgaveReducer: journalfoeringOppgaveReducer,
}
export const store = configureStore({
  reducer,
  devTools: true,
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

export const useAppDispatch: () => AppDispatch = useDispatch
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector
