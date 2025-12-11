import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Alert, Link, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'

import {
  teksterTilbakekrevingStatus,
  TilbakekrevingBehandling,
  TilbakekrevingPeriode,
} from '~shared/types/Tilbakekreving'
import { hentTilbakekrevingerISak } from '~shared/api/tilbakekreving'
import { VedtakKolonner } from '~components/person/VedtakKoloner'
import { MapApiResult } from '~shared/components/MapApiResult'
import { lastDayOfMonth } from 'date-fns'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { OmgjoerTilbakekreving } from '~components/person/sakOgBehandling/OmgjoerTilbakekreving'

function lenkeTilTilbakekrevingMedId(id: string): string {
  return `/tilbakekreving/${id}/`
}

function formaterPeriode(perioder: TilbakekrevingPeriode[]): string {
  if (perioder.length === 1) {
    return formaterDato(perioder[0].maaned)
  }
  return formaterDato(perioder[0].maaned) + ' - ' + formaterDato(lastDayOfMonth(perioder[perioder.length - 1].maaned))
}

function TilbakekrevingTabell(props: { tilbakekrevinger: Array<TilbakekrevingBehandling> }) {
  const { tilbakekrevinger } = props
  const omgjoerTilbakekrevingEnabled = useFeaturetoggle(FeatureToggle.omgjoer_tilbakekreving)

  if (!tilbakekrevinger?.length) {
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
          {omgjoerTilbakekrevingEnabled && <Table.HeaderCell></Table.HeaderCell>}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tilbakekrevinger.map((tilbakekreving) => (
          <Table.Row key={tilbakekreving.id} shadeOnHover={false}>
            <Table.DataCell>{formaterDato(tilbakekreving.opprettet)}</Table.DataCell>
            <Table.DataCell>{teksterTilbakekrevingStatus[tilbakekreving.status]}</Table.DataCell>
            <Table.DataCell>{formaterPeriode(tilbakekreving.tilbakekreving.perioder)}</Table.DataCell>
            <VedtakKolonner behandlingId={tilbakekreving.id} />
            <Table.DataCell>
              <Link href={lenkeTilTilbakekrevingMedId(tilbakekreving.id)}>Se behandling</Link>
            </Table.DataCell>
            {omgjoerTilbakekrevingEnabled && (
              <Table.DataCell>
                <OmgjoerTilbakekreving tilbakekreving={tilbakekreving} />
              </Table.DataCell>
            )}
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
      mapInitialOrPending={<Spinner label="Henter tilbakekrevinger til saken" />}
      mapError={() => <ApiErrorAlert>Kunne ikke hente tilbakekrevinger</ApiErrorAlert>}
      mapSuccess={(tilbakekrevinger) => <TilbakekrevingTabell tilbakekrevinger={tilbakekrevinger} />}
    />
  )
}
