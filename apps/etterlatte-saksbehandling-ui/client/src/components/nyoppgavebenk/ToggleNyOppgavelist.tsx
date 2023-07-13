import { useState } from 'react'
import Oppgavebenken from '~components/oppgavebenken/Oppgavebenken'
import { Oppgavelista } from '~components/nyoppgavebenk/Oppgavelista'
import { Button } from '@navikt/ds-react'
import styled from 'styled-components'

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`

//TODO: hvorfor er ikke denne riktig i prod?
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
          {visNyListe ? <Oppgavelista /> : <Oppgavebenken />}
        </>
      )}
    </OppgavebenkContainer>
  )
}
