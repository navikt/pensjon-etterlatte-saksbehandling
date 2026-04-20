import { useApiCall } from '~shared/hooks/useApiCall'
import { hentDokumentPDF } from '~shared/api/dokument'
import React, { useEffect, useState } from 'react'
import { Box, Detail, Heading } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import { Journalpost } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'

import { isSuccess, mapApiResult, mapResult, Result } from '~shared/api/apiUtils'

export default function VelgJournalpost({ journalpostStatus }: { journalpostStatus: Result<Journalpost> }) {
  const [dokument, hentDokument] = useApiCall(hentDokumentPDF)
  const [fileURL, setFileURL] = useState<string>()

  useEffect(() => {
    if (isSuccess(journalpostStatus) && !fileURL) {
      hentDokument(
        {
          journalpostId: journalpostStatus.data.journalpostId,
          dokumentInfoId: journalpostStatus.data.dokumenter[0].dokumentInfoId, // TODO: Sikre korrekt index
        },
        (bytes) => {
          const blob = new Blob([bytes], { type: 'application/pdf' })

          setFileURL(URL.createObjectURL(blob))
        }
      )
    }
  }, [journalpostStatus])

  useEffect(() => {
    if (!!fileURL)
      setTimeout(() => {
        URL.revokeObjectURL(fileURL)
      }, 1000)
  }, [fileURL])

  return (
    <JournalpostContainer>
      {mapResult(journalpostStatus, {
        pending: <Spinner label="Henter journalpost for bruker" />,
        error: (error) => (
          <ApiErrorAlert>
            {error.detail || 'Feil ved henting av journalpost. Kan ikke fortsette behandlingen.'}
          </ApiErrorAlert>
        ),
        success: (journalpost) => (
          <>
            <Heading size="medium" spacing>
              Journalpost ({journalpost.journalpostId})<Detail>{journalpost.tittel}</Detail>
            </Heading>

            {mapApiResult(
              dokument,
              <Spinner label="Klargjør forhåndsvisning av PDF" />,
              (error) => (
                <ApiErrorAlert>{error.detail || 'Feil ved henting av PDF'}</ApiErrorAlert>
              ),
              () => (!!fileURL ? <PdfViewer src={fileURL} /> : <></>)
            )}
          </>
        ),
      })}
    </JournalpostContainer>
  )
}

const JournalpostContainer = styled(Box)`
  padding: var(--ax-space-32);
  min-width: 680px;
`

const PdfViewer = styled.embed`
  min-width: 680px;
  width: 100%;
  height: 80vh;
  border: 1px solid var(--nav-dark-gray);
`
