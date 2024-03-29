import React, { createContext, ReactNode, useContext, useState } from 'react'
import { initialOppgavebenkState, OppgavebenkState } from '~components/oppgavebenk/state/oppgavebenkState'
import { OppgaveDTO } from '~shared/api/oppgaver'

export interface OppgavebenkStateDispatcher {
  setOppgavelistaOppgaver: (oppgaver: OppgaveDTO[]) => void
  setMinOppgavelisteOppgaver: (oppgaver: OppgaveDTO[]) => void
  setGosysOppgavelisteOppgaver: (oppgaver: OppgaveDTO[]) => void
}

const oppgavebenkContext = createContext(initialOppgavebenkState)
const oppgavebenkStateDispatch = createContext({} as OppgavebenkStateDispatcher)

const ProvideOppgavebenkContext = ({ children }: { children: ReactNode | Array<ReactNode> }): ReactNode => {
  const [state, setState] = useState<OppgavebenkState>(initialOppgavebenkState)

  const dispatcher: OppgavebenkStateDispatcher = {
    setOppgavelistaOppgaver: (oppgaver) => setState({ ...state, oppgavelistaOppgaver: oppgaver }),
    setMinOppgavelisteOppgaver: (oppgaver) => setState({ ...state, minOppgavelisteOppgaver: oppgaver }),
    setGosysOppgavelisteOppgaver: (oppgaver) => setState({ ...state, gosysOppgavelisteOppgaver: oppgaver }),
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
