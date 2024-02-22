import { Alert, AlertProps } from '@navikt/ds-react'
import { ReactNode, useEffect, useState } from 'react'
import styled from 'styled-components'

interface ToastProps extends Omit<AlertProps, 'variant' | 'children'> {
  melding: ReactNode
}

/**
 * Enkel "Toast" som vises øverst i høyre hjørne.
 * Forsvinner av seg selv etter 5 sekunder.
 **/
export const Toast = ({ melding, ...rest }: ToastProps) => {
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    const id = setTimeout(() => {
      setVisible(false)
    }, 5000)

    return () => clearTimeout(id)
  }, [])

  if (!visible) return null

  return (
    <ToastAlert variant="success" {...rest} closeButton={true} onClose={() => setVisible(false)}>
      {melding}
    </ToastAlert>
  )
}

const ToastAlert = styled(Alert)`
  position: absolute;
  right: 3rem;
  top: 3rem;
  box-shadow: -0.3rem 0.3rem 0.6rem 0 rgba(150, 150, 150, 0.5);
`
