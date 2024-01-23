import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleDokumenterInklPensjon, hentDokumentPDF } from '~shared/api/dokument'
import React, { useEffect, useState } from 'react'
import { Button, Detail, Heading, TextField } from '@navikt/ds-react'
import { fnrErGyldig } from '~utils/fnr'
import { ApiErrorAlert } from '~shared/error/ApiErrorAlert'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { Column, Container, FlexRow, GridContainer } from '~shared/styled'
import { Journalpost } from '~shared/types/Journalpost'
import styled from 'styled-components'
import { VelgJournalpost } from '~components/person/flyttjournalpost/VelgJournalpost'
import { Sakstilknytning } from '~components/person/flyttjournalpost/Sakstilknytning'

/**
 * Midlertidig løsning inntil Gjenlevendepensjon er helt ute av systemet.
 * Dette sikrer at vi kan overføre eventuelle feilsendte søknader (gjenlevendepensjon som skulle vært OMS) til Gjenny.
 **/
export const FlyttJournalpost = ({}: {}) => {
  const [fileURL, setFileURL] = useState<string>()
  const [bruker, setBruker] = useState<string>('')
  const [valgtJournalpost, setValgtJournalpost] = useState<Journalpost>()

  const [journalposterStatus, hentAlleJournalposter] = useApiCall(hentAlleDokumenterInklPensjon)

  const [dokument, hentDokument, resetHentDokument] = useApiCall(hentDokumentPDF)

  useEffect(() => {
    if (!!valgtJournalpost) {
      hentDokument(
        {
          journalpostId: valgtJournalpost.journalpostId,
          dokumentInfoId: valgtJournalpost.dokumenter[0].dokumentInfoId,
        },
        (bytes) => {
          const blob = new Blob([bytes], { type: 'application/pdf' })

          setFileURL(URL.createObjectURL(blob))
        }
      )
    }
  }, [valgtJournalpost])

  useEffect(() => {
    if (!!fileURL)
      setTimeout(() => {
        URL.revokeObjectURL(fileURL)
      }, 1000)
  }, [fileURL])

  const hentDataForBruker = () => {
    hentAlleJournalposter(bruker, () => {
      resetHentDokument()
      setValgtJournalpost(undefined)
    })
  }

  const oppdaterJournalposter = () => {
    hentAlleJournalposter(bruker, (journalposter) => {
      const oppdatertJournalpost = journalposter.find(
        (journalpost) => journalpost.journalpostId === valgtJournalpost!!.journalpostId
      )
      setValgtJournalpost(oppdatertJournalpost)
    })
  }

  return (
    <GridContainer>
      <Column style={{ minWidth: '50%' }}>
        <Container>
          <Heading size="large" spacing>
            Flytt journalpost
          </Heading>

          <FlexRow align="end" $spacing>
            <TextField
              label="Brukers fødselsnummer"
              description="Må være et gyldig fødselsnummer"
              value={bruker}
              onChange={(e) => setBruker(e.target.value)}
            />

            <Button onClick={hentDataForBruker} disabled={!fnrErGyldig(bruker)}>
              Hent journalposter
            </Button>
          </FlexRow>

          <VelgJournalpost
            valgtJournalpost={valgtJournalpost}
            setValgtJournalpost={setValgtJournalpost}
            journalposterStatus={journalposterStatus}
          />

          <br />
          <br />

          {!!valgtJournalpost && (
            <Sakstilknytning
              bruker={bruker}
              valgtJournalpost={valgtJournalpost}
              oppdaterJournalposter={oppdaterJournalposter}
            />
          )}
        </Container>
      </Column>

      <Column>
        {!!valgtJournalpost && (
          <>
            <Heading size="medium" spacing>
              Journalpost ({valgtJournalpost.journalpostId})<Detail>{valgtJournalpost.tittel}</Detail>
            </Heading>

            {mapApiResult(
              dokument,
              <Spinner label="Klargjør forhåndsvisning av PDF" visible />,
              () => (
                <ApiErrorAlert>Feil ved henting av PDF</ApiErrorAlert>
              ),
              () => (!!fileURL ? <PdfViewer src={fileURL} /> : <></>)
            )}
          </>
        )}
      </Column>
    </GridContainer>
  )
}

const PdfViewer = styled.embed`
  min-width: 680px;
  width: 100%;
  min-height: 600px;
  height: 100%;
`
