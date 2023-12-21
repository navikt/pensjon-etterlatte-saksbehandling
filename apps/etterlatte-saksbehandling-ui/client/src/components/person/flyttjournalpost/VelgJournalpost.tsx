import { mapAllApiResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Alert, Button, Heading, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import React from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import styled from 'styled-components'

interface Props {
  valgtJournalpost?: Journalpost
  setValgtJournalpost: (journalpost: Journalpost) => void
  journalposterStatus: Result<Journalpost[]>
}

const JournalpostDetaljer = ({ journalpost }: { journalpost: Journalpost }) => (
  <>
    <InfoPanel>
      <Heading size="small">Sak</Heading>

      <InfoWrapper>
        <Info label="SakID" tekst={journalpost.sak?.fagsakId || '-'} />
        <Info label="Sakstype" tekst={journalpost.sak?.sakstype || '-'} />
        <Info label="Fagsystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
        <Info label="Tema" tekst={journalpost.sak?.tema || '-'} />
      </InfoWrapper>
    </InfoPanel>

    <InfoPanel>
      <Heading size="small">Dokumenter</Heading>

      {journalpost.dokumenter.map((dok, i) => (
        <Info label={`Dokument (${dok.dokumentInfoId})`} tekst={dok.tittel} key={i} />
      ))}
    </InfoPanel>
  </>
)

export const VelgJournalpost = ({ valgtJournalpost, setValgtJournalpost, journalposterStatus }: Props) => {
  return mapAllApiResult(
    journalposterStatus,
    <Spinner visible label="Henter journalposter for bruker" />,
    null,
    () => <ApiErrorAlert>Feil oppsto ved henting av journalposter</ApiErrorAlert>,
    (journalposter) => (
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>{/* expand */}</Table.HeaderCell>
            <Table.HeaderCell>ID</Table.HeaderCell>
            <Table.HeaderCell>Opprettet</Table.HeaderCell>
            <Table.HeaderCell>Tema</Table.HeaderCell>
            <Table.HeaderCell>Status</Table.HeaderCell>
            <Table.HeaderCell>{/* knapper */}</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {journalposter.length ? (
            journalposter.map((journalpost) => (
              <Table.ExpandableRow
                key={journalpost.journalpostId}
                content={<JournalpostDetaljer journalpost={journalpost} />}
              >
                <Table.DataCell>{journalpost.journalpostId}</Table.DataCell>
                <Table.DataCell>
                  {journalpost.datoOpprettet ? formaterStringDato(journalpost.datoOpprettet) : '-'}
                </Table.DataCell>
                <Table.DataCell>{journalpost.tema}</Table.DataCell>
                <Table.DataCell>{journalpost.journalstatus}</Table.DataCell>
                <Table.DataCell>
                  {journalpost.journalpostId === valgtJournalpost?.journalpostId ? (
                    <Alert size="small" variant="success" style={{ width: 'fit-content' }}>
                      Valgt
                    </Alert>
                  ) : (
                    <Button size="small" onClick={() => setValgtJournalpost(journalpost)}>
                      Velg
                    </Button>
                  )}
                </Table.DataCell>
              </Table.ExpandableRow>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={100}>Ingen journalposter funnet på bruker</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    )
  )
}

const InfoPanel = styled.div`
  padding: 0.5rem;
  border: 1px solid lightgray;
  border-radius: 0.2rem;
  background: #fafafa;

  :not(:last-child) {
    margin-bottom: 1rem;
  }
`
