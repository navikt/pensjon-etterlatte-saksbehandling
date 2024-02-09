import { useEffect, useState } from 'react'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { FilterRad } from '~components/oppgavebenk/FilterRad'
import { Filter, filtrerOppgaver } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { hentFilterFraLocalStorage, leggFilterILocalStorage } from '~components/oppgavebenk/filter/filterLocalStorage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, hentOppgaverMedStatus, OppgaveDTO } from '~shared/api/oppgaver'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { oppdaterTildeling, sorterOppgaverEtterOpprettet } from '~components/oppgavebenk/oppgaveutils'
import { useAppDispatch } from '~store/Store'
import { settHovedOppgavelisteLengde } from '~store/reducers/OppgavelisteReducer'

export const OppgavelistaWrapper = () => {
  const dispatch = useAppDispatch()

  const [hentedeOppgaver, setHentedeOppgaver] = useState<OppgaveDTO[]>([])
  const [filter, setFilter] = useState<Filter>(hentFilterFraLocalStorage())

  const [oppgaver, hentOppgaverStatusFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaver, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)

  const hentAlleOppgaver = () => {
    hentOppgaverStatusFetch({ oppgavestatusFilter: filter.oppgavestatusFilter })
    hentGosysOppgaverFunc({})
  }
  useEffect(() => hentAlleOppgaver(), [])
  useEffect(() => {
    leggFilterILocalStorage(filter)
  }, [filter])

  useEffect(() => {
    if (isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      const alleOppgaver = sorterOppgaverEtterOpprettet([...oppgaver.data, ...gosysOppgaver.data])
      setHentedeOppgaver(alleOppgaver)
    } else if (isSuccess(oppgaver) && !isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(sorterOppgaverEtterOpprettet(oppgaver.data))
    } else if (!isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(sorterOppgaverEtterOpprettet(gosysOppgaver.data))
    }
  }, [oppgaver, gosysOppgaver])

  useEffect(() => {
    dispatch(settHovedOppgavelisteLengde(hentedeOppgaver.length))
  }, [hentedeOppgaver])

  const mutableOppgaver = hentedeOppgaver.concat()

  const filtrerteOppgaver = filtrerOppgaver(
    filter.enhetsFilter,
    filter.fristFilter,
    filter.saksbehandlerFilter,
    filter.ytelseFilter,
    filter.oppgavestatusFilter,
    filter.oppgavetypeFilter,
    filter.oppgavekildeFilter,
    mutableOppgaver,
    filter.fristSortering,
    filter.fnrSortering,
    filter.fnrFilter
  )

  return (
    <>
      {isPending(oppgaver) && <Spinner visible={true} label="Henter nye oppgaver" />}
      {isFailureHandler({
        apiResult: oppgaver,
        errorMessage: 'Kunne ikke hente oppgaver',
      })}
      {isFailureHandler({
        apiResult: gosysOppgaver,
        errorMessage: 'Kunne ikke hente gosys oppgaver',
      })}
      {isSuccess(oppgaver) && (
        <>
          <FilterRad
            hentAlleOppgaver={hentAlleOppgaver}
            hentOppgaverStatus={(oppgavestatusFilter: Array<string>) =>
              hentOppgaverStatusFetch({ oppgavestatusFilter: oppgavestatusFilter })
            }
            filter={filter}
            setFilter={setFilter}
            alleOppgaver={hentedeOppgaver}
          />
          <Oppgavelista
            oppgaver={filtrerteOppgaver}
            oppdaterTildeling={() => oppdaterTildeling(setHentedeOppgaver, hentedeOppgaver)}
            hentOppgaver={hentAlleOppgaver}
            filter={filter}
            setFilter={setFilter}
            totaltAntallOppgaver={hentedeOppgaver.length}
            erMinOppgaveliste={false}
          />
        </>
      )}
    </>
  )
}
