import { Alert, AlertProps } from '@navikt/ds-react'
import { ReactNode, useState } from 'react'
import styled from 'styled-components'

interface FixedAlertProps extends Omit<AlertProps, 'children'> {
  melding: ReactNode
  position?: 'bottom-center' | undefined
}

export const FixedAlert = ({ melding, variant, position, ...rest }: FixedAlertProps) => {
  const [visible, setVisible] = useState(true)

  if (!visible) return null

  return position === 'bottom-center' ? (
    <FixedAlertCompBottom variant={variant} {...rest} closeButton={true} onClose={() => setVisible(false)}>
      {melding}
    </FixedAlertCompBottom>
  ) : (
    <FixedAlertComp variant={variant} {...rest} closeButton={true} onClose={() => setVisible(false)}>
      {melding}
    </FixedAlertComp>
  )
}

const FixedAlertComp = styled(Alert)`
  position: fixed;
  right: 3rem;
  top: 3rem;
  z-index: 1;
  box-shadow: -0.3rem 0.3rem 0.6rem 0 rgba(150, 150, 150, 0.5);
`

const FixedAlertCompBottom = styled(Alert)`
  position: fixed;
  bottom: 6rem;
  left: max(65rem, 45%);
  z-index: 1;
  box-shadow: -0.3rem 0.3rem 0.6rem 0 rgba(150, 150, 150, 0.5);
`
