import { IAvkortetYtelse } from '~shared/types/IAvkorting'
import { Table } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'
import { ManglerRegelspesifikasjon } from '~components/behandling/felles/ManglerRegelspesifikasjon'
import { formaterStringDato } from '~utils/formattering'

export const YtelseEtterAvkorting = (props: { ytelser?: IAvkortetYtelse[] }) => {
  const ytelser = props.ytelser

  return (
    <>
      {ytelser && ytelser.length > 0 && (
        <TableWrapper>
          <Table className="table" zebraStripes>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Månedlig utbetaling</Table.HeaderCell>
                <Table.HeaderCell>Utbetalingsdato</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {ytelser?.map((ytelse, key) => (
                <Table.Row key={key} shadeOnHover={false}>
                  <Table.DataCell>
                    {formaterStringDato(ytelse.fom)} - {ytelse.tom ? formaterStringDato(ytelse.tom) : ''}
                  </Table.DataCell>
                  <Table.DataCell>
                    <ManglerRegelspesifikasjon>{ytelse.ytelseEtterAvkorting}</ManglerRegelspesifikasjon>
                  </Table.DataCell>
                  <Table.DataCell>{ytelse.fom}</Table.DataCell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table>
        </TableWrapper>
      )}
    </>
  )
}

const TableWrapper = styled.div`
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
