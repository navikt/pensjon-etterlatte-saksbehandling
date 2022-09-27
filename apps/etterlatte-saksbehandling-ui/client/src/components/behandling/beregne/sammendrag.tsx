import { BodyShort, Button, Label, Popover, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { useContext, useRef, useState } from 'react'
import { AppContext } from '../../../store/AppContext'
import { InformationColored } from '@navikt/ds-icons'
import { differenceInYears, lastDayOfMonth } from 'date-fns'
import { formaterDato, formaterStringDato } from '../../../utils/formattering'

interface ToolTipPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string | Date
}

export const Sammendrag = () => {
  const behandling = useContext(AppContext).state.behandlingReducer
  const beregningsperioder = behandling.beregning?.beregningsperioder

  const GjelderTooltip = ({ soesken, soeker }: { soesken: ToolTipPerson[]; soeker: ToolTipPerson }) => {
    const [isOpen, setIsOpen] = useState(false)
    const ref = useRef(null)

    const soeskenFlokk = [...soesken, soeker]

    return (
      <>
        {soeskenFlokk.length} barn
        <IconButton size="small" ref={ref} onClick={() => setIsOpen(true)}>
          <InformationColored title="Få mer informasjon om beregningsgrunnlaget" />
        </IconButton>
        <Popover anchorEl={ref.current} open={isOpen} onClose={() => setIsOpen(false)} placement="top">
          <PopoverContent>
            <Heading level="1" size="small">
              Søskenjustering
            </Heading>
            <BodyShort spacing>
              <strong>§18-5</strong> En forelder død: 40% av G til første barn, 25% av G til resterende. Beløpene slås
              sammen og fordeles likt.
            </BodyShort>
            <Label>Beregningen gjelder:</Label>
            <ul>
              {soeskenFlokk.map((soesken) => (
                <ListWithoutBullet key={soesken.foedselsnummer}>
                  {`${soesken.fornavn} ${soesken.etternavn} / ${soesken.foedselsnummer} / ${differenceInYears(
                    new Date(),
                    new Date(soesken.foedselsdato)
                  )} år`}
                </ListWithoutBullet>
              ))}
            </ul>
          </PopoverContent>
        </Popover>
      </>
    )
  }

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
                {`${formaterStringDato(beregning.datoFOM)} - ${
                  beregning.datoTOM ? formaterDato(lastDayOfMonth(new Date(beregning.datoTOM))) : ''
                }`}
              </Table.DataCell>
              <Table.DataCell>{beregning.type == 'GP' ? 'Grunnpensjon' : beregning.type}</Table.DataCell>
              <Table.DataCell>Mangler</Table.DataCell>
              <Table.DataCell>{beregning.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>
                {beregning.soeskenFlokk && behandling.søker ? (
                  <GjelderTooltip soesken={beregning.soeskenFlokk} soeker={behandling.søker} />
                ) : (
                  '-'
                )}
              </Table.DataCell>
              <Table.DataCell>{beregning.utbetaltBeloep} kr</Table.DataCell>
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

const PopoverContent = styled(Popover.Content)`
  max-width: 500px;
`

const IconButton = styled(Button)`
  border-radius: 50%;
  padding: 0;
  min-width: 0;
  vertical-align: sub;
  margin-left: 4px;
`
const ListWithoutBullet = styled.li`
  list-style-type: none;
`
