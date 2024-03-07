import { Detail, Heading, Table } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { Tema } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapApiResult, Result } from '~shared/api/apiUtils'
import { Container } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { useApiCall } from '~shared/hooks/useApiCall'
import { DokumentFilter } from '~components/person/dokumenter/DokumentFilter'
import { SakMedBehandlinger } from '~components/person/typer'
import { DokumentRad } from './DokumentRad'

export const Dokumentliste = ({ fnr, sakStatus }: { fnr: string; sakStatus: Result<SakMedBehandlinger> }) => {
  const [filter, setFilter] = useState<DokumentFilter>({
    tema: [Tema.EYO, Tema.EYB],
    type: [],
    status: [],
  })
  const [dokumenter, hentDokumenterForBruker] = useApiCall(hentDokumenter)

  useEffect(
    () => void hentDokumenterForBruker({ fnr, temaer: filter.tema, typer: filter.type, statuser: filter.status }),
    [fnr, filter]
  )

  return (
    <Container>
      <Heading size="medium" spacing>
        Dokumenter
      </Heading>

      <DokumentFilter filter={filter} setFilter={setFilter} />

      <Table zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell>ID</Table.HeaderCell>
            <Table.HeaderCell>Tittel</Table.HeaderCell>
            <Table.HeaderCell>Avsender/Mottaker</Table.HeaderCell>
            <Table.HeaderCell>Dato</Table.HeaderCell>
            <Table.HeaderCell>Sak</Table.HeaderCell>
            <Table.HeaderCell>Status</Table.HeaderCell>
            <Table.HeaderCell>Tema</Table.HeaderCell>
            <Table.HeaderCell>Type</Table.HeaderCell>
            <Table.HeaderCell />
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {mapApiResult(
            dokumenter,
            <Table.Row>
              <Table.DataCell colSpan={100}>
                <Spinner margin="0" visible label="Henter dokumenter" />
              </Table.DataCell>
            </Table.Row>,
            () => (
              <Table.Row>
                <Table.DataCell colSpan={100}>
                  <ApiErrorAlert>Det har oppstått en feil ved henting av dokumenter</ApiErrorAlert>
                </Table.DataCell>
              </Table.Row>
            ),
            (dokumentListe) =>
              !dokumentListe.length ? (
                <Table.Row shadeOnHover={false}>
                  <Table.DataCell colSpan={100}>
                    <Detail>
                      <i>Ingen dokumenter funnet</i>
                    </Detail>
                  </Table.DataCell>
                </Table.Row>
              ) : (
                <>
                  {dokumentListe.map((dokument) => (
                    <DokumentRad key={dokument.journalpostId} dokument={dokument} sakStatus={sakStatus} />
                  ))}
                </>
              )
          )}
        </Table.Body>
      </Table>
    </Container>
  )
}
