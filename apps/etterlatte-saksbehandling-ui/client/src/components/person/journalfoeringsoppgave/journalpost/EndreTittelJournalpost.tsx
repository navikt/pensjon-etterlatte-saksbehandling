import { Journalpost } from '~shared/types/Journalpost'
import Header from '@navikt/ds-react/esm/table/Header'
import { Alert, Button, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import { FlexRow } from '~shared/styled'

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

  const lagreTittelJournalpost = () => {
    oppdaterTittelJournalpost(nyTittel)
    setRediger(false)
  }
  return (
    <>
      <Header>Journalpost tittel</Header>
      {journalpost.tittel ? <p>{journalpost.tittel}</p> : <Alert variant="warning">Journalposten tittel er tom</Alert>}
      {rediger ? (
        <>
          <TextField label="Journalpost tittel" value={nyTittel} onChange={(e) => setNyTittel(e.target.value)} />

          <br />

          <FlexRow justify="right">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button variant="secondary" onClick={lagreTittelJournalpost} size="small">
              Lagre
            </Button>
          </FlexRow>
        </>
      ) : (
        <>
          <Button variant="secondary" size="small" onClick={() => setRediger(true)}>
            Endre tittel
          </Button>
        </>
      )}
    </>
  )
}
