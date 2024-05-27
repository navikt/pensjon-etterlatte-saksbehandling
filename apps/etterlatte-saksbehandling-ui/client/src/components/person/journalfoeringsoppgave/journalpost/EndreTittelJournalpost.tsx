import { Journalpost } from '~shared/types/Journalpost'
import { Alert, BodyShort, Button, Heading, HStack, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import { InputFlexRow } from './OppdaterJournalpost'

export const EndreTittelJournalpost = ({
  journalpost,
  oppdaterTittelJournalpost,
}: {
  journalpost: Journalpost
  oppdaterTittelJournalpost: (nyTittel: string) => void
}) => {
  const [rediger, setRediger] = useState(false)
  const [nyTittel, setNyTittel] = useState<string>(journalpost.tittel)

  const avbryt = () => {
    setNyTittel(journalpost.tittel)
    setRediger(false)
  }

  const lagre = () => {
    oppdaterTittelJournalpost(nyTittel)
    setRediger(false)
  }
  return (
    <div>
      <Heading size="small" spacing>
        Tittel
      </Heading>

      {rediger ? (
        <>
          <TextField label="Tittel" value={nyTittel} onChange={(e) => setNyTittel(e.target.value)} hideLabel={true} />

          <br />

          <HStack gap="4" justify="end">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button variant="secondary" onClick={lagre} disabled={nyTittel === journalpost.tittel} size="small">
              Lagre
            </Button>
          </HStack>
        </>
      ) : (
        <>
          <InputFlexRow>
            <BodyShort as="div">{nyTittel}</BodyShort>

            <Button variant="secondary" size="small" onClick={() => setRediger(true)}>
              Endre
            </Button>
          </InputFlexRow>

          <br />
          {!nyTittel && (
            <Alert variant="warning" size="small">
              Journalposten mangler tittel
            </Alert>
          )}
        </>
      )}
    </div>
  )
}
