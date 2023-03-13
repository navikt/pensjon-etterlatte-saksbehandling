import { Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { lastDayOfMonth } from 'date-fns'
import { formaterDato, formaterStringDato } from '~utils/formattering'
import { IPdlPerson } from '~shared/types/Person'
import { Beregning, Beregningstype } from '~shared/types/Beregning'
import { GjelderTooltip } from '~components/behandling/beregne/GjelderToolTip'

interface Props {
  beregning: Beregning
  soeker?: IPdlPerson
  soesken?: IPdlPerson[]
}

export const Sammendrag = ({ beregning, soeker, soesken }: Props) => {
  const beregningsperioder = beregning.beregningsperioder

  return (
    <TableWrapper>
      <Heading spacing size="small" level="2">
        Beregningssammendrag
      </Heading>
      <Table className="table" zebraStripes>
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
          {beregningsperioder?.map((beregningsperiode, key) => (
            <Table.Row key={key} shadeOnHover={false}>
              <Table.DataCell>
                {`${formaterStringDato(beregningsperiode.datoFOM)} - ${
                  beregningsperiode.datoTOM ? formaterDato(lastDayOfMonth(new Date(beregningsperiode.datoTOM))) : ''
                }`}
              </Table.DataCell>
              <Table.DataCell>
                {
                  {
                    [Beregningstype.BP]: 'Barnepensjon',
                    [Beregningstype.OMS]: 'Omstillingsstønad',
                  }[beregning.type]
                }
              </Table.DataCell>
              <Table.DataCell>40 år</Table.DataCell>
              <Table.DataCell>{beregningsperiode.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>
                {beregningsperiode.soeskenFlokk && soeker ? (
                  <GjelderTooltip
                    soesken={beregningsperiode.soeskenFlokk.map((fnr) => {
                      const soeskenMedData = soesken?.find((p) => p.foedselsnummer === fnr)
                      return soeskenMedData!!
                    })}
                    soeker={soeker}
                  />
                ) : (
                  '-'
                )}
              </Table.DataCell>
              <Table.DataCell>{beregningsperiode.utbetaltBeloep} kr</Table.DataCell>
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
