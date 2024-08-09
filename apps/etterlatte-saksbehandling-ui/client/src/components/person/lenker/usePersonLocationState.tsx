import { useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'

/**
 * Burde på sikt bruke cookies i stedet for å sikre at nøkler som ikke blir brukt slettes fra nettleseren
 **/
export const lagrePersonLocationState = (key: string, fnr: string) => {
  localStorage.setItem(key, JSON.stringify({ fnr }))
}

const hentPersonLocationState = (key: string | null) => {
  if (key) {
    const jsonState = localStorage.getItem(key)
    return jsonState ? JSON.parse(jsonState) : null
  } else return {}
}

interface PersonLocationState {
  fnr: string
}

/**
 * Hack for å støtte åpning av personsiden i ny fane.
 * FNR lagres med en nøkkel (UUID) i localStorage og hentes opp igjen når [Person.tsx] lastes.
 **/
export const usePersonLocationState = (key: string | null): PersonLocationState => {
  const location = useLocation()
  const [state, setState] = useState<PersonLocationState>(location.state || hentPersonLocationState(key))

  useEffect(() => {
    if (key) localStorage.removeItem(key)
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
