import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Alert, Link, Table } from '@navikt/ds-react'
import { formaterDato, formaterStringDato } from '~utils/formattering'

import {
  teksterTilbakekrevingStatus,
  TilbakekrevingBehandling,
  TilbakekrevingPeriode,
} from '~shared/types/Tilbakekreving'
import { hentTilbakekrevingerISak } from '~shared/api/tilbakekreving'
import { VedtakKolonner } from '~components/person/VedtakKoloner'
import { MapApiResult } from '~shared/components/MapApiResult'

export function lenkeTilTilbakekrevingMedId(id: string): string {
  return `/tilbakekreving/${id}/`
}

function formaterPeriode(perioder: TilbakekrevingPeriode[]): string {
  if (perioder.length === 1) {
    return formaterDato(perioder[0].maaned)
  }
  return formaterDato(perioder[0].maaned) + ' - ' + formaterDato(perioder[perioder.length - 1].maaned)
}

function TilbakekrevingTabell(props: { tilbakekrevinger: Array<TilbakekrevingBehandling> }) {
  const { tilbakekrevinger } = props

  if (tilbakekrevinger?.length == 0) {
    return (
      <Alert variant="info" inline>
        Ingen tilbakekrevinger på sak
      </Alert>
    )
  }

  return (
    <Table zebraStripes>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Reg. dato</Table.HeaderCell>
          <Table.HeaderCell>Status</Table.HeaderCell>
          <Table.HeaderCell>Vurderingsperiode</Table.HeaderCell>
          <Table.HeaderCell>Vedtaksdato</Table.HeaderCell>
          <Table.HeaderCell>Resultat</Table.HeaderCell>
          <Table.HeaderCell>Handling</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tilbakekrevinger.map((tilbakekreving) => (
          <Table.Row key={tilbakekreving.id} shadeOnHover={false}>
            <Table.DataCell>{formaterStringDato(tilbakekreving.opprettet)}</Table.DataCell>
            <Table.DataCell>{teksterTilbakekrevingStatus[tilbakekreving.status]}</Table.DataCell>
            <Table.DataCell>{formaterPeriode(tilbakekreving.tilbakekreving.perioder)}</Table.DataCell>
            <VedtakKolonner behandlingId={tilbakekreving.id} />
            <Table.DataCell>
              <Link href={lenkeTilTilbakekrevingMedId(tilbakekreving.id)}>Gå til behandling</Link>
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  )
}

export function TilbakekrevingListe(props: { sakId: number }) {
  const { sakId } = props
  const [tilbakekrevinger, hentTilbakekrevinger] = useApiCall(hentTilbakekrevingerISak)

  useEffect(() => {
    void hentTilbakekrevinger(sakId)
  }, [sakId])

  return (
    <MapApiResult
      result={tilbakekrevinger}
      mapInitialOrPending={<Spinner visible label="Henter tilbakekrevinger til saken" />}
      mapError={() => <ApiErrorAlert>Kunne ikke hente tilbakekrevinger</ApiErrorAlert>}
      mapSuccess={(tilbakekrevinger) => <TilbakekrevingTabell tilbakekrevinger={tilbakekrevinger} />}
    />
  )
}
