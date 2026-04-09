import { CheckmarkCircleIcon, ExclamationmarkTriangleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'

import { Warning700, Success600, TextLogo } from '@navikt/ds-tokens/js'

export type StatusIconProps = 'warning' | 'success' | 'error'

export const StatusIcon = (props: { status: StatusIconProps }) => {
  switch (props.status) {
    case 'success':
      return <CheckmarkCircleIcon color={Success600} stroke={Success600} aria-hidden="true" />
    case 'error':
      return <XMarkOctagonIcon color={TextLogo} stroke={TextLogo} fill={TextLogo} aria-hidden="true" />
    case 'warning':
      return <ExclamationmarkTriangleIcon color={Warning700} stroke={Warning700} aria-hidden="true" />
  }
}
