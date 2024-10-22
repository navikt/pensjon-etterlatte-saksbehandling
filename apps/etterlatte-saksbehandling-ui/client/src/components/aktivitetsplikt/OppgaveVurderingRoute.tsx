import { OppgaveDTO } from '~shared/types/oppgave'
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

const AktivitetspliktOppgaveContext = createContext<OppgaveDTO>({} as OppgaveDTO)

export function OppgaveVurderingRoute(props: { oppgave: OppgaveDTO }) {
  const { oppgave } = props
  return (
    <AktivitetspliktOppgaveContext.Provider value={oppgave}>
      <StatusBar ident={oppgave.fnr} />
      <AktivitetspliktStegmeny />

      <GridContainer>
        <MainContent>
          <Routes>
            <Route path={AktivitetspliktSteg.VURDERING} element={<VurderAktivitet />} />
            <Route path={AktivitetspliktSteg.BREV} element={<VurderingInfoBrev />} />
            <Route path="*" element={<Navigate to={AktivitetspliktSteg.VURDERING} replace />} />
          </Routes>
        </MainContent>
        <AktivitetspliktSidemeny />
      </GridContainer>
    </AktivitetspliktOppgaveContext.Provider>
  )
}

export const useOppgaveForVurdering = (): OppgaveDTO => {
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
