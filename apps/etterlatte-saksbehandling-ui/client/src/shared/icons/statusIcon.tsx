import { CheckmarkCircleIcon, ExclamationmarkTriangleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'
import { AGreen500, ANavRed, AOrange600 } from '@navikt/ds-tokens/dist/tokens'

export type StatusIconProps = 'warning' | 'success' | 'error'

export const StatusIcon = (props: { status: StatusIconProps }) => {
  switch (props.status) {
    case 'success':
      return <CheckmarkCircleIcon color={AGreen500} stroke={AGreen500} aria-hidden="true" />
    case 'error':
      return <XMarkOctagonIcon color={ANavRed} stroke={ANavRed} fill={ANavRed} aria-hidden="true" />
    case 'warning':
      return <ExclamationmarkTriangleIcon color={AOrange600} stroke={AOrange600} aria-hidden="true" />
  }
}
