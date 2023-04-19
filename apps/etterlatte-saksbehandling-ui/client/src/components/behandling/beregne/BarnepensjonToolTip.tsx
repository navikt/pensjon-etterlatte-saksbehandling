import { useRef, useState } from 'react'
import { InformationColored } from '@navikt/ds-icons'
import { BodyShort, Button, Heading, Label, Popover } from '@navikt/ds-react'
import { differenceInYears } from 'date-fns'
import styled from 'styled-components'

interface ToolTipPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string | Date
}

export const BarnepensjonToolTip = ({ soesken, soeker }: { soesken: ToolTipPerson[]; soeker: ToolTipPerson }) => {
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
        onBlur={() => setIsOpen(false)}
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
