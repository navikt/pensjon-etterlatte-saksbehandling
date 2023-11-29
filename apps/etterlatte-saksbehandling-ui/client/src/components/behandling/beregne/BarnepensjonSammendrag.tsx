import { Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { compareDesc, lastDayOfMonth } from 'date-fns'
import { formaterDato, formaterStringDato } from '~utils/formattering'
import { Beregning } from '~shared/types/Beregning'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Barnepensjonberegningssammendrag } from '~components/behandling/beregne/Barnepensjonberegningssammendrag'
import { ProrataBroek } from '~components/behandling/beregne/ProrataBroek'
import { hentLevendeSoeskenFraAvdoedeForSoekerNy } from '~shared/types/Person'
import { usePersonopplysningerAvdoede } from '~components/person/usePersonopplysninger'

interface Props {
  behandling: IDetaljertBehandling
  beregning: Beregning
}

const BenyttetTrygdetid = ({
  trygdetid,
  beregningsMetode,
  samletNorskTrygdetid,
  samletTeoretiskTrygdetid,
}: {
  trygdetid: number
  beregningsMetode: string | undefined
  samletNorskTrygdetid: number | undefined
  samletTeoretiskTrygdetid: number | undefined
}) => {
  let benyttetTrygdetid = trygdetid

  if (beregningsMetode === 'NASJONAL' && samletNorskTrygdetid) {
    benyttetTrygdetid = samletNorskTrygdetid
  }

  if (beregningsMetode === 'PRORATA' && samletTeoretiskTrygdetid) {
    benyttetTrygdetid = samletTeoretiskTrygdetid
  }

  return <>{benyttetTrygdetid}</>
}

export const BarnepensjonSammendrag = ({ behandling, beregning }: Props) => {
  const avdoede = usePersonopplysningerAvdoede()
  if (!avdoede) {
    return null
  }
  const beregningsperioder = [...beregning.beregningsperioder].sort((a, b) =>
    compareDesc(new Date(a.datoFOM), new Date(b.datoFOM))
  )
  const soeker = behandling.søker
  const soesken = hentLevendeSoeskenFraAvdoedeForSoekerNy(avdoede, soeker?.foedselsnummer as string)

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
            <Table.HeaderCell>Månedelig utbetaling før skatt</Table.HeaderCell>
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
                      beregningsperiode={beregningsperiode}
                      soeker={soeker}
                      soesken={soesken}
                    />
                  )}
                </>
              }
            >
              <Table.DataCell>
                {`${formaterStringDato(beregningsperiode.datoFOM)} - ${
                  beregningsperiode.datoTOM ? formaterDato(lastDayOfMonth(new Date(beregningsperiode.datoTOM))) : ''
                }`}
              </Table.DataCell>
              <Table.DataCell>Barnepensjon</Table.DataCell>
              <Table.DataCell>
                <BenyttetTrygdetid {...beregningsperiode} /> år
              </Table.DataCell>
              <Table.DataCell>
                {beregningsperiode.broek && beregningsperiode.beregningsMetode === 'PRORATA' && (
                  <ProrataBroek broek={beregningsperiode.broek} />
                )}
              </Table.DataCell>
              <Table.DataCell>{beregningsperiode.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>
                {beregningsperiode.soeskenFlokk && `${beregningsperiode.soeskenFlokk.length + 1} barn`}{' '}
                {beregningsperiode.institusjonsopphold && ' institusjonsopphold'}
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
  max-width: 1200px;
  .table {
    max-width: 1200px;

    .tableCell {
      max-width: 100px;
    }
  }
`
