import { useKlage, useKlageRedigerbar } from '~components/klage/useKlage'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import React, { useEffect } from 'react'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { addKlage, resetKlage } from '~store/reducers/KlageReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getPerson } from '~shared/api/grunnlag'
import { StatusBar } from '~shared/statusbar/Statusbar'
import Spinner from '~shared/Spinner'
import { GridContainer, MainContent } from '~shared/styled'
import { hentKlage } from '~shared/api/klage'
import { KlageStegmeny } from '~components/klage/stegmeny/KlageStegmeny'
import { KlageOppsummering } from '~components/klage/oppsummering/KlageOppsummering'
import { KlageSidemeny } from '~components/klage/sidemeny/KlageSidemeny'
import { KlageBrev } from '~components/klage/brev/KlageBrev'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { KlageFormkrav } from '~components/klage/formkrav/KlageFormkrav'
import { KlageVurdering } from '~components/klage/vurdering/KlageVurdering'

export function Klagebehandling() {
  const klage = useKlage()
  const match = useMatch('/klage/:klageId/*')
  const dispatch = useAppDispatch()
  const [fetchKlageStatus, fetchKlage] = useApiCall(hentKlage)
  const [personStatus, hentPerson] = useApiCall(getPerson)

  const klageIdFraUrl = match?.params.klageId
  const viHarLastetRiktigKlage = klageIdFraUrl === klage?.id

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const kanRedigere = (useKlageRedigerbar() && innloggetSaksbehandler.skriveTilgang) ?? false

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
              <Route path="formkrav" element={<KlageFormkrav kanRedigere={kanRedigere} />} />
              <Route path="vurdering" element={<KlageVurdering kanRedigere={kanRedigere} />} />
              <Route path="brev" element={<KlageBrev />} />
              <Route path="oppsummering" element={<KlageOppsummering />} />
              <Route path="*" element={<Navigate to="formkrav" replace />} />
            </Routes>
          </MainContent>
          <KlageSidemeny />
        </GridContainer>
      )}

      {isFailureHandler({
        apiResult: fetchKlageStatus,
        errorMessage: 'Kunne ikke hente klagebehandling',
      })}
    </>
  )
}
