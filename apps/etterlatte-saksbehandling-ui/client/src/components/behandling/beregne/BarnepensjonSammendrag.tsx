import { Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { lastDayOfMonth } from 'date-fns'
import { formaterDato, formaterStringDato } from '~utils/formattering'
import { Beregning } from '~shared/types/Beregning'
import { BarnepensjonToolTip } from '~components/behandling/beregne/BarnepensjonToolTip'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Barnepensjonberegningssammendrag } from '~components/behandling/beregne/Barnepensjonberegningssammendrag'

interface Props {
  behandling: IDetaljertBehandling
  beregning: Beregning
}

export const BarnepensjonSammendrag = ({ behandling, beregning }: Props) => {
  const beregningsperioder = beregning.beregningsperioder
  const soeker = behandling.søker
  const soesken = behandling.familieforhold?.avdoede.opplysning.avdoedesBarn?.filter(
    (barn) => barn.foedselsnummer !== soeker?.foedselsnummer
  )
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
            <Table.ExpandableRow key={key} shadeOnHover={false} content={<Barnepensjonberegningssammendrag />}>
              <Table.DataCell>
                {`${formaterStringDato(beregningsperiode.datoFOM)} - ${
                  beregningsperiode.datoTOM ? formaterDato(lastDayOfMonth(new Date(beregningsperiode.datoTOM))) : ''
                }`}
              </Table.DataCell>
              <Table.DataCell>Barnepensjon</Table.DataCell>
              <Table.DataCell>{beregningsperiode.trygdetid} år</Table.DataCell>
              <Table.DataCell>{beregningsperiode.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>
                {beregningsperiode.soeskenFlokk && soeker ? (
                  <BarnepensjonToolTip
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
            </Table.ExpandableRow>
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
