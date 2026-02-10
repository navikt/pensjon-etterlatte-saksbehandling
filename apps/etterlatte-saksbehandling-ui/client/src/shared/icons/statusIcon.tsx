import { CheckmarkCircleIcon, ExclamationmarkTriangleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'

export type StatusIconProps = 'warning' | 'success' | 'error'

export const StatusIcon = (props: { status: StatusIconProps }) => {
  switch (props.status) {
    case 'success':
      return <CheckmarkCircleIcon color="var(--a-green-500)" stroke="var(--a-green-500)" aria-hidden="true" />
    case 'error':
      return (
        <XMarkOctagonIcon
          color="var(--a-nav-red)"
          stroke="var(--a-nav-red)"
          fill="var(--a-nav-red)"
          aria-hidden="true"
        />
      )
    case 'warning':
      return <ExclamationmarkTriangleIcon color="var(--a-orange-600)" stroke="var(--a-orange-600)" aria-hidden="true" />
  }
}
