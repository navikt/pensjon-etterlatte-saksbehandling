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

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'
const TabsWidth = styled(Tabs)`
  max-width: 20em;
  margin-bottom: 2rem;
`

export const ToggleMinOppgaveliste = () => {
  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')
  const [oppgaver, hentOppgaver] = useApiCall(hentNyeOppgaver)
  const [hentedeOppgaver, setHentedeOppgaver] = useState<ReadonlyArray<OppgaveDTOny>>([])
  const hentOppgaverWrapper = () => {
    hentOppgaver({}, (oppgaver) => {
      setHentedeOppgaver(oppgaver)
    })
  }
  useEffect(() => {
    hentOppgaverWrapper()
  }, [])
  return (
    <>
      <TabsWidth value={oppgaveListeValg} onChange={(e) => setOppgaveListeValg(e as OppgavelisteToggle)}>
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label="Oppgavelisten" icon={<InboxIcon />} />
          <Tabs.Tab value="MinOppgaveliste" label="Min oppgaveliste" icon={<PersonIcon />} />
        </Tabs.List>
      </TabsWidth>
      {isPending(oppgaver) && <Spinner visible={true} label={'Henter nye oppgaver'} />}
      {isFailure(oppgaver) && <ApiErrorAlert>Kunne ikke hente oppgaver</ApiErrorAlert>}
      {isSuccess(oppgaver) && (
        <>
          {oppgaveListeValg === 'Oppgavelista' && (
            <Oppgavelista oppgaver={hentedeOppgaver} hentOppgaver={hentOppgaverWrapper} />
          )}
          {oppgaveListeValg === 'MinOppgaveliste' && <MinOppgaveliste oppgaver={hentedeOppgaver} />}
        </>
      )}
    </>
  )
}
