import React, { useState } from 'react'
import { Alert, BodyShort, Box, Heading, HStack } from '@navikt/ds-react'
import { IBrev } from '~shared/types/Brev'
import { BrevSpraakModal } from '~components/person/brev/spraak/BrevSpraakModal'
import { formaterSpraak } from '~utils/formatering/formatering'

interface Props {
  brev: IBrev
  kanRedigeres: boolean
}

export default function BrevSpraak({ brev, kanRedigeres }: Props) {
  const [nyttSpraak, setNyttSpraak] = useState(brev.spraak)

  return (
    <Box padding="space-4" borderWidth="1" borderColor="border-neutral-subtle">
      <HStack gap="space-4" justify="space-between">
        <Heading spacing level="2" size="medium">
          Språk / målform
        </Heading>
        {kanRedigeres && (
          <BrevSpraakModal
            nyttSpraak={nyttSpraak}
            settNyttSpraak={setNyttSpraak}
            brevId={brev.id}
            sakId={brev.sakId}
            behandlingId={brev.behandlingId}
            brevtype={brev.brevtype}
          />
        )}
      </HStack>

      {nyttSpraak ? (
        <>
          <BodyShort spacing>{formaterSpraak(nyttSpraak)}</BodyShort>
          {brev.spraak !== nyttSpraak && (
            <Alert variant="success" size="small">
              Språk oppdatert!
            </Alert>
          )}
        </>
      ) : (
        <Alert variant="warning" size="small">
          Språk mangler
        </Alert>
      )}
    </Box>
  )
}
