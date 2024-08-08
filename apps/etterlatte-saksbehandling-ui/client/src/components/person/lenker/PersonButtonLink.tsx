import { PersonOversiktFane } from '~components/person/Person'
import { HTMLAttributeAnchorTarget } from 'react'
import { Button, ButtonProps } from '@navikt/ds-react'
import { lagrePersonLocationState } from '~components/person/lenker/usePersonLocationState'
import { NavLink } from 'react-router-dom'

interface PersonButtonLinkProps extends Omit<ButtonProps, 'onClick'> {
  fnr: string
  fane?: PersonOversiktFane
  target?: HTMLAttributeAnchorTarget | undefined
  queryParams?: Record<string, string>
}

/**
 * Lenker til personsiden med location state slik at vi unngår fnr i URL-en.
 *
 * Knapplenke i tilfeller hvor man ønsker en knapp med samme funksjonalitet som en lenke
 **/
export const PersonButtonLink = ({ fnr, fane, queryParams, target, ...rest }: PersonButtonLinkProps) => {
  const key = window.crypto.randomUUID()

  const params = new URLSearchParams({
    ...queryParams,
    key: key || '',
    fane: fane || PersonOversiktFane.SAKER,
  })

  return (
    <Button
      {...rest}
      as={NavLink}
      to={`/person?${params}`}
      onContextMenu={() => lagrePersonLocationState(key, fnr)}
      onClick={() => lagrePersonLocationState(key, fnr)}
      state={{ fnr }}
      target={target}
    />
  )
}
