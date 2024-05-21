import { Alert, AlertProps } from '@navikt/ds-react'
import { ReactNode, useEffect, useState } from 'react'
import styled from 'styled-components'

interface ToastProps extends Omit<AlertProps, 'variant' | 'children'> {
  melding: ReactNode
  position?: 'bottom-center' | undefined
  timeout?: number
}

/**
 * Enkel "Toast" som vises øverst i høyre hjørne.
 * Forsvinner av seg selv etter 5 sekunder.
 **/
export const Toast = ({ melding, position, timeout, ...rest }: ToastProps) => {
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    const id = setTimeout(() => {
      setVisible(false)
    }, timeout || 5000)

    return () => clearTimeout(id)
  }, [])

  if (!visible) return null

  return position === 'bottom-center' ? (
    <ToastAlertBottom variant="success" {...rest} closeButton={true} onClose={() => setVisible(false)}>
      {melding}
    </ToastAlertBottom>
  ) : (
    <ToastAlert variant="success" {...rest} closeButton={true} onClose={() => setVisible(false)}>
      {melding}
    </ToastAlert>
  )
}

const ToastAlert = styled(Alert)`
  position: fixed;
  right: 3rem;
  top: 3rem;
  z-index: 1;
  box-shadow: -0.3rem 0.3rem 0.6rem 0 rgba(150, 150, 150, 0.5);
`

const ToastAlertBottom = styled(Alert)`
  position: fixed;
  bottom: 6rem;
  left: max(65rem, 45%);
  z-index: 1;
  box-shadow: -0.3rem 0.3rem 0.6rem 0 rgba(150, 150, 150, 0.5);
`
