import React, { useState } from 'react'
import { Alert, BodyShort, Heading, Panel } from '@navikt/ds-react'
import { BrevTittelModal } from '~components/person/brev/tittel/BrevTittelModal'
import { FlexRow } from '~shared/styled'

interface Props {
  brevId: number
  sakId: number
  tittel: string
  kanRedigeres: boolean
}

export default function BrevTittel({ brevId, sakId, tittel, kanRedigeres }: Props) {
  const [nyTittel, setNyTittel] = useState(tittel)

  return (
    <Panel border>
      <FlexRow justify="space-between">
        <Heading spacing level="2" size="medium">
          Tittel
        </Heading>
        {kanRedigeres && (
          <BrevTittelModal nyTittel={nyTittel} setNyTittel={setNyTittel} brevId={brevId} sakId={sakId} />
        )}
      </FlexRow>

      {nyTittel ? (
        <>
          <BodyShort spacing>{nyTittel}</BodyShort>
          {tittel !== nyTittel && (
            <Alert variant="success" size="small">
              Tittel oppdatert!
            </Alert>
          )}
        </>
      ) : (
        <Alert variant="warning" size="small">
          Tittel mangler
        </Alert>
      )}
    </Panel>
  )
}
