import { useEffect } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { hentBehandling } from '~shared/api/behandling'
import { IBehandlingReducer, resetBehandling, setBehandling } from '~store/reducers/BehandlingReducer'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { BehandlingRouteContext, useBehandlingRoutes } from './BehandlingRoutes'
import { StegMeny } from '~components/behandling/stegmeny/StegMeny'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useBehandling } from '~components/behandling/useBehandling'
import { BehandlingSidemeny } from '~components/behandling/sidemeny/BehandlingSidemeny'
import Spinner from '~shared/Spinner'
import { hentPersonopplysningerForBehandling } from '~shared/api/grunnlag'
import { resetPersonopplysninger, setPersonopplysninger } from '~store/reducers/PersonopplysningerReducer'
import { mapResult } from '~shared/api/apiUtils'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { StickyToppMeny } from '~shared/StickyToppMeny'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { Box, HStack, Modal } from '@navikt/ds-react'
import { usePerson } from '~shared/statusbar/usePerson'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { addEtteroppgjoer, resetEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

export const Behandling = () => {
  useSidetittel('Behandling')

  const behandlingFraRedux = useBehandling()
  const dispatch = useAppDispatch()
  const { behandlingId: behandlingIdFraURL } = useParams()
  const routedata = useBehandlingRoutes()
  const [fetchBehandlingStatus, fetchBehandling] = useApiCall(hentBehandling)
  const [, fetchPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)
  const [, fetchForbehandling] = useApiCall(hentEtteroppgjoerForbehandling)
  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const person = usePerson()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

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
    if (
      behandlingFraRedux?.revurderingsaarsak === Revurderingaarsak.ETTEROPPGJOER &&
      !!behandlingFraRedux?.relatertBehandlingId
    ) {
      fetchForbehandling(
        behandlingFraRedux?.relatertBehandlingId,
        (result) => dispatch(addEtteroppgjoer(result)),
        () => dispatch(resetEtteroppgjoer())
      )
    } else {
      dispatch(resetEtteroppgjoer())
    }
  }, [behandlingFraRedux])

  const endretFnr = soeker !== undefined && person !== null && soeker.foedselsnummer !== person.foedselsnummer

  return mapResult(fetchBehandlingStatus, {
    pending: <Spinner label="Henter behandling ..." />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente behandling: {error.detail}</ApiErrorAlert>,
    success: () => {
      if (behandlingFraRedux) {
        const behandling = behandlingFraRedux as IBehandlingReducer
        const redigerbar = behandlingErRedigerbar(
          behandling.status,
          behandling.sakEnhetId,
          innloggetSaksbehandler.skriveEnheter
        )
        return (
          <BehandlingRouteContext.Provider value={routedata}>
            <StickyToppMeny>
              <StatusBar ident={soeker?.foedselsnummer} />
              <StegMeny behandling={behandling} />
            </StickyToppMeny>
            {redigerbar && soeker && (
              <Modal
                header={{
                  heading: 'Nytt identnummer på bruker',
                  closeButton: false,
                }}
                open={endretFnr}
                onClose={() => {}}
              >
                <Modal.Body>
                  Nytt identnummer har blitt registrert for brukeren, behandlingen må derfor avbrytes. Når du kobler til
                  den nye identen, vil behandlingen bli slettet, og du må opprette en ny.
                </Modal.Body>
                <Modal.Footer>
                  <PersonButtonLink fnr={soeker.foedselsnummer} fane={PersonOversiktFane.SAKER}>
                    Gå til sakoversikt for å koble ny ident
                  </PersonButtonLink>
                </Modal.Footer>
              </Modal>
            )}
            <HStack height="100%" minHeight="100vh" wrap={false}>
              <Box width="100%">
                <Routes>
                  {routedata.behandlingRoutes.map((route) => (
                    <Route key={route.path} path={route.path} element={route.element(behandling)} />
                  ))}
                  <Route
                    path="*"
                    element={
                      <Navigate to={`/behandling/${behandling.id}/${routedata.behandlingRoutes[0].path}`} replace />
                    }
                  />
                </Routes>
              </Box>
              <BehandlingSidemeny behandling={behandling} />
            </HStack>
          </BehandlingRouteContext.Provider>
        )
      }
      return null
    },
  })
}
