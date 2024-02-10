import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { oppdaterTildeling } from '~components/oppgavebenk/oppgaveutils'
import styled from 'styled-components'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/OppgaveFeilWrapper'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { Result } from '~shared/api/apiUtils'
import { Filter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

export const MinOppgaveliste = (props: {
  minsideOppgaver: OppgaveDTO[]
  minsideOppgaverResult: Result<OppgaveDTO[]>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  minsideFilter: Filter
  setMinsideFilter: React.Dispatch<React.SetStateAction<Filter>>
  setMinsideOppgaver: React.Dispatch<React.SetStateAction<OppgaveDTO[]>>
}) => {
  const {
    minsideOppgaver,
    minsideOppgaverResult,
    gosysOppgaverResult,
    minsideFilter,
    setMinsideFilter,
    setMinsideOppgaver,
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
          hentOppgaver={() => {}}
          filter={minsideFilter}
          setFilter={setMinsideFilter}
          oppdaterTildeling={(id, _saksbehandler, versjon) =>
            oppdaterTildeling(setMinsideOppgaver, minsideOppgaver)(id, null, versjon)
          }
          erMinOppgaveliste={true}
        />
      </>
    </OppgaveFeilWrapper>
  )
}

const ValgWrapper = styled.div`
  margin-bottom: 2rem;
  width: 35rem;
`
