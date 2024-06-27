import { DokumentInfo } from '~shared/types/Journalpost'
import { Alert, Box, Button, Heading, HStack, TextField, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

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
        <Box background="bg-subtle" padding="4" borderColor="border-subtle" borderWidth="1" borderRadius="medium">
          <VStack gap="4">
            <TextField label="Dokumenttittel" value={nyTittel} onChange={(e) => setNyTittel(e.target.value)} />

            <HStack gap="4" justify="end">
              <Button variant="tertiary" onClick={avbryt} size="small">
                Avbryt
              </Button>
              <Button onClick={lagre} disabled={nyTittel === dokument.tittel} size="small">
                Lagre
              </Button>
            </HStack>
          </VStack>
        </Box>
      ) : (
        <HStack gap="4" justify="space-between" align="center">
          <Info label="Dokumenttittel" tekst={nyTittel} />

          <Button size="small" variant="secondary" onClick={() => setRediger(true)}>
            Endre
          </Button>
        </HStack>
      )}

      <br />
      {!dokument.tittel && (
        <Alert variant="warning" size="small">
          Dokumentet mangler tittel!
        </Alert>
      )}
    </div>
  )
}
