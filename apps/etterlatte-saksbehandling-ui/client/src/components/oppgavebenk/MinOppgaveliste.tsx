import React, { Dispatch, SetStateAction } from 'react'
import { VelgOppgavestatuser } from '~components/oppgavebenk/oppgaveFiltrering/VelgOppgavestatuser'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { oppdaterFrist } from '~components/oppgavebenk/utils/oppgaveutils'
import styled from 'styled-components'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/components/OppgaveFeilWrapper'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { Result } from '~shared/api/apiUtils'
import { Filter } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { Saksbehandler } from '~shared/types/saksbehandler'

interface Props {
  oppgaver: OppgaveDTO[]
  setOppgaver: Dispatch<SetStateAction<OppgaveDTO[]>>
  oppgaverResult: Result<OppgaveDTO[]>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  filter: Filter
  setFilter: (filter: Filter) => void
  saksbehandlereIEnheter: Array<Saksbehandler>
  oppdaterSaksbehandlerTildeling: (oppgave: OppgaveDTO, saksbehandler: string | null, versjon: number | null) => void
}

export const MinOppgaveliste = ({
  oppgaver,
  setOppgaver,
  oppgaverResult,
  gosysOppgaverResult,
  filter,
  setFilter,
  saksbehandlereIEnheter,
  oppdaterSaksbehandlerTildeling,
}: Props) => {
  return (
    <OppgaveFeilWrapper oppgaver={oppgaverResult} gosysOppgaver={gosysOppgaverResult}>
      <>
        <ValgWrapper>
          <VelgOppgavestatuser
            value={filter.oppgavestatusFilter}
            onChange={(oppgavestatusFilter) => {
              setFilter({ ...filter, oppgavestatusFilter })
            }}
          />
        </ValgWrapper>
        <Oppgaver
          oppgaver={oppgaver}
          oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
            oppdaterFrist(setOppgaver, oppgaver, id, nyfrist, versjon)
          }
          oppdaterTildeling={(id, _saksbehandler, versjon) => oppdaterSaksbehandlerTildeling(id, null, versjon)}
          erMinOppgaveliste={true}
          saksbehandlereIEnhet={saksbehandlereIEnheter}
        />
      </>
    </OppgaveFeilWrapper>
  )
}

const ValgWrapper = styled.div`
  margin-bottom: 2rem;
  width: 35rem;
`
