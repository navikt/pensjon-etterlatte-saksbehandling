import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { oppdaterFrist } from '~components/oppgavebenk/oppgaveutils'
import styled from 'styled-components'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/OppgaveFeilWrapper'
import { OppgaveDTO, Saksbehandler } from '~shared/api/oppgaver'
import { Result } from '~shared/api/apiUtils'
import { Filter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

export const MinOppgaveliste = (props: {
  minsideOppgaver: OppgaveDTO[]
  minsideOppgaverResult: Result<OppgaveDTO[]>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  minsideFilter: Filter
  setMinsideFilter: (filter: Filter) => void
  setMinsideOppgaver: React.Dispatch<React.SetStateAction<OppgaveDTO[]>>
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
