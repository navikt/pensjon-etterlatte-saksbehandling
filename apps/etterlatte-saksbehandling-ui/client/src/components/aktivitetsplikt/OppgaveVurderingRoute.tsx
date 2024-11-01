import { StatusBar } from '~shared/statusbar/Statusbar'
import { GridContainer, MainContent } from '~shared/styled'
import { Navigate, Route, Routes } from 'react-router-dom'
import React, { createContext, useContext } from 'react'
import { AktivitetspliktSidemeny } from '~components/aktivitetsplikt/sidemeny/AktivitetspliktSidemeny'
import {
  AktivitetspliktSteg,
  AktivitetspliktStegmeny,
} from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { VurderAktivitet } from '~components/aktivitetsplikt/vurdering/VurderAktivitet'
import { VurderingInfoBrev } from '~components/aktivitetsplikt/brev/VurderingInfoBrev'
import { AktivitetspliktOppgaveVurdering } from '~shared/types/Aktivitetsplikt'

const AktivitetspliktOppgaveContext = createContext<AktivitetspliktOppgaveVurdering>(
  {} as AktivitetspliktOppgaveVurdering
)

export function OppgaveVurderingRoute(props: {
  vurderingOgOppgave: AktivitetspliktOppgaveVurdering
  fetchOppgave: () => void
}) {
  const { vurderingOgOppgave, fetchOppgave } = props
  return (
    <AktivitetspliktOppgaveContext.Provider value={vurderingOgOppgave}>
      <StatusBar ident={vurderingOgOppgave.oppgave.fnr} />
      <AktivitetspliktStegmeny />

      <GridContainer>
        <MainContent>
          <Routes>
            <Route path={AktivitetspliktSteg.VURDERING} element={<VurderAktivitet fetchOppgave={fetchOppgave} />} />
            <Route path={AktivitetspliktSteg.BREV} element={<VurderingInfoBrev />} />
            <Route path="*" element={<Navigate to={AktivitetspliktSteg.VURDERING} replace />} />
          </Routes>
        </MainContent>
        <AktivitetspliktSidemeny />
      </GridContainer>
    </AktivitetspliktOppgaveContext.Provider>
  )
}

export const useAktivitetspliktOppgaveVurdering = (): AktivitetspliktOppgaveVurdering => {
  try {
    const oppgave = useContext(AktivitetspliktOppgaveContext)
    if (!oppgave) {
      throw new Error('Oppgave er ikke definert')
    }
    return oppgave
  } catch (e) {
    const msg = 'Kan ikke bruke useOppgaveForVurdering utenfor OppgaveVurderingRoute-treet'
    console.error(msg)
    throw new Error(msg, { cause: e })
  }
}
