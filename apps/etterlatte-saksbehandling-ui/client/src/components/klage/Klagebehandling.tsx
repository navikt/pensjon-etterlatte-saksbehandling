import { useKlage } from '~components/klage/useKlage'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import React, { useEffect } from 'react'
import { useAppDispatch } from '~store/Store'
import { addKlage, resetKlage } from '~store/reducers/KlageReducer'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { getPerson } from '~shared/api/grunnlag'
import { StatusBar } from '~shared/statusbar/Statusbar'
import Spinner from '~shared/Spinner'
import { GridContainer, MainContent } from '~shared/styled'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentKlage } from '~shared/api/klage'
import { KlageStegmeny } from '~components/klage/stegmeny/KlageStegmeny'
import { KlageFormkrav } from '~components/klage/formkrav/KlageFormkrav'
import { KlageVurdering } from '~components/klage/vurdering/KlageVurdering'
import { KlageOppsummering } from '~components/klage/oppsummering/KlageOppsummering'
import { KlageSidemeny } from '~components/klage/sidemeny/KlageSidemeny'
import { KlageBrev } from '~components/klage/brev/KlageBrev'

export function Klagebehandling() {
  const klage = useKlage()
  const match = useMatch('/klage/:klageId/*')
  const dispatch = useAppDispatch()
  const [fetchKlageStatus, fetchKlage] = useApiCall(hentKlage)
  const [personStatus, hentPerson] = useApiCall(getPerson)

  const klageIdFraUrl = match?.params.klageId
  const viHarLastetRiktigKlage = klageIdFraUrl === klage?.id

  useEffect(() => {
    if (!klageIdFraUrl) return

    if (viHarLastetRiktigKlage) {
      return
    }
    fetchKlage(
      klageIdFraUrl,
      (hentetKlage) => {
        dispatch(addKlage(hentetKlage))
      },
      () => dispatch(resetKlage())
    )
  }, [klageIdFraUrl, viHarLastetRiktigKlage])

  useEffect(() => {
    if (klage?.sak.ident) {
      hentPerson(klage.sak.ident)
    }
  }, [klage?.sak])

  return (
    <>
      <StatusBar result={personStatus} />
      <KlageStegmeny />
      {isPending(fetchKlageStatus) && <Spinner visible label="Henter klagebehandling" />}

      {klage !== null && viHarLastetRiktigKlage && (
        <GridContainer>
          <MainContent>
            <Routes>
              <Route path="formkrav" element={<KlageFormkrav />} />
              <Route path="vurdering" element={<KlageVurdering />} />
              <Route path="brev" element={<KlageBrev />} />
              <Route path="oppsummering" element={<KlageOppsummering />} />
              <Route path="*" element={<Navigate to="formkrav" replace />} />
            </Routes>
          </MainContent>
          <KlageSidemeny />
        </GridContainer>
      )}

      {isFailure(fetchKlageStatus) && <ApiErrorAlert>Kunne ikke hente klagebehandling</ApiErrorAlert>}
    </>
  )
}
