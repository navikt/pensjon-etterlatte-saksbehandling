import { Button, Heading } from '@navikt/ds-react'
import { useState } from 'react'
import { MottatteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/MottatteSeder'

export const BosattUtland = () => {
  const [bosattUtland, setBosattUtland] = useState<boolean>(false) //TODO: koble opp mot hva som er valgt opp i utenlandstilknytning
  return (
    <>
      <Heading level="2" size="medium" style={{ marginTop: '2rem' }}>
        Er dette en bosatt utland sak?
      </Heading>
      <Button onClick={() => setBosattUtland((prev) => !prev)}>Klikk her for å åpne SED/land oversikt</Button>
      {bosattUtland && <MottatteSeder />}
      {bosattUtland && <MottatteSeder />}
    </>
  )
}
