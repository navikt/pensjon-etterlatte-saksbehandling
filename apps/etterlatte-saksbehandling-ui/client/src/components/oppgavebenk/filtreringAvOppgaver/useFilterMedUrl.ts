import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'

/**
 * Filter-felter som er tillatt i URL. SakEllerFnrFilter holdes utenfor av hensyn til personvern.
 */
export type FilterUrlNokler = 'frist' | 'saksbehandler' | 'enhet' | 'ytelse' | 'oppgavestatus' | 'oppgavetype'

export const OPPGAVELISTA_URL_NOKLER: FilterUrlNokler[] = [
  'frist',
  'saksbehandler',
  'enhet',
  'ytelse',
  'oppgavestatus',
  'oppgavetype',
]

export const MIN_OPPGAVELISTE_URL_NOKLER: FilterUrlNokler[] = ['frist', 'ytelse', 'oppgavestatus', 'oppgavetype']

function parseFilterFraUrl(
  searchParams: URLSearchParams,
  fallback: Filter,
  urlNokkel: FilterUrlNokler[]
): Filter | null {
  const harNoenParams = urlNokkel.some((nokkel) => searchParams.has(nokkel))
  if (!harNoenParams) return null

  return {
    ...fallback,
    sakEllerFnrFilter: '', // aldri fra URL
    ...(urlNokkel.includes('frist') && searchParams.has('frist')
      ? { fristFilter: searchParams.get('frist') as Filter['fristFilter'] }
      : {}),
    ...(urlNokkel.includes('saksbehandler') && searchParams.has('saksbehandler')
      ? { saksbehandlerFilter: searchParams.get('saksbehandler')! }
      : {}),
    ...(urlNokkel.includes('enhet') && searchParams.has('enhet')
      ? { enhetsFilter: searchParams.get('enhet') as Filter['enhetsFilter'] }
      : {}),
    ...(urlNokkel.includes('ytelse') && searchParams.has('ytelse')
      ? { ytelseFilter: searchParams.get('ytelse') as Filter['ytelseFilter'] }
      : {}),
    ...(urlNokkel.includes('oppgavestatus') && searchParams.has('oppgavestatus')
      ? { oppgavestatusFilter: searchParams.get('oppgavestatus')!.split(',').filter(Boolean) }
      : {}),
    ...(urlNokkel.includes('oppgavetype') && searchParams.has('oppgavetype')
      ? { oppgavetypeFilter: searchParams.get('oppgavetype')!.split(',').filter(Boolean) }
      : {}),
  }
}

/**
 * Bygger nye search params basert på faktisk nåværende URL (window.location.search).
 * Bevarer tab-parameteren og erstatter kun filter-parameterne for dette tabets nøkler.
 *
 * Vi bruker window.location.search fremfor React Routers searchParams-kontekst fordi
 * konteksten kan ligge ett render bak (stale) ved tab-bytte, noe som vil gi feil URL.
 */
function byggSearchParams(filter: Filter, urlNokkel: FilterUrlNokler[]): URLSearchParams {
  const params = new URLSearchParams(window.location.search)

  // Fjern alle eksisterende filter-params for dette tabets nøkler
  urlNokkel.forEach((nokkel) => params.delete(nokkel))

  // Sett nye filter-params
  if (urlNokkel.includes('frist')) params.set('frist', filter.fristFilter)
  if (urlNokkel.includes('saksbehandler')) params.set('saksbehandler', filter.saksbehandlerFilter)
  if (urlNokkel.includes('enhet')) params.set('enhet', filter.enhetsFilter)
  if (urlNokkel.includes('ytelse')) params.set('ytelse', filter.ytelseFilter)
  if (urlNokkel.includes('oppgavestatus')) params.set('oppgavestatus', filter.oppgavestatusFilter.join(','))
  if (urlNokkel.includes('oppgavetype')) params.set('oppgavetype', filter.oppgavetypeFilter.join(','))

  return params
}

/**
 * Hook som synkroniserer filter med URL og localStorage.
 *
 * Prioritetsrekkefølge ved initialisering:
 * 1. URL-parametere (overskrives av bruker ved navigasjon til en delt lenke)
 * 2. localStorage (husker forrige valg)
 *
 * Når brukeren endrer et filter oppdateres URL og localStorage uten sidereload.
 * Ingen loop: URL leses kun én gang ved mount, ikke i en reaktiv effect.
 *
 * Vi leser/skriver window.location.search direkte overalt fordi React Routers
 * searchParams-kontekst kan ligge ett render bak ved tab-bytte.
 */
export function useFilterMedUrl(
  urlNokler: FilterUrlNokler[],
  hentFraLocalStorage: () => Filter,
  lagreILocalStorage: (filter: Filter) => void
): [Filter, (filter: Filter) => void] {
  const [, setSearchParams] = useSearchParams()

  const [filter, setFilterState] = useState<Filter>(() => {
    const faktiskeParams = new URLSearchParams(window.location.search)
    const fraLocalStorage = hentFraLocalStorage()
    const fraUrl = parseFilterFraUrl(faktiskeParams, fraLocalStorage, urlNokler)

    if (fraUrl !== null) {
      lagreILocalStorage(fraUrl)
      return fraUrl
    }

    return fraLocalStorage
  })

  // Ved mount: hvis URL ikke har filter-parametere, sett dem fra gjeldende filter.
  // Kjøres kun én gang. Ingen loop siden vi ikke lytter på URL-endringer.
  useEffect(() => {
    const faktiskeParams = new URLSearchParams(window.location.search)
    const harNoenParams = urlNokler.some((nokkel) => faktiskeParams.has(nokkel))
    if (!harNoenParams) {
      setSearchParams(byggSearchParams(filter, urlNokler), { replace: true })
    }
  }, [])

  const setFilter = (nyFilter: Filter) => {
    setFilterState(nyFilter)
    lagreILocalStorage(nyFilter)
    setSearchParams(byggSearchParams(nyFilter, urlNokler), { replace: true })
  }

  return [filter, setFilter]
}
