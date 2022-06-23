import { Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import format from 'date-fns/format'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'

export const Sammendrag = () => {
  const beregningsperioder = useContext(AppContext).state.behandlingReducer.beregning?.beregningsperioder

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
            <Table.HeaderCell>Månedelig utbetaling før skatt</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {beregningsperioder?.map((beregning, key) => (
            <Table.Row key={key}>
              <Table.DataCell>
                {format(new Date(beregning.datoFOM), 'dd.MM.yyyy')} -{' '}
                {beregning.datoTOM && format(new Date(beregning.datoTOM), 'dd.MM.yyyy')}
              </Table.DataCell>
              <Table.DataCell>{beregning.type == 'GP' ? 'Grunnpensjon' : beregning.type}</Table.DataCell>
              <Table.DataCell>Mangler</Table.DataCell>
              <Table.DataCell>{beregning.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>1 barn (hardkodet)</Table.DataCell>
              <Table.DataCell>{beregning.grunnbelopMnd} kr</Table.DataCell>
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
