import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, hentOppgaverMedStatus, OppgaveDTO } from '~shared/api/oppgaver'
import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { Filter, minOppgavelisteFiltre } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { useEffect, useState } from 'react'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { oppdaterTildeling, sorterOppgaverEtterOpprettet } from '~components/oppgavebenk/oppgaveutils'
import styled from 'styled-components'
import { useAppDispatch, useAppSelector } from '~store/Store'
import Spinner from '~shared/Spinner'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { settMinOppgavelisteLengde } from '~store/reducers/OppgavelisteReducer'

export const MinOppgaveliste = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const dispatch = useAppDispatch()

  const [filter, setFilter] = useState<Filter>(minOppgavelisteFiltre())
  const [hentedeOppgaver, setHentedeOppgaver] = useState<OppgaveDTO[]>([])

  const [oppgaver, hentOppgaverStatusFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaver, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)

  const hentAlleOppgaver = () => {
    hentOppgaverStatusFetch({ oppgavestatusFilter: filter.oppgavestatusFilter, minOppgavelisteIdent: true })
    hentGosysOppgaverFunc({})
  }

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandlerIdent === innloggetSaksbehandler.ident)
  }

  useEffect(() => hentAlleOppgaver(), [])
  useEffect(() => {
    if (isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      const alleOppgaver = sorterOppgaverEtterOpprettet([
        ...oppgaver.data,
        ...filtrerKunInnloggetBrukerOppgaver(gosysOppgaver.data),
      ])
      setHentedeOppgaver(alleOppgaver)
    } else if (isSuccess(oppgaver) && !isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(sorterOppgaverEtterOpprettet(oppgaver.data))
    } else if (!isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(sorterOppgaverEtterOpprettet(filtrerKunInnloggetBrukerOppgaver(gosysOppgaver.data)))
    }
  }, [oppgaver, gosysOppgaver])

  useEffect(() => {
    dispatch(settMinOppgavelisteLengde(hentedeOppgaver.length))
  }, [hentedeOppgaver])

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
          <ValgWrapper>
            <VelgOppgavestatuser
              value={filter.oppgavestatusFilter}
              onChange={(oppgavestatusFilter) => {
                hentOppgaverStatusFetch({
                  oppgavestatusFilter: oppgavestatusFilter,
                  minOppgavelisteIdent: true,
                })
                setFilter({ ...filter, oppgavestatusFilter })
              }}
            />
          </ValgWrapper>
          <Oppgavelista
            oppgaver={hentedeOppgaver}
            hentOppgaver={hentAlleOppgaver}
            filter={filter}
            setFilter={setFilter}
            oppdaterTildeling={(id, _saksbehandler, versjon) =>
              oppdaterTildeling(setHentedeOppgaver, hentedeOppgaver)(id, null, versjon)
            }
            erMinOppgaveliste={true}
          />
        </>
      )}
    </>
  )
}

const ValgWrapper = styled.div`
  margin-bottom: 2rem;
  width: 35rem;
`
