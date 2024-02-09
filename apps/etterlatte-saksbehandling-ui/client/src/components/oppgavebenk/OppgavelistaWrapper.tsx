import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { FilterRad } from '~components/oppgavebenk/FilterRad'
import { filtrerOppgaver } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { oppdaterTildeling } from '~components/oppgavebenk/oppgaveutils'
import { useOppgaverEffect } from '~components/oppgavebenk/useOppgaverEffect'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/OppgaveFeilWrapper'

export const OppgavelistaWrapper = () => {
  const {
    hovedsideOppgaver,
    hentHovedsideOppgaverAlle,
    hovedsideOppgaverResult,
    gosysOppgaverResult,
    hentHovedsideOppgaver,
    hovedsideFilter,
    setHovedsideFilter,
    setHovedsideOppgaver,
  } = useOppgaverEffect()

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
    hovedsideFilter.fristSortering,
    hovedsideFilter.fnrSortering,
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
          hentOppgaver={() => {}}
          filter={hovedsideFilter}
          setFilter={setHovedsideFilter}
          totaltAntallOppgaver={hovedsideOppgaver.length}
          erMinOppgaveliste={false}
        />
      </>
    </OppgaveFeilWrapper>
  )
}
