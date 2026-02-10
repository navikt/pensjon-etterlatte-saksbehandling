import { CheckmarkCircleIcon, ExclamationmarkTriangleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'

export type StatusIconProps = 'warning' | 'success' | 'error'

export const StatusIcon = (props: { status: StatusIconProps }) => {
  switch (props.status) {
    case 'success':
      return <CheckmarkCircleIcon color="var(--ax-success-600)" stroke="var(--ax-success-600)" aria-hidden="true" />
    case 'error':
      return (
        <XMarkOctagonIcon
          color="var(--ax-text-logo)"
          stroke="var(--ax-text-logo)"
          fill="var(--ax-text-logo)"
          aria-hidden="true"
        />
      )
    case 'warning':
      return (
        <ExclamationmarkTriangleIcon color="var(--ax-warning-700)" stroke="var(--ax-warning-700)" aria-hidden="true" />
      )
  }
}
