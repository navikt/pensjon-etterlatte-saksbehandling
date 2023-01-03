import { BodyShort, Button, Heading, Label, Popover, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { useRef, useState } from 'react'
import { InformationColored } from '@navikt/ds-icons'
import { differenceInYears, lastDayOfMonth } from 'date-fns'
import { formaterDato, formaterStringDato } from '~utils/formattering'
import { IPdlPerson } from '~shared/types/Person'
import { Beregning } from '~shared/types/Beregning'

interface ToolTipPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string | Date
}

interface Props {
  beregning: Beregning
  soeker?: IPdlPerson
  soesken: IPdlPerson[]
}

export const Sammendrag = ({ beregning, soeker, soesken }: Props) => {
  const beregningsperioder = beregning.beregningsperioder

  //TODO: egen komponent
  const GjelderTooltip = ({ soesken, soeker }: { soesken: ToolTipPerson[]; soeker: ToolTipPerson }) => {
    const [isOpen, setIsOpen] = useState(false)
    const ref = useRef(null)

    const soeskenFlokk = [...soesken, soeker]

    return (
      <>
        {soeskenFlokk.length} barn
        <IconButton
          size="small"
          ref={ref}
          onClick={() => setIsOpen(true)}
          icon={<InformationColored title="Få mer informasjon om beregningsgrunnlaget" />}
        />
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
                {beregningsperiode.type == 'GP' ? 'Grunnpensjon' : beregningsperiode.type}
              </Table.DataCell>
              <Table.DataCell>40 år</Table.DataCell>
              <Table.DataCell>{beregningsperiode.grunnbelop} kr</Table.DataCell>
              <Table.DataCell>
                {beregningsperiode.soeskenFlokk && soeker ? (
                  <GjelderTooltip
                    soesken={beregningsperiode.soeskenFlokk.map((fnr) => {
                      const pdlPerson = soesken.find((p) => p.foedselsnummer === fnr)
                      return pdlPerson!!
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

const PopoverContent = styled(Popover.Content)`
  max-width: 500px;
`

const IconButton = styled(Button)`
  height: 1.25rem;
  width: 1.25rem;
  border-radius: 50%;
  padding: 0;
  min-width: 0;
  vertical-align: sub;
  margin-left: 4px;
`
const ListWithoutBullet = styled.li`
  list-style-type: none;
`
