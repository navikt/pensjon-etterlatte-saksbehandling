import { useKlage, useKlageRedigerbar } from '~components/klage/useKlage'
import { Navigate, Route, Routes, useMatch } from 'react-router-dom'
import React, { useEffect, useState } from 'react'
import { useAppDispatch } from '~store/Store'
import { addKlage, resetKlage } from '~store/reducers/KlageReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
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
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export function Klagebehandling() {
  useSidetittel('Klage')

  const klage = useKlage()
  const match = useMatch('/klage/:klageId/*')
  const dispatch = useAppDispatch()
  const [fetchKlageStatus, fetchKlage] = useApiCall(hentKlage)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [kanRedigere, setKanRedigere] = useState(false)

  const klageIdFraUrl = match?.params.klageId
  const viHarLastetRiktigKlage = klageIdFraUrl === klage?.id
  const klageRedigerbar = useKlageRedigerbar()

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
    if (klage?.sak.enhet) {
      setKanRedigere(
        (klageRedigerbar && enhetErSkrivbar(klage.sak.enhet, innloggetSaksbehandler.skriveEnheter)) ?? false
      )
    }
  }, [klage?.sak])

  return (
    <>
      <StatusBar ident={klage?.sak.ident} />
      <KlageStegmeny />

      <Spinner visible={isPending(fetchKlageStatus)} label="Henter klagebehandling" />

      {klage !== null && viHarLastetRiktigKlage && (
        <GridContainer>
          <MainContent>
            <Routes>
              <Route path="formkrav" element={<KlageFormkrav kanRedigere={kanRedigere} />} />
              <Route path="vurdering" element={<KlageVurdering kanRedigere={kanRedigere} />} />
              <Route path="brev" element={<KlageBrev />} />
              <Route path="oppsummering" element={<KlageOppsummering kanRedigere={kanRedigere} />} />
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
