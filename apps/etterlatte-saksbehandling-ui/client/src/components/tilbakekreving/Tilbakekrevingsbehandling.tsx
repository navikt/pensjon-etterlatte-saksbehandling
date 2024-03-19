import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import React, { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getPerson } from '~shared/api/grunnlag'
import { StatusBar } from '~shared/statusbar/Statusbar'
import Spinner from '~shared/Spinner'
import { GridContainer, MainContent } from '~shared/styled'
import { hentTilbakekreving } from '~shared/api/tilbakekreving'
import { addTilbakekreving, resetTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { TilbakekrevingVurdering } from '~components/tilbakekreving/vurdering/TilbakekrevingVurdering'
import { TilbakekrevingStegmeny } from '~components/tilbakekreving/stegmeny/TilbakekrevingStegmeny'
import { TilbakekrevingSidemeny } from '~components/tilbakekreving/sidemeny/TilbakekrevingSidemeny'
import { TilbakekrevingBrev } from '~components/tilbakekreving/brev/TilbakekrevingBrev'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { erUnderBehandling } from '~shared/types/Tilbakekreving'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { TilbakekrevingUtbetalinger } from '~components/tilbakekreving/utbetalinger/TilbakekrevingUtbetalinger'
import { TilbakekrevingOppsummering } from '~components/tilbakekreving/oppsummering/TilbakekrevingOppsummering'

export function Tilbakekrevingsbehandling() {
  useSidetittel('Tilbakekreving')

  const tilbakekreving = useTilbakekreving()
  const dispatch = useAppDispatch()
  const { tilbakekrevingId } = useParams()
  const [fetchTilbakekrevingStatus, fetchTilbakekreving] = useApiCall(hentTilbakekreving)
  const [personStatus, hentPerson] = useApiCall(getPerson)
  const viHarLastetRiktigTilbakekreving = tilbakekrevingId === tilbakekreving?.id.toString()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [redigerbar, setRedigerbar] = useState(false)

  useEffect(() => {
    if (!tilbakekrevingId) return

    fetchTilbakekreving(
      tilbakekrevingId,
      (hentetTilbakekreving) => {
        dispatch(addTilbakekreving(hentetTilbakekreving))
      },
      () => dispatch(resetTilbakekreving())
    )
  }, [tilbakekrevingId])

  useEffect(() => {
    if (tilbakekreving?.sak.ident) {
      hentPerson(tilbakekreving.sak.ident)
    }

    if (tilbakekreving?.sak?.enhet) {
      setRedigerbar(
        enhetErSkrivbar(tilbakekreving.sak.enhet, innloggetSaksbehandler.skriveEnheter) &&
          erUnderBehandling(tilbakekreving.status)
      )
    }
  }, [tilbakekreving?.sak])

  return (
    <>
      <StatusBar result={personStatus} />
      <TilbakekrevingStegmeny />
      <Spinner visible={isPending(fetchTilbakekrevingStatus)} label="Henter tilbakekrevingsbehandling" />

      {!!tilbakekreving && viHarLastetRiktigTilbakekreving && (
        <GridContainer>
          <MainContent>
            <Routes>
              <Route
                path="vurdering"
                element={<TilbakekrevingVurdering behandling={tilbakekreving} redigerbar={redigerbar} />}
              />
              <Route
                path="utbetalinger"
                element={<TilbakekrevingUtbetalinger behandling={tilbakekreving} redigerbar={redigerbar} />}
              />
              <Route path="oppsummering" element={<TilbakekrevingOppsummering behandling={tilbakekreving} />} />
              <Route path="brev" element={<TilbakekrevingBrev behandling={tilbakekreving} redigerbar={redigerbar} />} />
              <Route path="*" element={<Navigate to="vurdering" replace />} />
            </Routes>
          </MainContent>
          <TilbakekrevingSidemeny />
        </GridContainer>
      )}

      {isFailureHandler({
        apiResult: fetchTilbakekrevingStatus,
        errorMessage: 'Kunne ikke hente tilbakekrevingsbehandling',
      })}
    </>
  )
}
