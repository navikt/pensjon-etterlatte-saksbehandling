import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import React from 'react'
import { Table } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'

export function TilbakekrevingVurderingOppsummering({ behandling }: { behandling: TilbakekrevingBehandling }) {
  function sum(beloeper: (number | null)[]) {
    if (beloeper.length === 0) return 0
    return beloeper.map((beloep) => (beloep ? beloep : 0)).reduce((sum, current) => sum + current)
  }

  const tilbakekreving = behandling.tilbakekreving
  const sumFeilutbetaling = sum(tilbakekreving.perioder.map((it) => it.ytelse.beregnetFeilutbetaling))
  const sumNettoTilbakekreving = sum(tilbakekreving.perioder.map((it) => it.ytelse.nettoTilbakekreving))
  const sumBruttoTilbakekreving = sum(tilbakekreving.perioder.map((it) => it.ytelse.bruttoTilbakekreving))
  const sumRenter = sum(tilbakekreving.perioder.map((it) => it.ytelse.rentetillegg))
  const sumSkatt = sum(tilbakekreving.perioder.map((it) => it.ytelse.skatt))
  const oppsummertInnkreving = sumNettoTilbakekreving + sumRenter

  return (
    <Table className="table" zebraStripes style={{ width: '50rem' }}>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell></Table.HeaderCell>
          <Table.HeaderCell>Beregnet feilutbetaling</Table.HeaderCell>
          <Table.HeaderCell></Table.HeaderCell>
          <Table.HeaderCell>Tilbakekreving</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row key="Beloep">
          <Table.DataCell></Table.DataCell>
          <Table.DataCell>{NOK(sumFeilutbetaling)}</Table.DataCell>
          <Table.DataCell></Table.DataCell>
          <Table.DataCell></Table.DataCell>
        </Table.Row>
        <Table.Row key="Brutto">
          <Table.DataCell>Brutto tilbakekreving</Table.DataCell>
          <Table.DataCell></Table.DataCell>
          <Table.DataCell></Table.DataCell>
          <Table.DataCell>{NOK(sumBruttoTilbakekreving)}</Table.DataCell>
        </Table.Row>
        <Table.Row key="Skatt">
          <Table.DataCell>Fradrag skatt</Table.DataCell>
          <Table.DataCell></Table.DataCell>
          <Table.DataCell>-</Table.DataCell>
          <Table.DataCell>{NOK(sumSkatt)}</Table.DataCell>
        </Table.Row>
        <Table.Row key="Netto">
          <Table.DataCell>Netto tilbakekreving</Table.DataCell>
          <Table.DataCell></Table.DataCell>
          <Table.DataCell>=</Table.DataCell>
          <Table.DataCell>{NOK(sumNettoTilbakekreving)}</Table.DataCell>
        </Table.Row>
        <Table.Row key="Renter">
          <Table.DataCell>Rentetillegg</Table.DataCell>
          <Table.DataCell></Table.DataCell>
          <Table.DataCell>+</Table.DataCell>
          <Table.DataCell>{NOK(sumRenter)}</Table.DataCell>
        </Table.Row>
        <Table.Row key="SumInnkreving">
          <Table.HeaderCell>Sum til innkreving</Table.HeaderCell>
          <Table.HeaderCell></Table.HeaderCell>
          <Table.HeaderCell>=</Table.HeaderCell>
          <Table.HeaderCell>{NOK(oppsummertInnkreving)}</Table.HeaderCell>
        </Table.Row>
      </Table.Body>
    </Table>
  )
}
