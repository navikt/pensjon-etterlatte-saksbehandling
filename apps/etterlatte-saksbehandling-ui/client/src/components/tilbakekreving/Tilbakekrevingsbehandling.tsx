import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import React, { useEffect } from 'react'
import { useAppDispatch } from '~store/Store'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { getPerson } from '~shared/api/grunnlag'
import { StatusBar } from '~shared/statusbar/Statusbar'
import Spinner from '~shared/Spinner'
import { GridContainer, MainContent } from '~shared/styled'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentTilbakekreving } from '~shared/api/tilbakekreving'
import { addTilbakekreving, resetTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { TilbakekrevingOversikt } from '~components/tilbakekreving/oversikt/TilbakekrevingOversikt'
import { TilbakekrevingVurdering } from '~components/tilbakekreving/vurdering/TilbakekrevingVurdering'
import { TilbakekrevingVedtak } from '~components/tilbakekreving/vedtak/TilbakekrevingVedtak'
import { TilbakekrevingStegmeny } from '~components/tilbakekreving/stegmeny/TilbakekrevingStegmeny'
import { TilbakekrevingSidemeny } from '~components/tilbakekreving/sidemeny/TilbakekrevingSidemeny'
import { TilbakekrevingBrev } from '~components/tilbakekreving/brev/TilbakekrevingBrev'

export function Tilbakekrevingsbehandling() {
  const tilbakekreving = useTilbakekreving()
  const dispatch = useAppDispatch()
  const { tilbakekrevingId } = useParams()
  const [fetchTilbakekrevingStatus, fetchTilbakekreving] = useApiCall(hentTilbakekreving)
  const [personStatus, hentPerson] = useApiCall(getPerson)
  const viHarLastetRiktigTilbakekreving = tilbakekrevingId === tilbakekreving?.id

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
  }, [tilbakekreving?.sak])

  return (
    <>
      <StatusBar result={personStatus} />
      <Spinner visible={isPending(fetchTilbakekrevingStatus)} label="Henter tilbakekrevingsbehandling" />

      {!!tilbakekreving && viHarLastetRiktigTilbakekreving && (
        <GridContainer>
          <MainContent>
            <TilbakekrevingStegmeny />
            <Routes>
              <Route path="oversikt" element={<TilbakekrevingOversikt />} />
              <Route path="vurdering" element={<TilbakekrevingVurdering />} />
              <Route path="vedtak" element={<TilbakekrevingVedtak />} />
              <Route path="brev" element={<TilbakekrevingBrev />} />
              <Route path="*" element={<Navigate to="oversikt" replace />} />
            </Routes>
          </MainContent>
          <TilbakekrevingSidemeny />
        </GridContainer>
      )}

      {isFailure(fetchTilbakekrevingStatus) && (
        <ApiErrorAlert>Kunne ikke hente tilbakekrevingsbehandling</ApiErrorAlert>
      )}
    </>
  )
}
