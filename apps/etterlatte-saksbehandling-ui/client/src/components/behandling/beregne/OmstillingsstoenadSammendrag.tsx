import { Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { lastDayOfMonth } from 'date-fns'
import { formaterDato, formaterStringDato } from '~utils/formattering'
import { Beregning } from '~shared/types/Beregning'
import { OmstillingsstoenadToolTip } from '~components/behandling/beregne/OmstillingsstoenadToolTip'
import { ManglerRegelspesifikasjon } from '~components/behandling/felles/ManglerRegelspesifikasjon'

interface Props {
  beregning: Beregning
}

export const OmstillingsstoenadSammendrag = ({ beregning }: Props) => {
  const beregningsperioder = beregning.beregningsperioder

  return (
    <TableWrapper>
      <Heading spacing size="small" level="2">
        Beregning før avkorting
      </Heading>
      <Table className="table" zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Periode</Table.HeaderCell>
            <Table.HeaderCell>Ytelse</Table.HeaderCell>
            <Table.HeaderCell>Trygdetid</Table.HeaderCell>
            <Table.HeaderCell>Grunnbeløp</Table.HeaderCell>
            <Table.HeaderCell>Bruttoytelse</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {beregningsperioder?.map((beregningsperiode, key) => (
            <Table.Row key={key} shadeOnHover={false}>
              <Table.DataCell>
                {`${formaterStringDato(beregningsperiode.datoFOM)} - ${
                  beregningsperiode.datoTOM ? formaterDato(lastDayOfMonth(new Date(beregningsperiode.datoTOM))) : ''
                }`}
              </Table.DataCell>
              <Table.DataCell>Omstillingsstønad</Table.DataCell>
              <Table.DataCell>{beregningsperiode.trygdetid} år</Table.DataCell>
              <Table.DataCell>{beregningsperiode.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>
                <ManglerRegelspesifikasjon>
                  {beregningsperiode.utbetaltBeloep} kr <OmstillingsstoenadToolTip />
                </ManglerRegelspesifikasjon>
              </Table.DataCell>
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
  margin-bottom: 5em;
  .table {
    max-width: 1000px;

    .tableCell {
      max-width: 100px;
    }
  }
`
