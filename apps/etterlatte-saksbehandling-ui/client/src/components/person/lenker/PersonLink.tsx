import { NavLink } from 'react-router-dom'
import { PersonOversiktFane } from '~components/person/Person'
import { lagrePersonLocationState } from '~components/person/lenker/usePersonLocationState'
import { Link, LinkProps } from '@navikt/ds-react'

interface PersonLinkProps extends Omit<LinkProps, 'href' | 'onClick'> {
  fane?: PersonOversiktFane
  fnr: string
  label?: string
}

/**
 * Lenker til personsiden med location state slik at vi unngår fnr i URL-en.
 *
 * Helt alminnelig lenke i tilfeller hvor man ønsker det (vanlig lenke med blå skrift)
 **/
export const PersonLink = ({ fane, fnr, ...rest }: PersonLinkProps) => {
  const key = window.crypto.randomUUID()

  const params = new URLSearchParams({
    key: key || '',
    fane: fane || PersonOversiktFane.SAKER,
  })

  return (
    <Link
      {...rest}
      as={NavLink}
      onContextMenu={() => lagrePersonLocationState(key, fnr)}
      onClick={() => lagrePersonLocationState(key, fnr)}
      to={`/person?${params}`}
      state={{ fnr }}
    />
  )
}
