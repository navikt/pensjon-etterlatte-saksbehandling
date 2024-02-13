import React, { Dispatch, SetStateAction } from 'react'
import { VelgOppgavestatuser } from '~components/oppgavebenk/oppgaveFiltrering/VelgOppgavestatuser'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { oppdaterFrist } from '~components/oppgavebenk/utils/oppgaveutils'
import styled from 'styled-components'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/OppgaveFeilWrapper'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { Result } from '~shared/api/apiUtils'
import { Filter } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { Saksbehandler } from '~shared/types/saksbehandler'

export const MinOppgaveliste = (props: {
  minsideOppgaver: OppgaveDTO[]
  minsideOppgaverResult: Result<OppgaveDTO[]>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  minsideFilter: Filter
  setMinsideFilter: (filter: Filter) => void
  setMinsideOppgaver: Dispatch<SetStateAction<OppgaveDTO[]>>
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterSaksbehandlerTildeling: (oppgave: OppgaveDTO, saksbehandler: string | null, versjon: number | null) => void
}) => {
  const {
    minsideOppgaver,
    minsideOppgaverResult,
    gosysOppgaverResult,
    minsideFilter,
    setMinsideFilter,
    setMinsideOppgaver,
    saksbehandlereIEnhet,
    oppdaterSaksbehandlerTildeling,
  } = props

  return (
    <OppgaveFeilWrapper oppgaver={minsideOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
      <>
        <ValgWrapper>
          <VelgOppgavestatuser
            value={minsideFilter.oppgavestatusFilter}
            onChange={(oppgavestatusFilter) => {
              setMinsideFilter({ ...minsideFilter, oppgavestatusFilter })
            }}
          />
        </ValgWrapper>
        <Oppgavelista
          oppgaver={minsideOppgaver}
          oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
            oppdaterFrist(setMinsideOppgaver, minsideOppgaver, id, nyfrist, versjon)
          }
          oppdaterTildeling={(id, _saksbehandler, versjon) => oppdaterSaksbehandlerTildeling(id, null, versjon)}
          erMinOppgaveliste={true}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
        />
      </>
    </OppgaveFeilWrapper>
  )
}

const ValgWrapper = styled.div`
  margin-bottom: 2rem;
  width: 35rem;
`
