import { useEffect } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { GridContainer, MainContent } from '~shared/styled'
import { setBehandling, resetBehandling, IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { PdlPersonStatusBar } from '~shared/statusbar/Statusbar'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from './StegMeny/stegmeny'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useBehandling } from '~components/behandling/useBehandling'
import { BehandlingSidemeny } from '~components/behandling/sidemeny/BehandlingSidemeny'
import Spinner from '~shared/Spinner'
import { hentPersonopplysningerForBehandling } from '~shared/api/grunnlag'
import { resetPersonopplysninger, setPersonopplysninger } from '~store/reducers/PersonopplysningerReducer'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { mapAllApiResult } from '~shared/api/apiUtils'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { StickyToppMeny } from '~shared/StickyToppMeny'
import { IPdlPerson, IPdlPersonNavnFoedsel } from '~shared/types/Person'

export const Behandling = () => {
  useSidetittel('Behandling')

  const behandlingFraRedux = useBehandling()
  const dispatch = useAppDispatch()
  const { behandlingId: behandlingIdFraURL } = useParams()
  const { behandlingRoutes } = useBehandlingRoutes()
  const [fetchBehandlingStatus, fetchBehandling] = useApiCall(hentBehandling)
  const [, fetchPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)
  const soeker = usePersonopplysninger()?.soeker?.opplysning

  useEffect(() => {
    if (!behandlingIdFraURL) {
      return
    }

    if (behandlingIdFraURL !== behandlingFraRedux?.id) {
      fetchBehandling(
        behandlingIdFraURL,
        (behandling) => dispatch(setBehandling(behandling)),
        () => dispatch(resetBehandling())
      )
    }
  }, [behandlingIdFraURL, behandlingFraRedux?.id])

  useEffect(() => {
    if (behandlingFraRedux !== null) {
      fetchPersonopplysninger(
        { behandlingId: behandlingFraRedux.id, sakType: behandlingFraRedux.sakType },
        (result) => dispatch(setPersonopplysninger(result)),
        () => dispatch(resetPersonopplysninger())
      )
    } else {
      dispatch(resetPersonopplysninger())
    }
  }, [behandlingFraRedux])

  return mapAllApiResult(
    fetchBehandlingStatus,
    <Spinner label="Henter behandling ..." visible />,
    null,
    () => <ApiErrorAlert>Kunne ikke hente behandling</ApiErrorAlert>,
    () => {
      if (behandlingFraRedux) {
        const behandling = behandlingFraRedux as IBehandlingReducer
        return (
          <>
            <StickyToppMeny>
              {soeker && <PdlPersonStatusBar person={personTilPersonNavnFoedselsAar(soeker)} />}
              <StegMeny behandling={behandling} />
            </StickyToppMeny>
            <GridContainer>
              <MainContent>
                <Routes>
                  {behandlingRoutes.map((route) => (
                    <Route key={route.path} path={route.path} element={route.element} />
                  ))}
                  <Route path="*" element={<Navigate to={behandlingRoutes[0].path} replace />} />
                </Routes>
              </MainContent>
              <BehandlingSidemeny behandling={behandling} />
            </GridContainer>
          </>
        )
      }
      return null
    }
  )
}

const personTilPersonNavnFoedselsAar = (person: IPdlPerson): IPdlPersonNavnFoedsel => {
  return {
    foedselsnummer: person.foedselsnummer,
    fornavn: person.fornavn,
    mellomnavn: person.mellomnavn,
    etternavn: person.etternavn,
    foedselsaar: person.foedselsaar,
    foedselsdato: person.foedselsdato,
  }
}
