import { Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { compareDesc, isBefore, lastDayOfMonth } from 'date-fns'
import { formaterDato } from '~utils/formatering/dato'
import { Beregning } from '~shared/types/Beregning'
import {
  Barnepensjonberegningssammendrag,
  SISTE_DATO_GAMMELT_REGELVERK,
} from '~components/behandling/beregne/Barnepensjonberegningssammendrag'
import { ProrataBroek } from '~components/behandling/beregne/ProrataBroek'
import { hentLevendeSoeskenFraAvdoedeForSoeker } from '~shared/types/Person'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { BenyttetTrygdetid } from '~components/behandling/beregne/BenyttetTrygdetid'

interface Props {
  beregning: Beregning
}

export const BarnepensjonSammendrag = ({ beregning }: Props) => {
  const personopplysninger = usePersonopplysninger()

  const beregningsperioder = [...beregning.beregningsperioder].sort((a, b) =>
    compareDesc(new Date(a.datoFOM), new Date(b.datoFOM))
  )
  const soeker = personopplysninger?.soeker?.opplysning
  const soesken =
    (personopplysninger &&
      hentLevendeSoeskenFraAvdoedeForSoeker(personopplysninger.avdoede, soeker?.foedselsnummer as string)) ??
    []

  const TableWrapper = styled.div`
    display: flex;
    flex-wrap: wrap;
    max-width: 1200px;

    .table {
      max-width: 1200px;

      .tableCell {
        max-width: 100px;
      }
    }
  `

  return (
    <TableWrapper>
      <Heading spacing size="small" level="2">
        Beregningssammendrag
      </Heading>
      <Table className="table" zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell>Periode</Table.HeaderCell>
            <Table.HeaderCell>Ytelse</Table.HeaderCell>
            <Table.HeaderCell>Trygdetid</Table.HeaderCell>
            <Table.HeaderCell>Prorata Brøk</Table.HeaderCell>
            <Table.HeaderCell>Grunnbeløp</Table.HeaderCell>
            <Table.HeaderCell>Beregning gjelder</Table.HeaderCell>
            <Table.HeaderCell>Månedlig utbetaling før skatt</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {beregningsperioder?.map((beregningsperiode, key) => (
            <Table.ExpandableRow
              key={key}
              shadeOnHover={false}
              content={
                <>
                  {soeker && (
                    <Barnepensjonberegningssammendrag
                      overstyring={beregning.overstyrBeregning}
                      beregningsperiode={beregningsperiode}
                      soeker={soeker}
                      soesken={soesken}
                    />
                  )}
                </>
              }
            >
              <Table.DataCell>
                {`${formaterDato(beregningsperiode.datoFOM)} - ${
                  beregningsperiode.datoTOM ? formaterDato(lastDayOfMonth(new Date(beregningsperiode.datoTOM))) : ''
                }`}
              </Table.DataCell>
              <Table.DataCell>Barnepensjon</Table.DataCell>
              <Table.DataCell>
                <BenyttetTrygdetid {...beregningsperiode} />
              </Table.DataCell>
              <Table.DataCell>
                {beregningsperiode.broek && beregningsperiode.beregningsMetode === 'PRORATA' && (
                  <ProrataBroek broek={beregningsperiode.broek} />
                )}
              </Table.DataCell>
              <Table.DataCell>{beregningsperiode.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>
                {beregningsperiode.soeskenFlokk &&
                  isBefore(beregningsperiode.datoFOM, SISTE_DATO_GAMMELT_REGELVERK) &&
                  `${beregningsperiode.soeskenFlokk.length + 1} barn`}{' '}
                {beregningsperiode.institusjonsopphold && ' Institusjonsopphold'}
              </Table.DataCell>
              <Table.DataCell>{beregningsperiode.utbetaltBeloep} kr</Table.DataCell>
            </Table.ExpandableRow>
          ))}
        </Table.Body>
      </Table>
    </TableWrapper>
  )
}
