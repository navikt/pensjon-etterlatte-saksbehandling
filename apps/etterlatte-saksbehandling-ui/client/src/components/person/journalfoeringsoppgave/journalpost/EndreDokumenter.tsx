import { DokumentInfo } from '~shared/types/Journalpost'
import { Alert, Button, Heading, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import { InputFlexRow } from '~components/person/journalfoeringsoppgave/journalpost/OppdaterJournalpost'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'

export const EndreDokumenter = ({
  initielleDokumenter,
  oppdater,
}: {
  initielleDokumenter: DokumentInfo[]
  oppdater: (dokumenter: DokumentInfo[]) => void
}) => {
  const oppdaterTittel = (id: string, tittel: string) => {
    const oppdaterteDokumenter = initielleDokumenter.map((dok) => {
      if (id === dok.dokumentInfoId) {
        return { ...dok, tittel }
      } else {
        return dok
      }
    })

    oppdater(oppdaterteDokumenter)
  }

  return (
    <div>
      <Heading size="small" spacing>
        Dokumenter
      </Heading>

      {initielleDokumenter.map((dokument) => (
        <Dokument key={dokument.dokumentInfoId} dokument={dokument} oppdater={oppdaterTittel} />
      ))}
    </div>
  )
}

const Dokument = ({
  dokument,
  oppdater,
}: {
  dokument: DokumentInfo
  oppdater: (id: string, tittel: string) => void
}) => {
  const [rediger, setRediger] = useState(false)
  const [nyTittel, setNyTittel] = useState(dokument.tittel)

  const lagre = () => {
    oppdater(dokument.dokumentInfoId, nyTittel)
    setRediger(false)
  }

  const avbryt = () => {
    setNyTittel(dokument.tittel)
    setRediger(false)
  }

  return (
    <div key={dokument.dokumentInfoId}>
      {rediger ? (
        <>
          <TextField label="Dokumenttittel" value={nyTittel} onChange={(e) => setNyTittel(e.target.value)} />

          <br />

          <FlexRow justify="right">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button variant="secondary" onClick={lagre} disabled={nyTittel === dokument.tittel} size="small">
              Lagre
            </Button>
          </FlexRow>
        </>
      ) : (
        <InputFlexRow>
          <Info label="Dokumenttittel" tekst={nyTittel} />

          <Button size="small" variant="secondary" onClick={() => setRediger(true)}>
            Endre
          </Button>
        </InputFlexRow>
      )}

      <br />
      {!dokument.tittel && <Alert variant="warning">Dokumentet mangler tittel!</Alert>}
    </div>
  )
}
