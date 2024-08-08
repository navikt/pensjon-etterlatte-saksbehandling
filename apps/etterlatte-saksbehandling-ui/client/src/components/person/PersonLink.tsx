import { NavLink, NavLinkProps, useLocation } from 'react-router-dom'
import { PersonOversiktFane } from '~components/person/Person'
import { Button } from '@navikt/ds-react'
import { HTMLAttributeAnchorTarget, useEffect, useState } from 'react'
import { ButtonProps } from '@navikt/ds-react/src/button/Button'

/**
 * Burde på sikt bruke cookies i stedet for å sikre at nøkler som ikke blir brukt slettes fra nettleseren
 **/
const lagrePersonLocationState = (key: string, fnr: string) => {
  localStorage.setItem(key, JSON.stringify({ fnr }))
}

interface PersonLocationState {
  fnr: string
}

/**
 * Hack for å støtte åpning av personsiden i ny fane.
 * FNR lagres med en nøkkel (UUID) i localStorage og hentes opp igjen når [Person.tsx] lastes.
 **/
export const usePersonLocationState = (key: string): PersonLocationState => {
  const jsonState = localStorage.getItem(key)
  const initialState = jsonState ? JSON.parse(jsonState) : null

  const location = useLocation()
  const [state, setState] = useState<PersonLocationState>(location.state || initialState)

  useEffect(() => {
    localStorage.removeItem(key)
  }, [])

  useEffect(() => {
    if (location.state?.fnr) {
      setState(location.state)
    } else {
      // Dersom location.state er tom (eks. ved ny fane) må dette settes manuelt på "gamle måten"
      window.history.pushState({ ...window.history.state, usr: state }, '', window.location.href)
    }
  }, [location])

  return state
}

interface PersonButtonLinkProps extends Omit<ButtonProps, 'onClick'> {
  fnr: string
  fane?: PersonOversiktFane
  target?: HTMLAttributeAnchorTarget | undefined
  queryParams?: Record<string, string>
}

/**
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

interface PersonLinkProps extends Omit<NavLinkProps, 'to' | 'onClick'> {
  fane?: PersonOversiktFane
  fnr: string
  label?: string
}

/**
 * Helt alminnelig lenke i tilfeller hvor man ønsker det (vanlig lenke med blå skrift)
 **/
export const PersonLink = ({ fane, fnr, children, ...rest }: PersonLinkProps) => {
  const key = window.crypto.randomUUID()

  const params = new URLSearchParams({
    key: key || '',
    fane: fane || PersonOversiktFane.SAKER,
  })

  return (
    <NavLink
      {...rest}
      onContextMenu={() => lagrePersonLocationState(key, fnr)}
      onClick={() => lagrePersonLocationState(key, fnr)}
      to={`/person?${params}`}
      state={{ fnr }}
    >
      {children || fnr}
    </NavLink>
  )
}
