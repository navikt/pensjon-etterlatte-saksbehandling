import React, { useState } from 'react'
import { Alert, BodyShort, Heading, Panel } from '@navikt/ds-react'
import RedigerBrevTittelModal from '~components/person/brev/tittel/RedigerBrevTittelModal'

export default function BrevTittel({ brevId, sakId, tittel }: { brevId: number; sakId: number; tittel: string }) {
  const [nyTittel, setNyTittel] = useState(tittel)

  return (
    <Panel border>
      <Heading spacing level="2" size="medium">
        Tittel
        <RedigerBrevTittelModal
          brevId={brevId}
          sakId={sakId}
          tittel={tittel}
          oppdater={(tittel) => setNyTittel(tittel)}
        />
      </Heading>

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
