import React, { ReactNode, useState } from 'react'
import { Alert, Heading } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'

export const PDLInfoAlert = (): ReactNode => {
  const [vis, setVis] = useState<boolean>(true)

  return (
    vis && (
      <FlexRow justify="center">
        <Alert variant="info" fullWidth closeButton onClose={() => setVis(false)}>
          <Heading size="small" level="3" spacing>
            Personopplysningene kommer i sanntid fra PDL
          </Heading>
          Personopplysningene som vises på denne siden kommer i sanntid fra PDL. Dette betyr at hvis PDL oppdaterer
          informasjonen for en person, vil denne siden også endre seg til å speile det. Det kan derfor være forskjell i
          informasjonen på denne siden og den som er gitt i en behandling.
        </Alert>
      </FlexRow>
    )
  )
}
