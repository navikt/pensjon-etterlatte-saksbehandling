import { useRef, useState } from 'react'
import { BodyShort, Button, Popover } from '@navikt/ds-react'
import styled from 'styled-components'
import { InformationSquareIcon } from '@navikt/aksel-icons'

export const OmstillingsstoenadToolTip = (props: {
  title: string
  children: string | JSX.Element | (string | JSX.Element)[]
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const ref = useRef(null)

  return (
    <>
      <IconButton
        size="small"
        ref={ref}
        onClick={() => setIsOpen(true)}
        icon={<InformationSquareIcon title={props.title} />}
        onBlur={() => setIsOpen(false)}
      />
      <Popover anchorEl={ref.current} open={isOpen} onClose={() => setIsOpen(false)} placement="top">
        <PopoverContent>
          <BodyShort spacing>{props.children}</BodyShort>
        </PopoverContent>
      </Popover>
    </>
  )
}

const PopoverContent = styled(Popover.Content)`
  max-width: 500px;
`

const IconButton = styled(Button).attrs({ variant: 'tertiary' })`
  height: 1.25rem;
  width: 1.25rem;
  padding: 0;
  min-width: 0;
  vertical-align: sub;
  margin-left: 4px;
`
