import { Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { beregningsperioder } from './mockdata'
import format from 'date-fns/format'

export const Sammendrag = () => {
  return (
    <TableWrapper>
      <Heading spacing size="small" level="5">
        Beregningssammendrag
      </Heading>
      <Table className="table">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Periode</Table.HeaderCell>
            <Table.HeaderCell>Ytelse</Table.HeaderCell>
            <Table.HeaderCell>Trygdetid</Table.HeaderCell>
            <Table.HeaderCell>Grunnbeløp</Table.HeaderCell>
            <Table.HeaderCell>Beregning gjelder</Table.HeaderCell>
            <Table.HeaderCell>Månedelig utbetaling</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {beregningsperioder.map((beregning, key) => (
            <Table.Row key={key}>
              <Table.DataCell>
                {format(new Date(beregning.gyldigFraOgMed), 'dd.MM.yyyy')} -{' '}
                {beregning.gyldigTilOgMed && format(new Date(beregning.gyldigTilOgMed), 'dd.MM.yyyy')}
              </Table.DataCell>
              <Table.DataCell>{beregning.ytelse}</Table.DataCell>
              <Table.DataCell>{beregning.trygdetid} år</Table.DataCell>
              <Table.DataCell>{beregning.grunnbeløp}</Table.DataCell>
              <Table.DataCell>{beregning.antallBarnIKull} barn</Table.DataCell>
              <Table.DataCell>{beregning.maantligBetaling}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </TableWrapper>
  )
}

export const TableWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  max-width: 1000px;
  .table {
    max-width: 1000px;

    .tableCell {
      max-width: 100px;
    }
  }
`
