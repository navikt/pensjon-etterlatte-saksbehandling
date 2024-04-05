import React, { createContext, ReactNode, useContext, useState } from 'react'
import {
  initialOppgavebenkState,
  OppgavebenkState,
  OppgavebenkStats,
} from '~components/oppgavebenk/state/oppgavebenkState'
import { hentOppgavebenkStats } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OppgaveDTO } from '~shared/types/oppgave'

export interface OppgavebenkStateDispatcher {
  setOppgavelistaOppgaver: (oppgaver: OppgaveDTO[]) => void
  setMinOppgavelisteOppgaver: (oppgaver: OppgaveDTO[]) => void
  setGosysOppgavelisteOppgaver: (oppgaver: OppgaveDTO[]) => void
  setOppgavebenkStats: (stats: OppgavebenkStats) => void
}

const oppgavebenkContext = createContext(initialOppgavebenkState)
const oppgavebenkStateDispatch = createContext({} as OppgavebenkStateDispatcher)

const ProvideOppgavebenkContext = ({ children }: { children: ReactNode | Array<ReactNode> }): ReactNode => {
  const [state, setState] = useState<OppgavebenkState>(initialOppgavebenkState)

  const [, oppgavebenkStatsFetch] = useApiCall(hentOppgavebenkStats)

  const dispatcher: OppgavebenkStateDispatcher = {
    setOppgavelistaOppgaver: (oppgaver) => {
      oppgavebenkStatsFetch({}, (result) =>
        setState({ ...state, oppgavelistaOppgaver: oppgaver, oppgpavebenkStats: result })
      )
    },
    setMinOppgavelisteOppgaver: (oppgaver) => {
      oppgavebenkStatsFetch({}, (result) =>
        setState({ ...state, minOppgavelisteOppgaver: oppgaver, oppgpavebenkStats: result })
      )
    },
    setGosysOppgavelisteOppgaver: (oppgaver) => setState({ ...state, gosysOppgavelisteOppgaver: oppgaver }),
    setOppgavebenkStats: (stats) => setState({ ...state, oppgpavebenkStats: stats }),
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
