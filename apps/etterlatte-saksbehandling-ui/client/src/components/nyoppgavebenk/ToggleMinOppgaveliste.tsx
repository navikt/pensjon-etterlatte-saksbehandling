import { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { Oppgavelista } from '~components/nyoppgavebenk/Oppgavelista'
import { MinOppgaveliste } from '~components/nyoppgavebenk/minoppgaveliste/MinOppgaveliste'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, hentNyeOppgaver, OppgaveDTOny } from '~shared/api/oppgaverny'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import { FilterRad } from '~components/nyoppgavebenk/FilterRad'
import { Filter, filtrerOppgaver, initialFilter } from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'
const TabsWidth = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`

export const ToggleMinOppgaveliste = () => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  const [filter, setFilter] = useState<Filter>(initialFilter())
  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')
  const [oppgaver, hentOppgaver] = useApiCall(hentNyeOppgaver)
  const [gosysOppgaver, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)

  const [hentedeOppgaver, setHentedeOppgaver] = useState<OppgaveDTOny[]>([])

  useEffect(() => {
    if (isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      const alleOppgaver = [...oppgaver.data, ...gosysOppgaver.data].sort(
        (a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime()
      )
      setHentedeOppgaver(alleOppgaver)
    } else if (isSuccess(oppgaver) && isFailure(gosysOppgaver)) {
      setHentedeOppgaver(oppgaver.data)
    }
  }, [oppgaver, gosysOppgaver])

  const hentAlleOppgaver = () => {
    hentOppgaver({})
    hentGosysOppgaverFunc({})
  }

  useEffect(() => hentAlleOppgaver(), [])

  const oppdaterTildeling = (id: string, saksbehandler: string | null) => {
    const oppdatertOppgaveState = [...hentedeOppgaver]
    const index = oppdatertOppgaveState.findIndex((o) => o.id === id)
    oppdatertOppgaveState[index].saksbehandler = saksbehandler
    setHentedeOppgaver(oppdatertOppgaveState)
  }

  const mutableOppgaver = hentedeOppgaver.concat()
  const innloggetSaksbehandleroppgaver = mutableOppgaver.filter((o) => o.saksbehandler === user.ident)
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
    <Container>
      <TabsWidth value={oppgaveListeValg} onChange={(e) => setOppgaveListeValg(e as OppgavelisteToggle)}>
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label="Oppgavelisten" icon={<InboxIcon />} />
          <Tabs.Tab
            value="MinOppgaveliste"
            label={`Min oppgaveliste (${innloggetSaksbehandleroppgaver.length})`}
            icon={<PersonIcon />}
          />
        </Tabs.List>
      </TabsWidth>

      {isPending(oppgaver) && <Spinner visible={true} label={'Henter nye oppgaver'} />}
      {isFailure(oppgaver) && <ApiErrorAlert>Kunne ikke hente oppgaver</ApiErrorAlert>}
      {isFailure(gosysOppgaver) && <ApiErrorAlert>Kunne ikke hente gosys oppgaver</ApiErrorAlert>}
      {isSuccess(oppgaver) && (
        <>
          {oppgaveListeValg === 'Oppgavelista' && (
            <>
              <FilterRad hentOppgaver={hentAlleOppgaver} filter={filter} setFilter={setFilter} />
              <Oppgavelista
                oppgaver={hentedeOppgaver}
                filtrerteOppgaver={filtrerteOppgaver}
                oppdaterTildeling={oppdaterTildeling}
              />
            </>
          )}
          {oppgaveListeValg === 'MinOppgaveliste' && (
            <MinOppgaveliste
              oppgaver={innloggetSaksbehandleroppgaver}
              hentOppgaver={hentAlleOppgaver}
              oppdaterTildeling={(id) => oppdaterTildeling(id, null)}
            />
          )}
        </>
      )}
    </Container>
  )
}
