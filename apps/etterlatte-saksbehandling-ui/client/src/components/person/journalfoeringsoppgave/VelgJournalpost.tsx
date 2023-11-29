import { isFailure, isPending, isSuccess, mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentDokumenter, hentDokumentPDF, hentJournalpost } from '~shared/api/dokument'
import React, { useEffect, useState } from 'react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { Button, Detail, Heading, Link, Panel, Table } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { formaterStringDato } from '~utils/formattering'
import { useAppDispatch } from '~store/Store'
import { settJournalpost } from '~store/reducers/JournalfoeringOppgaveReducer'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import DokumentModal from '../dokumenter/dokumentModal'
import { Journalpost } from '~shared/types/Journalpost'
import { FlexRow } from '~shared/styled'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

export default function VelgJournalpost({ journalpostId }: { journalpostId: string | null }) {
  const { bruker, journalpost } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()

  const [journalposter, apiHentJournalposter] = useApiCall(hentDokumenter)
  const [journalpostStatus, apiHentJournalpost] = useApiCall(hentJournalpost)
  const [dokument, hentDokument] = useApiCall(hentDokumentPDF)
  const [fileURL, setFileURL] = useState<string>()

  const velgJournalpost = (journalpost: Journalpost) => {
    dispatch(settJournalpost(journalpost))
  }

  useEffect(() => {
    if (fnrHarGyldigFormat(bruker) && !journalpost) {
      if (journalpostId) {
        apiHentJournalpost(journalpostId, (journalpost) => {
          velgJournalpost(journalpost)
        })
      } else {
        apiHentJournalposter(bruker!!, (journalposter) => {
          if (journalposter.length === 1) {
            velgJournalpost(journalposter[0])
          }
        })
      }
    }
  }, [bruker])

  useEffect(() => {
    if (journalpost && !fileURL) {
      hentDokument(
        {
          journalpostId: journalpost.journalpostId,
          dokumentInfoId: journalpost.dokumenter[0].dokumentInfoId, // TODO: Sikre korrekt index
        },
        (bytes) => {
          const blob = new Blob([bytes], { type: 'application/pdf' })

          setFileURL(URL.createObjectURL(blob))
        }
      )
    }
  }, [journalpost])

  useEffect(() => {
    if (!!fileURL)
      setTimeout(() => {
        URL.revokeObjectURL(fileURL)
      }, 1000)
  }, [fileURL])

  return (
    <>
      {isPending(journalposter) && <Spinner label="Henter journalposter for bruker" visible />}
      {isPending(journalpostStatus) && <Spinner label="Henter journalpost for bruker" visible />}
      {isFailure(journalpostStatus) && (
        <ApiErrorAlert>Feil ved henting av journalpost. Kan ikke fortsette behandlingen.</ApiErrorAlert>
      )}

      {journalpost ? (
        <>
          <Panel>
            <Heading size="medium" spacing>
              Journalpost ({journalpost.journalpostId})<Detail>{journalpost.tittel}</Detail>
            </Heading>
            <InfoWrapper>
              <Info
                label="Avsender/mottaker"
                tekst={
                  <>
                    {journalpost.avsenderMottaker?.navn} (
                    {journalpost.avsenderMottaker?.id?.length === 11 ? (
                      <Link href={`/person/${journalpost.avsenderMottaker?.id}`}>
                        {journalpost.avsenderMottaker?.id}
                      </Link>
                    ) : (
                      journalpost.avsenderMottaker?.id
                    )}
                    )
                  </>
                }
              />
              <Info
                label="Sak"
                tekst={
                  journalpost.sak
                    ? `${journalpost.sak.fagsaksystem}: ${journalpost.sak.fagsakId || '-'}`
                    : 'Ikke tilknyttet sak'
                }
              />
              <Info label="Status" tekst={journalpost.journalstatus} />
              <Info label="Kanal" tekst={journalpost.kanal} />
            </InfoWrapper>
          </Panel>

          <br />

          {mapApiResult(
            dokument,
            <Spinner label="Klargjør forhåndsvisning av PDF" visible />,
            () => (
              <ApiErrorAlert>Feil ved henting av PDF</ApiErrorAlert>
            ),
            () => (!!fileURL ? <PdfViewer src={`${fileURL}#toolbar=0`} /> : <></>)
          )}
        </>
      ) : (
        <>
          {isSuccess(journalposter) && journalposter.data.length > 1 && (
            <Table zebraStripes>
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell>ID</Table.HeaderCell>
                  <Table.HeaderCell>Tittel</Table.HeaderCell>
                  <Table.HeaderCell>Avsender/mottaker</Table.HeaderCell>
                  <Table.HeaderCell>Opprettet</Table.HeaderCell>
                  <Table.HeaderCell>Status</Table.HeaderCell>
                  <Table.HeaderCell>Type</Table.HeaderCell>
                  <Table.HeaderCell></Table.HeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {journalposter.data.map((journalpost) => (
                  <Table.Row key={journalpost.journalpostId} shadeOnHover={false}>
                    <Table.DataCell>{journalpost.journalpostId}</Table.DataCell>
                    <Table.DataCell>{journalpost.tittel}</Table.DataCell>
                    <Table.DataCell>{journalpost.avsenderMottaker.navn || 'Ukjent'}</Table.DataCell>
                    <Table.DataCell>{formaterStringDato(journalpost.datoOpprettet)}</Table.DataCell>
                    <Table.DataCell>{journalpost.journalstatus}</Table.DataCell>
                    <Table.DataCell>{journalpost.journalposttype === 'I' ? 'Inngående' : 'Utgående'}</Table.DataCell>
                    <Table.DataCell>
                      <FlexRow>
                        <DokumentModal
                          tittel={journalpost.tittel}
                          journalpostId={journalpost.journalpostId}
                          dokumentInfoId={journalpost.dokumenter[0].dokumentInfoId}
                        />

                        <Button variant="primary" size="small" onClick={() => velgJournalpost(journalpost)}>
                          Velg
                        </Button>
                      </FlexRow>
                    </Table.DataCell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table>
          )}
        </>
      )}
    </>
  )
}

const PdfViewer = styled.embed`
  min-width: 680px;
  width: 100%;
  min-height: 600px;
  height: 100%;
`
