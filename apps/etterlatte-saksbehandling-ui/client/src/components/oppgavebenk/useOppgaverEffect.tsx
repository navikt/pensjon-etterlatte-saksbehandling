import { useEffect, useState } from 'react'
import { hentGosysOppgaver, hentOppgaverMedStatus, OppgaveDTO } from '~shared/api/oppgaver'
import { isSuccess, Result } from '~shared/api/apiUtils'
import { sorterOppgaverEtterOpprettet } from '~components/oppgavebenk/oppgaveutils'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { settHovedOppgavelisteLengde, settMinOppgavelisteLengde } from '~store/reducers/OppgavelisteReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Filter, minOppgavelisteFiltre } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { hentFilterFraLocalStorage, leggFilterILocalStorage } from '~components/oppgavebenk/filter/filterLocalStorage'

export const useOppgaverEffect = (): {
  gosysOppgaverResult: Result<OppgaveDTO[]>
  hovedsideOppgaver: Array<OppgaveDTO>
  setMinsideOppgaver: (value: ((prevState: Array<OppgaveDTO>) => Array<OppgaveDTO>) | Array<OppgaveDTO>) => void
  setHovedsideFilter: (value: ((prevState: Filter) => Filter) | Filter) => void
  setMinsideFilter: (value: ((prevState: Filter) => Filter) | Filter) => void
  minsideOppgaverResult: Result<OppgaveDTO[]>
  hovedsideOppgaverResult: Result<OppgaveDTO[]>
  hentMinsideOppgaver: (oppgavestatusFilter: Array<string> | undefined) => void
  minsideOppgaver: Array<OppgaveDTO>
  setHovedsideOppgaver: (value: ((prevState: Array<OppgaveDTO>) => Array<OppgaveDTO>) | Array<OppgaveDTO>) => void
  hentMinsideOppgaverAlle: () => void
  hentHovedsideOppgaverAlle: () => void
  hentHovedsideOppgaver: (oppgavestatusFilter: Array<string> | undefined) => void
  minsideFilter: Filter
  hovedsideFilter: Filter
} => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const dispatch = useAppDispatch()

  const [minsideFilter, setMinsideFilter] = useState<Filter>(minOppgavelisteFiltre())
  const [hovedsideFilter, setHovedsideFilter] = useState<Filter>(hentFilterFraLocalStorage())

  useEffect(() => {
    leggFilterILocalStorage(hovedsideFilter)
  }, [hovedsideFilter])

  const [minsideOppgaverResult, hentOppgaverMinside] = useApiCall(hentOppgaverMedStatus)
  const [hovedsideOppgaverResult, hentAlleOppgaverStatusFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaverResult, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)

  const hentMinsideOppgaver = (oppgavestatusFilter: Array<string> | undefined) =>
    hentOppgaverMinside({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : minsideFilter.oppgavestatusFilter,
      minOppgavelisteIdent: true,
    })
  const hentHovedsideOppgaver = (oppgavestatusFilter: Array<string> | undefined) =>
    hentAlleOppgaverStatusFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : hovedsideFilter.oppgavestatusFilter,
      minOppgavelisteIdent: false,
    })

  const hentMinsideOppgaverAlle = () => {
    hentMinsideOppgaver(undefined)
    hentGosysOppgaverFunc({})
  }

  const hentHovedsideOppgaverAlle = () => {
    hentMinsideOppgaver(undefined)
    hentGosysOppgaverFunc({})
  }

  const hentAlleOppgaver = () => {
    hentMinsideOppgaver(undefined)
    hentHovedsideOppgaver(undefined)
    hentGosysOppgaverFunc({})
  }

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandlerIdent === innloggetSaksbehandler.ident)
  }

  const [hovedsideOppgaver, setHovedsideOppgaver] = useState<Array<OppgaveDTO>>([])
  const [minsideOppgaver, setMinsideOppgaver] = useState<Array<OppgaveDTO>>([])

  useEffect(() => {
    hentAlleOppgaver()
  }, [])

  useEffect(() => {
    if (isSuccess(hovedsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      const alleOppgaverMerget = sorterOppgaverEtterOpprettet([
        ...hovedsideOppgaverResult.data,
        ...gosysOppgaverResult.data,
      ])
      setHovedsideOppgaver(alleOppgaverMerget)
    } else if (isSuccess(hovedsideOppgaverResult) && !isSuccess(gosysOppgaverResult)) {
      setHovedsideOppgaver(sorterOppgaverEtterOpprettet(hovedsideOppgaverResult.data))
    } else if (!isSuccess(hovedsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      setHovedsideOppgaver(sorterOppgaverEtterOpprettet(gosysOppgaverResult.data))
    }
  }, [hovedsideOppgaverResult, gosysOppgaverResult])

  useEffect(() => {
    if (isSuccess(minsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      const alleOppgaverMerget = sorterOppgaverEtterOpprettet([
        ...minsideOppgaverResult.data,
        ...filtrerKunInnloggetBrukerOppgaver(gosysOppgaverResult.data),
      ])
      setMinsideOppgaver(alleOppgaverMerget)
    } else if (isSuccess(minsideOppgaverResult) && !isSuccess(gosysOppgaverResult)) {
      setMinsideOppgaver(sorterOppgaverEtterOpprettet(minsideOppgaverResult.data))
    } else if (!isSuccess(minsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      setMinsideOppgaver(sorterOppgaverEtterOpprettet(filtrerKunInnloggetBrukerOppgaver(gosysOppgaverResult.data)))
    }
  }, [gosysOppgaverResult, minsideOppgaverResult])

  useEffect(() => {
    dispatch(settHovedOppgavelisteLengde(hovedsideOppgaver.length))
  }, [hovedsideOppgaver])

  useEffect(() => {
    dispatch(settMinOppgavelisteLengde(minsideOppgaver.length))
  }, [minsideOppgaver])

  return {
    hovedsideOppgaver,
    setHovedsideOppgaver,
    minsideOppgaver,
    setMinsideOppgaver,
    setMinsideFilter,
    setHovedsideFilter,
    hentMinsideOppgaverAlle,
    hentHovedsideOppgaverAlle,
    minsideOppgaverResult,
    hovedsideOppgaverResult,
    gosysOppgaverResult,
    hentMinsideOppgaver,
    hentHovedsideOppgaver,
    minsideFilter,
    hovedsideFilter,
  }
}
