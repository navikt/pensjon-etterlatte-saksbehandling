import React, { useState } from 'react'
import { Alert, BodyShort, Heading, Panel } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { IBrev } from '~shared/types/Brev'
import { BrevSpraakModal } from '~components/person/brev/spraak/BrevSpraakModal'
import { formaterSpraak } from '~utils/formattering'

interface Props {
  brev: IBrev
  kanRedigeres: boolean
}

export default function BrevSpraak({ brev, kanRedigeres }: Props) {
  const [nyttSpraak, setNyttSpraak] = useState(brev.spraak)

  return (
    <Panel border>
      <FlexRow justify="space-between">
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
      </FlexRow>

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
    </Panel>
  )
}
