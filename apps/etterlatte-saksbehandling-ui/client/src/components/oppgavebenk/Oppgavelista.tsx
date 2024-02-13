import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { FilterRad } from '~components/oppgavebenk/oppgaveFiltrering/FilterRad'
import { Filter, filtrerOppgaver } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/components/OppgaveFeilWrapper'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { Result } from '~shared/api/apiUtils'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { Dispatch, SetStateAction } from 'react'

export const Oppgavelista = (props: {
  hovedsideOppgaver: OppgaveDTO[]
  hentHovedsideOppgaverAlle: () => void
  hovedsideOppgaverResult: Result<OppgaveDTO[]>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  hentHovedsideOppgaver: (oppgavestatusFilter: Array<string>) => void
  hovedsideFilter: Filter
  setHovedsideFilter: Dispatch<SetStateAction<Filter>>
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterSaksbehandlerTildeling: (oppgave: OppgaveDTO, saksbehandler: string | null, versjon: number | null) => void
}) => {
  const {
    hovedsideOppgaver,
    hentHovedsideOppgaverAlle,
    hovedsideOppgaverResult,
    gosysOppgaverResult,
    hentHovedsideOppgaver,
    hovedsideFilter,
    setHovedsideFilter,
    saksbehandlereIEnhet,
    oppdaterSaksbehandlerTildeling,
  } = props

  const mutableOppgaver = hovedsideOppgaver.concat()

  const filtrerteOppgaver = filtrerOppgaver(
    hovedsideFilter.enhetsFilter,
    hovedsideFilter.fristFilter,
    hovedsideFilter.saksbehandlerFilter,
    hovedsideFilter.ytelseFilter,
    hovedsideFilter.oppgavestatusFilter,
    hovedsideFilter.oppgavetypeFilter,
    hovedsideFilter.oppgavekildeFilter,
    mutableOppgaver,
    hovedsideFilter.fnrFilter
  )

  return (
    <OppgaveFeilWrapper oppgaver={hovedsideOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
      <>
        <FilterRad
          hentAlleOppgaver={hentHovedsideOppgaverAlle}
          hentOppgaverStatus={(oppgavestatusFilter: Array<string>) => hentHovedsideOppgaver(oppgavestatusFilter)}
          filter={hovedsideFilter}
          setFilter={setHovedsideFilter}
          alleOppgaver={hovedsideOppgaver}
        />
        <Oppgaver
          oppgaver={filtrerteOppgaver}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
          oppdaterFrist={() => {}}
          totaltAntallOppgaver={hovedsideOppgaver.length}
          erMinOppgaveliste={false}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
        />
      </>
    </OppgaveFeilWrapper>
  )
}
