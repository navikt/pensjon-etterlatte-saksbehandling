import React, { useState } from 'react'
import { Alert, BodyShort, Button, Heading, Panel } from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import { BrevTittelModal } from '~components/person/brev/tittel/BrevTittelModal'

interface Props {
  brevId: number
  sakId: number
  tittel: string
}

export default function BrevTittel({ brevId, sakId, tittel }: Props) {
  const [nyTittel, setNyTittel] = useState(tittel)

  const [erModalAapen, setErModalAapen] = useState<boolean>(false)

  return (
    <>
      <Panel border>
        <div>
          <Heading spacing level="2" size="medium">
            Tittel
          </Heading>
          <Button
            variant="secondary"
            onClick={() => setErModalAapen(true)}
            icon={<DocPencilIcon />}
            style={{ float: 'right' }}
            size="small"
          />
        </div>

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
      <BrevTittelModal
        isOpen={erModalAapen}
        setIsOpen={setErModalAapen}
        nyTittel={nyTittel}
        setNyTittel={setNyTittel}
        brevId={brevId}
        sakId={sakId}
      />
    </>
  )
}
