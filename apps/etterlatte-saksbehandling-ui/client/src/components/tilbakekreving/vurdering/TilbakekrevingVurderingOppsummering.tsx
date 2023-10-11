import { Tilbakekreving } from '~shared/types/Tilbakekreving'
import { InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import React from 'react'
import { Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { NOK } from '~utils/formattering'

export function TilbakekrevingVurderingOppsummering({ tilbakekreving }: { tilbakekreving: Tilbakekreving }) {
  function sum(beloper: (number | null)[]) {
    const test = beloper.flatMap((it) => (it ? [it] : []))
    return test.length === 0 ? 0 : test.reduce((sum, current) => (sum += current))
  }

  const sumFeilutbetaling = sum(tilbakekreving.perioder.map((it) => it.ytelse.beregnetFeilutbetaling))
  const sumTilbakekreving = sum(tilbakekreving.perioder.map((it) => it.ytelse.bruttoTilbakekreving))
  const sumRenter = sum(tilbakekreving.perioder.map((it) => it.ytelse.rentetillegg))
  const sumSkatt = sum(tilbakekreving.perioder.map((it) => it.ytelse.skatt))
  const oppsummertInnkreving = sumTilbakekreving + sumRenter - sumSkatt
  return (
    <InnholdPadding>
      <Heading level="3" size="medium">
        Oppsummering
      </Heading>
      <TableWrapper>
        <Table className="table" zebraStripes>
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
              <Table.HeaderCell>Beløp</Table.HeaderCell>
              <Table.DataCell>{NOK(sumFeilutbetaling)}</Table.DataCell>
              <Table.DataCell></Table.DataCell>
              <Table.DataCell></Table.DataCell>
            </Table.Row>
            <Table.Row key="tilbakekreving">
              <Table.HeaderCell>Brutto tilbakekreving</Table.HeaderCell>
              <Table.DataCell></Table.DataCell>
              <Table.DataCell></Table.DataCell>
              <Table.DataCell>{NOK(sumTilbakekreving)}</Table.DataCell>
            </Table.Row>
            <Table.Row key="Renter">
              <Table.HeaderCell>Renter</Table.HeaderCell>
              <Table.DataCell></Table.DataCell>
              <Table.DataCell>+</Table.DataCell>
              <Table.DataCell>{NOK(sumRenter)}</Table.DataCell>
            </Table.Row>
            <Table.Row key="Skatt">
              <Table.HeaderCell>Skatt</Table.HeaderCell>
              <Table.DataCell></Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>{NOK(sumSkatt)}</Table.DataCell>
            </Table.Row>
            <Table.Row key="SumInnkreving">
              <Table.HeaderCell>Sum til innkreving</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
              <Table.HeaderCell>=</Table.HeaderCell>
              <Table.HeaderCell>{NOK(oppsummertInnkreving)}</Table.HeaderCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </TableWrapper>
    </InnholdPadding>
  )
}

const TableWrapper = styled.div`
  width: 35%;
`
