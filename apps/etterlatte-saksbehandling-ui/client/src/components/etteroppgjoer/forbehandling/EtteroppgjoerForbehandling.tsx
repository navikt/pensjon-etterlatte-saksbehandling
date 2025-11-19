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
import { useAppDispatch } from '~store/Store'
import {
  addDetaljertEtteroppgjoerForbehandling,
  useEtteroppgjoerForbehandling,
} from '~store/reducers/EtteroppgjoerReducer'
import {
  EtteroppjoerForbehandlingSteg,
  EtteroppjoerForbehandlingStegmeny,
} from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'
import { EtteroppgjoerForbehandlingOversikt } from '~components/etteroppgjoer/forbehandling/EtteroppgjoerForbehandlingOversikt'
import { EtteroppjoerSidemeny } from '~components/etteroppgjoer/forbehandling/sidemeny/EtteroppgjoerForbehandlingSidemeny'

export function EtteroppgjoerForbehandling() {
  const { forbehandlingId } = useParams()

  const dispatch = useAppDispatch()
  const [henteEtteroppgjoerForbehandlingResult, hentEtteroppgjoerForbehandlingRequest] =
    useApiCall(hentEtteroppgjoerForbehandling)
  // const { etteroppgjoerForbehandling } = useAppSelector((state) => state.etteroppgjoerReducer)

  const etteroppgjoerForbehandling = useEtteroppgjoerForbehandling()

  useEffect(() => {
    if (!forbehandlingId) return

    hentEtteroppgjoerForbehandlingRequest(forbehandlingId, (etteroppgjoerForbehandling) => {
      dispatch(addDetaljertEtteroppgjoerForbehandling(etteroppgjoerForbehandling))
    })
  }, [forbehandlingId])

  console.log(etteroppgjoerForbehandling)

  return (
    <>
      <StatusBar ident={etteroppgjoerForbehandling?.forbehandling?.sak.ident} />
      <EtteroppjoerForbehandlingStegmeny />

      {mapResult(henteEtteroppgjoerForbehandlingResult, {
        pending: <Spinner label="Henter etteroppgjørbehandling" />,
        error: (error) => (
          <ApiErrorAlert>Kunne ikke hente forbehandlingen for etteroppgjør: {error.detail}</ApiErrorAlert>
        ),
      })}
      {/* Laster kun sidene hvis etteroppgjør finnes i reducer, slik at de kan bruke garantert etteroppgjør */}
      {!!etteroppgjoerForbehandling && (
        <HStack height="100%" minHeight="100vh" wrap={false}>
          <Box width="100%">
            <Routes>
              <Route path={EtteroppjoerForbehandlingSteg.OVERSIKT} element={<EtteroppgjoerForbehandlingOversikt />} />
              <Route path={EtteroppjoerForbehandlingSteg.BREV} element={<EtteroppgjoerForbehandlingBrev />} />
              <Route
                path="*"
                element={
                  <Navigate
                    to={`/etteroppgjoer/${forbehandlingId}/${EtteroppjoerForbehandlingSteg.OVERSIKT}`}
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
