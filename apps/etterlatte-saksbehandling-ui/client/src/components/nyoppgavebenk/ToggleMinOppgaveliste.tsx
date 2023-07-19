import { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { Oppgavelista } from '~components/nyoppgavebenk/Oppgavelista'
import { MinOppgaveliste } from '~components/nyoppgavebenk/MinOppgaveliste'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentNyeOppgaver, OppgaveDTOny } from '~shared/api/oppgaverny'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'

export const ToggleMinOppgaveliste = () => {
  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')
  const [oppgaver, hentOppgaver] = useApiCall(hentNyeOppgaver)
  const [hentedeOppgaver, setHentedeOppgaver] = useState<ReadonlyArray<OppgaveDTOny>>([])
  useEffect(() => {
    hentOppgaver({}, (oppgaver) => {
      setHentedeOppgaver(oppgaver)
    })
  }, [])
  return (
    <>
      <Tabs value={oppgaveListeValg} onChange={(e) => setOppgaveListeValg(e as OppgavelisteToggle)}>
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label="Oppgavelisten" icon={<InboxIcon />} />
          <Tabs.Tab value="MinOppgaveliste" label="Min oppgaeliste" icon={<PersonIcon />} />
        </Tabs.List>
      </Tabs>
      {isPending(oppgaver) && <Spinner visible={true} label={'henter nye oppgaver'} />}
      {isFailure(oppgaver) && <ApiErrorAlert>Kunne ikke hente oppgaver</ApiErrorAlert>}
      {isSuccess(oppgaver) && <>Hentet antall oppgaver: {hentedeOppgaver?.length}</>}
      {isSuccess(oppgaver) && (
        <>
          {oppgaveListeValg === 'Oppgavelista' && <Oppgavelista oppgaver={hentedeOppgaver} />}
          {oppgaveListeValg === 'MinOppgaveliste' && <MinOppgaveliste oppgaver={hentedeOppgaver} />}
        </>
      )}
    </>
  )
}
