import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Alert, Button, Link, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { hentEtteroppgjoerForbehandlinger } from '~shared/api/etteroppgjoer'
import {
  EtteroppgjoerBehandling,
  EtteroppgjoerBehandlingStatus,
  teksterEtteroppgjoerBehandlingStatus,
} from '~shared/types/Etteroppgjoer'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { opprettRevurderingEtteroppgjoer as opprettRevurderingApi } from '~shared/api/revurdering'

function lenkeTilForbehandlingMedId(id: string): string {
  return `/etteroppgjoer/${id}/`
}

function EtteroppgjoerForbehandlingTabell({
  sakId,
  forbehandlinger,
}: {
  sakId: number
  forbehandlinger: Array<EtteroppgjoerBehandling>
}) {
  const [opprettRevurderingResult, opprettRevurderingRequest] = useApiCall(opprettRevurderingApi)

  if (!forbehandlinger?.length) {
    return (
      <Alert variant="info" inline>
        Ingen forbehandlinger på sak
      </Alert>
    )
  }

  // TODO disse revurderingene skal antageligvis ikke opprettes på denne måten, men vi trenger en måte å komme fra forbehandling
  const opprettRevurderingEtteroppgjoer = (forbehandlingId: string) => {
    opprettRevurderingRequest({ sakId: sakId, forbehandlingId: forbehandlingId }, () => {
      window.location.reload()
    })
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
          <Table.HeaderCell>Revurdering</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {forbehandlinger.map((forbehandling) => (
          <Table.Row key={forbehandling.id} shadeOnHover={false}>
            <Table.DataCell>{formaterDato(forbehandling.opprettet)}</Table.DataCell>
            <Table.DataCell>{teksterEtteroppgjoerBehandlingStatus[forbehandling.status]}</Table.DataCell>
            <Table.DataCell>{forbehandling.aar}</Table.DataCell>
            <Table.DataCell>
              {forbehandling.innvilgetPeriode.fom} - {forbehandling.innvilgetPeriode.tom}
            </Table.DataCell>
            <Table.DataCell>
              <Link href={lenkeTilForbehandlingMedId(forbehandling.id)}>Gå til behandling</Link>
            </Table.DataCell>
            <Table.DataCell>
              {forbehandling.status == EtteroppgjoerBehandlingStatus.SVAR_MOTTATT ||
                (forbehandling.status == EtteroppgjoerBehandlingStatus.INGEN_SVAR_INNEN_TIDSFRIST && (
                  <Button
                    loading={isPending(opprettRevurderingResult)}
                    size="small"
                    onClick={() => opprettRevurderingEtteroppgjoer(forbehandling.id)}
                  >
                    Opprett revurdering
                  </Button>
                ))}
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  )
}

export function EtteroppgjoerForbehandlingListe(props: { sakId: number }) {
  const { sakId } = props
  const [hentEtteroppgjoerForbehandlingerResult, hentEtteroppgjoerForbehandlingerFetch] = useApiCall(
    hentEtteroppgjoerForbehandlinger
  )

  useEffect(() => {
    void hentEtteroppgjoerForbehandlingerFetch(sakId)
  }, [sakId])

  return mapResult(hentEtteroppgjoerForbehandlingerResult, {
    pending: <Spinner label="Henter forbehandlinger til saken"></Spinner>,
    success: (forbehandlinger: EtteroppgjoerBehandling[]) => {
      return <EtteroppgjoerForbehandlingTabell sakId={sakId} forbehandlinger={forbehandlinger} />
    },
    error: () => {
      return <ApiErrorAlert>Kunne ikke hente forbehandlinger</ApiErrorAlert>
    },
  })
}
