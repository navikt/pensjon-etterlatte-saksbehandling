import { useRef, useState } from 'react'
import { BodyShort, Button, Popover } from '@navikt/ds-react'
import styled from 'styled-components'
import { InformationSquareIcon } from '@navikt/aksel-icons'
import { ABlue500 } from '@navikt/ds-tokens/dist/tokens'

export const OmstillingsstoenadToolTip = () => {
  const [isOpen, setIsOpen] = useState(false)
  const ref = useRef(null)

  return (
    <>
      <IconButton
        size="small"
        ref={ref}
        onClick={() => setIsOpen(true)}
        icon={<InformationSquareIconBlue title="Få mer informasjon om beregningsgrunnlaget" />}
        onBlur={() => setIsOpen(false)}
      />
      <Popover anchorEl={ref.current} open={isOpen} onClose={() => setIsOpen(false)} placement="top">
        <PopoverContent>
          <BodyShort spacing>
            <strong>Folketrygdloven § 17-6</strong> Full årlig omstillingsstønad utgjør 2,25 ganger grunnbeløpet (G),
            forutsatt at den avdøde hadde 40 års (full) trygdetid (folketrygdloven §§ 3-5 og 3-7). Dersom trygdetiden er
            kortere, reduseres omstillingsstønaden forholdsmessig. Eks. på månedlig utbetaling ved 30 års trygdetid:
            2,25 G * 30/40 /12 mnd.
          </BodyShort>
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
const InformationSquareIconBlue = styled(InformationSquareIcon)`
  color: ${ABlue500};
`
