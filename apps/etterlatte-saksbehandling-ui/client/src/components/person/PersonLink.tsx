import { NavLink, useLocation } from 'react-router-dom'
import { PersonOversiktFane } from '~components/person/Person'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { Button } from '@navikt/ds-react'
import { useEffect, useState } from 'react'

const lagrePersonLocationState = (key: string, fnr: string) => {
  localStorage.setItem(key, JSON.stringify({ fnr }))
}

interface PersonLocationState {
  fnr: string
}

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
      window.history.pushState({ ...window.history.state, usr: state }, '', window.location.href)
    }
  }, [location])

  return state
}

export const PersonButtonLink = ({ fnr, fane }: { fnr: string; fane: PersonOversiktFane }) => {
  const key = window.crypto.randomUUID()

  return (
    <Button
      variant="tertiary"
      size="small"
      as={NavLink}
      to={`/person?fane=${fane}&key=${key}`}
      onClick={() => lagrePersonLocationState(key, fnr)}
      state={{ fnr }}
      target="_blank"
      icon={<ExternalLinkIcon />}
    >
      Gå til dokumentoversikten
    </Button>
  )
}

export const PersonLink = ({ fane, fnr }: { fane: PersonOversiktFane; fnr: string }) => {
  const key = window.crypto.randomUUID()

  return (
    <NavLink onClick={() => lagrePersonLocationState(key, fnr)} to={`/person?fane=${fane}&key=${key}`}>
      {fnr}
    </NavLink>
  )
}
