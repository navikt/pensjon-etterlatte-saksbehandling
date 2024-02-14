import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { FilterRad } from '~components/oppgavebenk/oppgaveFiltrering/FilterRad'
import { Filter, filtrerOppgaver } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/components/OppgaveFeilWrapper'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { Result } from '~shared/api/apiUtils'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { Dispatch, SetStateAction } from 'react'

interface Props {
  oppgaver: OppgaveDTO[]
  hentOppgavelistaOppgaver: (oppgavestatusFilter: Array<string>) => void
  hentAlleMinOppgavelisteOppgaver: () => void
  oppgavelistaOppgaverResult: Result<OppgaveDTO[]>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  filter: Filter
  setFilter: Dispatch<SetStateAction<Filter>>
  saksbehandlereIEnheter: Array<Saksbehandler>
  oppdaterSaksbehandlerTildeling: (oppgave: OppgaveDTO, saksbehandler: string | null, versjon: number | null) => void
}

export const Oppgavelista = ({
  oppgaver,
  hentAlleMinOppgavelisteOppgaver,
  oppgavelistaOppgaverResult,
  gosysOppgaverResult,
  hentOppgavelistaOppgaver,
  filter,
  setFilter,
  saksbehandlereIEnheter,
  oppdaterSaksbehandlerTildeling,
}: Props) => {
  const mutableOppgaver = oppgaver.concat()

  const filtrerteOppgaver = filtrerOppgaver(
    filter.enhetsFilter,
    filter.fristFilter,
    filter.saksbehandlerFilter,
    filter.ytelseFilter,
    filter.oppgavestatusFilter,
    filter.oppgavetypeFilter,
    filter.oppgavekildeFilter,
    mutableOppgaver,
    filter.fnrFilter
  )

  return (
    <OppgaveFeilWrapper oppgaver={oppgavelistaOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
      <>
        <FilterRad
          hentAlleOppgaver={hentAlleMinOppgavelisteOppgaver}
          hentOppgaverStatus={(oppgavestatusFilter: Array<string>) => hentOppgavelistaOppgaver(oppgavestatusFilter)}
          filter={filter}
          setFilter={setFilter}
          alleOppgaver={oppgaver}
        />
        <Oppgaver
          oppgaver={filtrerteOppgaver}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
          oppdaterFrist={() => {}}
          totaltAntallOppgaver={oppgaver.length}
          erMinOppgaveliste={false}
          saksbehandlereIEnhet={saksbehandlereIEnheter}
        />
      </>
    </OppgaveFeilWrapper>
  )
}
