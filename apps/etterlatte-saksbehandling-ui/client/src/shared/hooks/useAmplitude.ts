import amplitude from '@amplitude/analytics-browser'
import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'

export enum EventType {
  WHY_WE_ASK = 'hvorfor spør vi',
  CANCEL_APPLICATION = 'avbryt soknad',
}

export enum LogEvents {
  START_APPLICATION = 'skjema startet',
  SELECT_SCENARIO = 'scenario valgt',
  SELECT_SITUATION = 'situasjon valgt',
  SEND_APPLICATION = 'send soknad',
  SEND_APPLICATION_ERROR = 'send soknad feilet',
  PAGE_CHANGE = 'sidevisning',
  CHANGE_LANGUAGE = 'endre sprak',
  SYSTEM_UNAVAILABLE = 'system utilgjengelig',
  PAGE_NOT_FOUND = 'side ikke funnet',
  CLICK = 'klikk',
  VALIDATION_ERROR = 'valideringsfeil',
}

const getAmplitudeKey = () => {
  if (typeof window === 'undefined') return '' // server side
  if (window.location.href.includes('dev.nav.no')) return '3acd3a52e065d2d99856a12e7e9e1432' // dev
  if (window.location.href.includes('nav.no')) return AMPLITUDE_TOKEN // prod
  return '' // other e.g. localhost
}

export const useAmplitude = () => {
  const location = useLocation()
  const [prevLocation, setPrevLocation] = useState<any>(location)

  useEffect(() => {
    amplitude.init(getAmplitudeKey(), '', {
      serverUrl: 'amplitude.nav.no/collect-auto',
      ingestionMetadata: {
        sourceName: window.location.toString(),
      },
    })
  }, [])

  useEffect(() => {
    if (prevLocation?.pathname !== location?.pathname) {
      logEvent(LogEvents.PAGE_CHANGE)
    }
    setPrevLocation(location)
  }, [location])

  const logEvent = (eventName: LogEvents, eventData?: any): void => {
    setTimeout(() => {
      try {
        if (amplitude) {
          amplitude.logEvent(eventName, eventData)
        }
      } catch (error) {
        console.error(error)
      }
    }, 0)
  }
  return { logEvent }
}
