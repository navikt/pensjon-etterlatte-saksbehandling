import React, { useState } from 'react'
import { Alert, BodyShort, Box, Heading, HStack } from '@navikt/ds-react'
import { BrevTittelModal } from '~components/person/brev/tittel/BrevTittelModal'

interface Props {
  brevId: number
  sakId: number
  tittel: string
  kanRedigeres: boolean
  manueltBrev?: boolean
}

export default function BrevTittel({ brevId, sakId, tittel, kanRedigeres, manueltBrev = false }: Props) {
  const [nyTittel, setNyTittel] = useState(tittel)

  return (
    <Box padding="space-4" borderWidth="1" borderColor="border-neutral-subtle">
      <HStack gap="space-4" justify="space-between">
        <Heading spacing level="2" size="medium">
          Tittel
        </Heading>
        {kanRedigeres && (
          <BrevTittelModal nyTittel={nyTittel} setNyTittel={setNyTittel} brevId={brevId} sakId={sakId} />
        )}
      </HStack>

      {nyTittel ? (
        <>
          <BodyShort spacing>{nyTittel}</BodyShort>
          {tittel !== nyTittel && (
            <Alert variant="success" size="small">
              Tittel oppdatert!
            </Alert>
          )}
          {tittel === nyTittel && manueltBrev && (
            <Alert variant="warning" size="small">
              Språk er ikke bokmål, så dobbeltsjekk tittelen
            </Alert>
          )}
        </>
      ) : (
        <Alert variant="warning" size="small">
          Tittel mangler
        </Alert>
      )}
    </Box>
  )
}
