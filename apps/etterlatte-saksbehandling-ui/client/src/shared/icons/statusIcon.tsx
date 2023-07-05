import styled from 'styled-components'
import { SuccessColored } from '@navikt/ds-icons'
import { ErrorColored } from '@navikt/ds-icons'
import { WarningColored } from '@navikt/ds-icons'

export type StatusIconProps = 'warning' | 'success' | 'error'

export const StatusIcon = (props: { status: StatusIconProps }) => {
  const symbol = hentSymbol()

  function hentSymbol() {
    switch (props.status) {
      case 'success':
        return <SuccessColored aria-hidden={'true'} /> // Vurder å bruk tittel for å forklare istendenfor å skjule
      case 'error':
        return <ErrorColored aria-hidden={'true'} />
      case 'warning':
        return <WarningColored aria-hidden={'true'} />
    }
  }

  return <SvgWrapper status={props.status}>{symbol}</SvgWrapper>
}

const SvgWrapper = styled.div<{ status: StatusIconProps }>`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
  padding-left: 0;
`
