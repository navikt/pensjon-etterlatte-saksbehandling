import React from 'react'
import { Journalpost, Journalposttype } from '~shared/types/Journalpost'
import { FlexRow } from '~shared/styled'
import { UtsendingsinfoModal } from '~components/person/dokumenter/UtsendingsinfoModal'
import DokumentModal from '~components/person/dokumenter/DokumentModal'
import { Alert } from '@navikt/ds-react'

export const VisDokument = ({ dokument }: { dokument: Journalpost }) => {
  return (
    <FlexRow justify="right">
      {dokument.dokumenter.length === 1 ? (
        <>
          {dokument.dokumenter[0].dokumentvarianter[0].saksbehandlerHarTilgang ? (
            <>
              {dokument.journalposttype === Journalposttype.U && <UtsendingsinfoModal journalpost={dokument} />}
              <DokumentModal journalpost={dokument} />
            </>
          ) : (
            <Alert variant="warning" size="small">
              Ingen tilgang
            </Alert>
          )}
        </>
      ) : (
        <>
          {dokument.journalposttype === Journalposttype.U && <UtsendingsinfoModal journalpost={dokument} />}
          <DokumentModal journalpost={dokument} />
        </>
      )}
    </FlexRow>
  )
}
