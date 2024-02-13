import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { FilterRad } from '~components/oppgavebenk/FilterRad'
import { Filter, filtrerOppgaver } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { oppdaterTildeling } from '~components/oppgavebenk/oppgaveutils'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/OppgaveFeilWrapper'
import { OppgaveDTO, Saksbehandler } from '~shared/api/oppgaver'
import { Result } from '~shared/api/apiUtils'

export const OppgavelistaWrapper = (props: {
  hovedsideOppgaver: OppgaveDTO[]
  hentHovedsideOppgaverAlle: () => void
  hovedsideOppgaverResult: Result<OppgaveDTO[]>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  hentHovedsideOppgaver: (oppgavestatusFilter: Array<string>) => void
  hovedsideFilter: Filter
  setHovedsideFilter: React.Dispatch<React.SetStateAction<Filter>>
  setHovedsideOppgaver: React.Dispatch<React.SetStateAction<OppgaveDTO[]>>
  saksbehandlereIEnhet: Array<Saksbehandler>
}) => {
  const {
    hovedsideOppgaver,
    hentHovedsideOppgaverAlle,
    hovedsideOppgaverResult,
    gosysOppgaverResult,
    hentHovedsideOppgaver,
    hovedsideFilter,
    setHovedsideFilter,
    setHovedsideOppgaver,
    saksbehandlereIEnhet,
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
        <Oppgavelista
          oppgaver={filtrerteOppgaver}
          oppdaterTildeling={() => oppdaterTildeling(setHovedsideOppgaver, hovedsideOppgaver)}
          oppdaterFrist={() => {}}
          totaltAntallOppgaver={hovedsideOppgaver.length}
          erMinOppgaveliste={false}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
        />
      </>
    </OppgaveFeilWrapper>
  )
}
