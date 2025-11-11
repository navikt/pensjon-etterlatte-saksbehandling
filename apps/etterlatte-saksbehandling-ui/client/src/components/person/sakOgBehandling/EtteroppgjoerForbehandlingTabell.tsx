import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Alert, Link, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { hentEtteroppgjoerForbehandlinger } from '~shared/api/etteroppgjoer'
import {
  EtteroppgjoerForbehandling,
  teksterEtteroppgjoerBehandlingStatus,
} from '~shared/types/EtteroppgjoerForbehandling'
import { mapResult } from '~shared/api/apiUtils'

export function EtteroppgjoerForbehandlingTabell({ sakId }: { sakId: number }) {
  const [hentEtteroppgjoerForbehandlingerResult, hentEtteroppgjoerForbehandlingerFetch] = useApiCall(
    hentEtteroppgjoerForbehandlinger
  )

  // TODO toggle visning av kopier eller ikke?
  const relevanteForbehandlinger = (forbehandlinger: Array<EtteroppgjoerForbehandling>) =>
    forbehandlinger.filter((forbehandling) => forbehandling.kopiertFra == null)

  useEffect(() => {
    hentEtteroppgjoerForbehandlingerFetch(sakId)
  }, [sakId])

  return mapResult(hentEtteroppgjoerForbehandlingerResult, {
    pending: <Spinner label="Henter forbehandlinger til saken" />,
    error: <ApiErrorAlert>Kunne ikke hente forbehandlinger</ApiErrorAlert>,
    success: (forbehandlinger) => {
      return !forbehandlinger?.length ? (
        <Alert variant="info" inline>
          Ingen forbehandlinger på sak
        </Alert>
      ) : (
        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Reg. dato</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>År</Table.HeaderCell>
              <Table.HeaderCell>Periode</Table.HeaderCell>
              <Table.HeaderCell>Handling</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {relevanteForbehandlinger(forbehandlinger).map((forbehandling) => (
              <Table.Row key={forbehandling.id} shadeOnHover={false}>
                <Table.DataCell>{formaterDato(forbehandling.opprettet)}</Table.DataCell>
                <Table.DataCell>{teksterEtteroppgjoerBehandlingStatus[forbehandling.status]}</Table.DataCell>
                <Table.DataCell>{forbehandling.aar}</Table.DataCell>
                <Table.DataCell>
                  {forbehandling.innvilgetPeriode.fom} - {forbehandling.innvilgetPeriode.tom}
                </Table.DataCell>
                <Table.DataCell>
                  <Link href={`/etteroppgjoer/${forbehandling.id}/`}>Se behandling</Link>
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )
    },
  })
}
