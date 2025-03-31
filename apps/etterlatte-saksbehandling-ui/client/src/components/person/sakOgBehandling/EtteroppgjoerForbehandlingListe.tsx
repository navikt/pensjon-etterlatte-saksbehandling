import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Alert, Link, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { MapApiResult } from '~shared/components/MapApiResult'
import { hentEtteroppgjoerForbehandlinger } from '~shared/api/etteroppgjoer'
import { EtteroppgjoerBehandling } from '~shared/types/Etteroppgjoer'

export function lenkeTilForbehandlingMedId(id: string): string {
  return `/etteroppgjoer/${id}/`
}

function EtteroppgjoerForbehandlingTabell(props: { forbehandlinger: Array<EtteroppgjoerBehandling> }) {
  const { forbehandlinger } = props

  if (!forbehandlinger?.length) {
    return (
      <Alert variant="info" inline>
        Ingen forbehandlinger på sak
      </Alert>
    )
  }

  return (
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
        {forbehandlinger.map((forbehandling) => (
          <Table.Row key={forbehandling.id} shadeOnHover={false}>
            <Table.DataCell>{formaterDato(forbehandling.opprettet)}</Table.DataCell>
            {/* TODO riktig visning av statuser når de er klare */}
            <Table.DataCell>{forbehandling.status}</Table.DataCell>
            <Table.DataCell>{forbehandling.aar}</Table.DataCell>
            <Table.DataCell>
              {forbehandling.innvilgetPeriode.fom} - {forbehandling.innvilgetPeriode.tom}
            </Table.DataCell>
            <Table.DataCell>
              <Link href={lenkeTilForbehandlingMedId(forbehandling.id)}>Gå til behandling</Link>
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  )
}

export function EtteroppgjoerForbehandlingListe(props: { sakId: number }) {
  const { sakId } = props
  const [etteroppgjoerForbehandlinger, hentForbehandlinger] = useApiCall(hentEtteroppgjoerForbehandlinger)

  useEffect(() => {
    void hentForbehandlinger(sakId)
  }, [sakId])

  return (
    <MapApiResult
      result={etteroppgjoerForbehandlinger}
      mapInitialOrPending={<Spinner label="Henter forbehandlinger til saken" />}
      mapError={() => <ApiErrorAlert>Kunne ikke hente forbehandlinger</ApiErrorAlert>}
      mapSuccess={(forbehandlinger) => <EtteroppgjoerForbehandlingTabell forbehandlinger={forbehandlinger} />}
    />
  )
}
