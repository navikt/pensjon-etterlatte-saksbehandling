import { StatusBar } from '~shared/statusbar/Statusbar'
import React, { useEffect } from 'react'
import { Box, HStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { EtteroppgjoerForbehandlingBrev } from '~components/etteroppgjoer/forbehandling/EtteroppgjoerForbehandlingBrev'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import {
  EtteroppjoerForbehandlingSteg,
  EtteroppjoerForbehandlingStegmeny,
} from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'
import { EtteroppgjoerForbehandlingOversikt } from '~components/etteroppgjoer/forbehandling/EtteroppgjoerForbehandlingOversikt'
import { EtteroppjoerSidemeny } from '~components/etteroppgjoer/forbehandling/sidemeny/EtteroppgjoerForbehandlingSidemeny'

export function EtteroppgjoerForbehandling() {
  const { etteroppgjoerId } = useParams()

  const dispatch = useAppDispatch()
  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)
  const etteroppgjoerReducer = useAppSelector((state) => state.etteroppgjoerReducer)

  useEffect(() => {
    if (!etteroppgjoerId) return

    hentEtteroppgjoerRequest(etteroppgjoerId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerId])

  return (
    <>
      <StatusBar ident={etteroppgjoerReducer.etteroppgjoer?.behandling.sak.ident} />
      <EtteroppjoerForbehandlingStegmeny />

      {mapResult(etteroppgjoerResult, {
        pending: <Spinner label="Henter etteroppgjørbehandling" />,
        error: (error) => (
          <ApiErrorAlert>Kunne ikke hente forbehandlingen for etteroppgjør: {error.detail}</ApiErrorAlert>
        ),
      })}
      {/* Laster kun sidene hvis etteroppgjør finnes i reducer, slik at de kan bruke garantert etteroppgjør */}
      {etteroppgjoerReducer.etteroppgjoer && (
        <HStack height="100%" minHeight="100vh" wrap={false}>
          <Box width="100%">
            <Routes>
              <Route path={EtteroppjoerForbehandlingSteg.OVERSIKT} element={<EtteroppgjoerForbehandlingOversikt />} />
              <Route path={EtteroppjoerForbehandlingSteg.BREV} element={<EtteroppgjoerForbehandlingBrev />} />
              <Route
                path="*"
                element={
                  <Navigate
                    to={`/etteroppgjoer/${etteroppgjoerId}/${EtteroppjoerForbehandlingSteg.OVERSIKT}`}
                    replace
                  />
                }
              />
            </Routes>
          </Box>
          <EtteroppjoerSidemeny />
        </HStack>
      )}
    </>
  )
}
