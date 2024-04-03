import React, { createContext, ReactNode, useContext, useState } from 'react'
import {
  initialOppgavebenkState,
  OppgavebenkState,
  OppgavebenkStats,
} from '~components/oppgavebenk/state/oppgavebenkState'
import { hentOppgavebenkStats, OppgaveDTO } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'

export interface OppgavebenkStateDispatcher {
  setOppgavelistaOppgaver: (oppgaver: OppgaveDTO[]) => void
  setMinOppgavelisteOppgaver: (oppgaver: OppgaveDTO[]) => void
  setGosysOppgavelisteOppgaver: (oppgaver: OppgaveDTO[]) => void
  setOppgavebenkStats: (stats: OppgavebenkStats) => void
  oppdaterOppgavebenkStats: () => void
}

const oppgavebenkContext = createContext(initialOppgavebenkState)
const oppgavebenkStateDispatch = createContext({} as OppgavebenkStateDispatcher)

const ProvideOppgavebenkContext = ({ children }: { children: ReactNode | Array<ReactNode> }): ReactNode => {
  const [state, setState] = useState<OppgavebenkState>(initialOppgavebenkState)

  const [, oppgavebenkStatsFetch] = useApiCall(hentOppgavebenkStats)

  const dispatcher: OppgavebenkStateDispatcher = {
    setOppgavelistaOppgaver: (oppgaver) => setState({ ...state, oppgavelistaOppgaver: oppgaver }),
    setMinOppgavelisteOppgaver: (oppgaver) => setState({ ...state, minOppgavelisteOppgaver: oppgaver }),
    setGosysOppgavelisteOppgaver: (oppgaver) => setState({ ...state, gosysOppgavelisteOppgaver: oppgaver }),
    setOppgavebenkStats: (stats) => setState({ ...state, opgpavebenkStats: stats }),
    oppdaterOppgavebenkStats: () =>
      oppgavebenkStatsFetch({}, (result) => setState({ ...state, opgpavebenkStats: result })),
  }

  return (
    <oppgavebenkContext.Provider value={state}>
      <oppgavebenkStateDispatch.Provider value={dispatcher}>{children}</oppgavebenkStateDispatch.Provider>
    </oppgavebenkContext.Provider>
  )
}

const useOppgaveBenkState = (): OppgavebenkState => {
  return useContext(oppgavebenkContext)
}

const useOppgavebenkStateDispatcher = (): OppgavebenkStateDispatcher => {
  return useContext(oppgavebenkStateDispatch)
}

export { ProvideOppgavebenkContext, useOppgaveBenkState, useOppgavebenkStateDispatcher }
