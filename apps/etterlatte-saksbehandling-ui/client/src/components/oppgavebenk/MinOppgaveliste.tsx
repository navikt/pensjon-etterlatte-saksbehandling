import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { oppdaterTildeling } from '~components/oppgavebenk/oppgaveutils'
import styled from 'styled-components'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/OppgaveFeilWrapper'
import { useOppgaverEffect } from '~components/oppgavebenk/useOppgaverEffect'

export const MinOppgaveliste = () => {
  const {
    minsideOppgaver,
    minsideOppgaverResult,
    gosysOppgaverResult,
    minsideFilter,
    hentMinsideOppgaver,
    setMinsideFilter,
    setMinsideOppgaver,
  } = useOppgaverEffect()

  return (
    <OppgaveFeilWrapper oppgaver={minsideOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
      <>
        <ValgWrapper>
          <VelgOppgavestatuser
            value={minsideFilter.oppgavestatusFilter}
            onChange={(oppgavestatusFilter) => {
              hentMinsideOppgaver(oppgavestatusFilter)
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
