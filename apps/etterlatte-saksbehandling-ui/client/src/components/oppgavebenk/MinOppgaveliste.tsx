import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, hentOppgaverMedStatus } from '~shared/api/oppgaver'
import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { Filter, minOppgavelisteFiltre } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { useEffect, useState } from 'react'
import { oppdaterTildeling } from '~components/oppgavebenk/oppgaveutils'
import styled from 'styled-components'
import { useAppDispatch } from '~store/Store'
import { settMinOppgavelisteLengde } from '~store/reducers/OppgavelisteReducer'
import { useOppgaverEffect } from '~components/oppgavebenk/useOppgaverEffect'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/OppgaveFeilWrapper'

export const MinOppgaveliste = () => {
  const dispatch = useAppDispatch()

  const [filter, setFilter] = useState<Filter>(minOppgavelisteFiltre())

  const [oppgaver, hentOppgaverStatusFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaver, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)

  const hentAlleOppgaver = () => {
    hentOppgaverStatusFetch({ oppgavestatusFilter: filter.oppgavestatusFilter, minOppgavelisteIdent: true })
    hentGosysOppgaverFunc({})
  }

  const { hentedeOppgaver, setHentedeOppgaver } = useOppgaverEffect({
    oppgaver,
    gosysOppgaver,
    hentAlleOppgaver,
    filtrerGosysOppgaverForInnloggetSaksbehandler: true,
  })

  useEffect(() => {
    dispatch(settMinOppgavelisteLengde(hentedeOppgaver.length))
  }, [hentedeOppgaver])

  return (
    <OppgaveFeilWrapper oppgaver={oppgaver} gosysOppgaver={gosysOppgaver}>
      <>
        <ValgWrapper>
          <VelgOppgavestatuser
            value={filter.oppgavestatusFilter}
            onChange={(oppgavestatusFilter) => {
              hentOppgaverStatusFetch({
                oppgavestatusFilter: oppgavestatusFilter,
                minOppgavelisteIdent: true,
              })
              setFilter({ ...filter, oppgavestatusFilter })
            }}
          />
        </ValgWrapper>
        <Oppgavelista
          oppgaver={hentedeOppgaver}
          hentOppgaver={hentAlleOppgaver}
          filter={filter}
          setFilter={setFilter}
          oppdaterTildeling={(id, _saksbehandler, versjon) =>
            oppdaterTildeling(setHentedeOppgaver, hentedeOppgaver)(id, null, versjon)
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
