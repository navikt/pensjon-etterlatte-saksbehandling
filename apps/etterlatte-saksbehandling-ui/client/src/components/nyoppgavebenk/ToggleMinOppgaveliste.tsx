import { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { Oppgavelista } from '~components/nyoppgavebenk/Oppgavelista'
import { MinOppgaveliste } from '~components/nyoppgavebenk/minoppgaveliste/MinOppgaveliste'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentNyeOppgaver, OppgaveDTOny } from '~shared/api/oppgaverny'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import { FilterRad } from '~components/nyoppgavebenk/FilterRad'
import { Filter, filtrerOppgaver, initialFilter } from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { useAppSelector } from '~store/Store'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'
const TabsWidth = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`

export const ToggleMinOppgaveliste = () => {
  const [filter, setFilter] = useState<Filter>(initialFilter())
  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')
  const [oppgaver, hentOppgaver] = useApiCall(hentNyeOppgaver)
  const [hentedeOppgaver, setHentedeOppgaver] = useState<ReadonlyArray<OppgaveDTOny>>([])
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  const mineOppgaver = hentedeOppgaver.filter((o) => o.saksbehandler === user.ident)

  const hentOppgaverWrapper = () => {
    hentOppgaver({}, (oppgaver) => {
      setHentedeOppgaver(oppgaver)
    })
  }

  useEffect(() => {
    hentOppgaverWrapper()
  }, [])

  const mutableOppgaver = hentedeOppgaver.concat()

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
    <>
      <TabsWidth value={oppgaveListeValg} onChange={(e) => setOppgaveListeValg(e as OppgavelisteToggle)}>
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label="Oppgavelisten" icon={<InboxIcon />} />
          <Tabs.Tab value="MinOppgaveliste" label={`Min oppgaveliste (${mineOppgaver.length})`} icon={<PersonIcon />} />
        </Tabs.List>
      </TabsWidth>
      {oppgaveListeValg === 'Oppgavelista' && (
        <FilterRad hentOppgaver={hentOppgaverWrapper} filter={filter} setFilter={setFilter} />
      )}

      {isPending(oppgaver) && <Spinner visible={true} label={'Henter nye oppgaver'} />}
      {isFailure(oppgaver) && <ApiErrorAlert>Kunne ikke hente oppgaver</ApiErrorAlert>}
      {isSuccess(oppgaver) && (
        <>
          {oppgaveListeValg === 'Oppgavelista' && (
            <Oppgavelista
              oppgaver={hentedeOppgaver}
              filtrerteOppgaver={filtrerteOppgaver}
              hentOppgaver={hentOppgaverWrapper}
            />
          )}
          {oppgaveListeValg === 'MinOppgaveliste' && (
            <MinOppgaveliste oppgaver={mineOppgaver} hentOppgaver={hentOppgaverWrapper} />
          )}
        </>
      )}
    </>
  )
}
