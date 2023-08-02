import { IAvkortetYtelse } from '~shared/types/IAvkorting'
import { Heading, Table } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'
import { ManglerRegelspesifikasjon } from '~components/behandling/felles/ManglerRegelspesifikasjon'
import { formaterStringDato, NOK } from '~utils/formattering'
import { YtelseEtterAvkortingDetaljer } from '~components/behandling/avkorting/YtelseEtterAvkortingDetaljer'

export const YtelseEtterAvkorting = (props: { ytelser: IAvkortetYtelse[] }) => {
  const ytelser = [...props.ytelser]
  ytelser.sort((a, b) => new Date(b.fom).getTime() - new Date(a.fom).getTime())

  return (
    <>
      {ytelser.length > 0 && (
        <TableWrapper>
          <Heading spacing size="small" level="2">
            Beregning etter avkorting
          </Heading>
          <Table className="table" zebraStripes>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Avkorting</Table.HeaderCell>
                <Table.HeaderCell>Restanse</Table.HeaderCell>
                <Table.HeaderCell>Brutto stønad etter avkorting</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {ytelser.map((ytelse, key) => (
                <Table.ExpandableRow
                  key={key}
                  shadeOnHover={false}
                  content={<YtelseEtterAvkortingDetaljer ytelse={ytelse} />}
                >
                  <Table.DataCell>
                    {formaterStringDato(ytelse.fom)} - {ytelse.tom ? formaterStringDato(ytelse.tom) : ''}
                  </Table.DataCell>
                  <Table.DataCell>{NOK(ytelse.avkortingsbeloep)}</Table.DataCell>
                  {ytelse.restanse < 0 ? (
                    <Table.DataCell>+ {NOK(ytelse.restanse * -1)}</Table.DataCell>
                  ) : (
                    <Table.DataCell>- {NOK(ytelse.restanse)}</Table.DataCell>
                  )}
                  <Table.DataCell>
                    <ManglerRegelspesifikasjon>{NOK(ytelse.ytelseEtterAvkorting)}</ManglerRegelspesifikasjon>
                  </Table.DataCell>
                </Table.ExpandableRow>
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
