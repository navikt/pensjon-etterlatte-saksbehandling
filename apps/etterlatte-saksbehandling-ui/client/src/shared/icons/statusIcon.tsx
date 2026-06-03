import { CheckmarkCircleIcon, ExclamationmarkTriangleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'

const Success600 = 'var(--a-green-600)'
const Warning700 = 'var(--a-orange-700)'
const TextLogo = 'var(--a-nav-red)'

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
