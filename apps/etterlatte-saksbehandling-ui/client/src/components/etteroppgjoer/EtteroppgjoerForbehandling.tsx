import { StatusBar } from '~shared/statusbar/Statusbar'
import React, { useEffect } from 'react'
import { Box, HStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { EtteroppgjoerOversikt } from '~components/etteroppgjoer/oversikt/EtteroppgjoerOversikt'
import { EtteroppgjoerBrev } from '~components/etteroppgjoer/brev/EtteroppgjoerBrev'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import {
  EtteroppjoerForbehandlingSteg,
  EtteroppjoerSteg,
} from '~components/etteroppgjoer/stegmeny/EtteroppjoerForbehandlingSteg'
import { EtteroppjoerSidemeny } from '~components/etteroppgjoer/EtteroppgjoerSidemeny'

export function EtteroppgjoerForbehandling() {
  const { etteroppgjoerId } = useParams()

  const dispatch = useAppDispatch()
  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoer)
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
      <EtteroppjoerForbehandlingSteg />

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
              <Route path={EtteroppjoerSteg.OVERSIKT} element={<EtteroppgjoerOversikt />} />
              <Route path={EtteroppjoerSteg.OPPSUMMERING_OG_BREV} element={<EtteroppgjoerBrev />} />
              <Route
                path="*"
                element={
                  <Navigate
                    to={`/etteroppgjoer/${etteroppgjoerReducer.etteroppgjoer.behandling.id}/${EtteroppjoerSteg.OVERSIKT}`}
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
