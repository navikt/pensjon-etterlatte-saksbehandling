import { Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { compareDesc, lastDayOfMonth } from 'date-fns'
import { formaterDato, formaterStringDato, NOK } from '~utils/formattering'
import { Beregning } from '~shared/types/Beregning'
import { ToolTip } from '~components/behandling/felles/ToolTip'
import { ProrataBroek } from '~components/behandling/beregne/ProrataBroek'

interface Props {
  beregning: Beregning
}

export const OmstillingsstoenadSammendrag = ({ beregning }: Props) => {
  const beregningsperioder = [...beregning.beregningsperioder].sort((a, b) =>
    compareDesc(new Date(a.datoFOM), new Date(b.datoFOM))
  )

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
            <Table.HeaderCell>Prorata Brøk</Table.HeaderCell>
            <Table.HeaderCell>Grunnbeløp</Table.HeaderCell>
            <Table.HeaderCell>Brutto stønad før avkorting</Table.HeaderCell>
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
              <Table.DataCell>
                {beregningsperiode.broek && beregningsperiode.beregningsMetode === 'PRORATA' && (
                  <ProrataBroek broek={beregningsperiode.broek} />
                )}
              </Table.DataCell>
              <Table.DataCell>{NOK(beregningsperiode.grunnbelop)}</Table.DataCell>
              <Table.DataCell>
                {NOK(beregningsperiode.utbetaltBeloep)}{' '}
                <ToolTip title="Få mer informasjon om beregningsgrunnlaget">
                  <strong>Folketrygdloven § 17-6</strong> Full årlig omstillingsstønad utgjør 2,25 ganger grunnbeløpet
                  (G), forutsatt at den avdøde hadde 40 års (full) trygdetid (folketrygdloven §§ 3-5 og 3-7). Dersom
                  trygdetiden er kortere, reduseres omstillingsstønaden forholdsmessig. Eks. på månedlig utbetaling ved
                  30 års trygdetid: 2,25 G * 30/40 /12 mnd.
                </ToolTip>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </TableWrapper>
  )
}

export const TableWrapper = styled.div<{
  marginBottom?: string
}>`
  display: flex;
  flex-wrap: wrap;
  max-width: 1000px;
  margin-bottom: ${(props) => props.marginBottom ?? '5em'};

  .table {
    max-width: 1000px;

    .tableCell {
      max-width: 100px;
    }
  }
`
