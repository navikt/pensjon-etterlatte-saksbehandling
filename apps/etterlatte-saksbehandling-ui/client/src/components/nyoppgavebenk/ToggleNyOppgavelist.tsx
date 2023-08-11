import { useEffect, useState } from 'react'
import Oppgavebenken from '~components/oppgavebenken/Oppgavebenken'
import { Button } from '@navikt/ds-react'
import styled from 'styled-components'
import { ToggleMinOppgaveliste } from '~components/nyoppgavebenk/ToggleMinOppgaveliste'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import Spinner from '~shared/Spinner'

const OppgavebenkContainer = styled.div`
  padding: 2rem;
`
const FEATURE_TOGGLE_VIS_NY_OPPGAVELISTE = 'pensjon-etterlatte.vis-ny-oppgaveliste'

export const ToggleNyOppgaveliste = () => {
  const [visNyListe, setVisNyListe] = useState<boolean>(true)
  const [brytere, getBrytere] = useApiCall(hentFunksjonsbrytere)

  useEffect(() => {
    getBrytere([FEATURE_TOGGLE_VIS_NY_OPPGAVELISTE])
  }, [])

  return mapApiResult(
    brytere,
    <Spinner label="Laster applikasjon" visible={true} />,
    (error) => {
      console.error('Kunne ikke hente inn funksjonsbrytere, fallbacker til å vise gammel oppgaveliste', error)
      return (
        <OppgavebenkContainer>
          <Oppgavebenken />
        </OppgavebenkContainer>
      )
    },
    ([nyOppgaveListeFeature]) => (
      <OppgavebenkContainer>
        {nyOppgaveListeFeature.enabled ? (
          <>
            <Button onClick={() => setVisNyListe(!visNyListe)}>
              {visNyListe ? 'Vis gammel liste' : 'Vis ny liste'}
            </Button>
            {visNyListe ? <ToggleMinOppgaveliste /> : <Oppgavebenken />}
          </>
        ) : (
          <Oppgavebenken />
        )}
      </OppgavebenkContainer>
    )
  )
}
