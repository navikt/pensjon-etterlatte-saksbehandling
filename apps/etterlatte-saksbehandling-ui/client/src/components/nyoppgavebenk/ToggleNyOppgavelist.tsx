import { useState } from 'react'
import Oppgavebenken from '~components/oppgavebenken/Oppgavebenken'
import { Button } from '@navikt/ds-react'
import styled from 'styled-components'
import { ToggleMinOppgaveliste } from '~components/nyoppgavebenk/ToggleMinOppgaveliste'

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`

const erIProd = process.env.NAIS_CLUSTER_NAME === 'prod-gcp'

export const ToggleNyOppgaveliste = () => {
  const [visNyListe, setVisNyListe] = useState<boolean>(false)

  return (
    <OppgavebenkContainer>
      {erIProd ? (
        <Oppgavebenken />
      ) : (
        <>
          <Button onClick={() => setVisNyListe(!visNyListe)}>{visNyListe ? 'Vis gammel liste' : 'Vis ny liste'}</Button>
          {visNyListe ? <ToggleMinOppgaveliste /> : <Oppgavebenken />}
        </>
      )}
    </OppgavebenkContainer>
  )
}
