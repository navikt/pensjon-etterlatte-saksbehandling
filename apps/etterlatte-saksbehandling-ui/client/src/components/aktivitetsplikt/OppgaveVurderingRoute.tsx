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
import { VurderingInfoBrevOgOppsummering } from '~components/aktivitetsplikt/brev/VurderingInfoBrevOgOppsummering'
import { AktivitetspliktOppgaveVurdering } from '~shared/types/Aktivitetsplikt'

interface AktivitetspliktOppgaveVurderingProvider extends AktivitetspliktOppgaveVurdering {
  oppdater: () => void
}

const AktivitetspliktOppgaveContext = createContext<AktivitetspliktOppgaveVurderingProvider>(
  {} as AktivitetspliktOppgaveVurderingProvider
)

export function OppgaveVurderingRoute(props: {
  vurderingOgOppgave: AktivitetspliktOppgaveVurdering
  fetchOppgave: () => void
}) {
  const { vurderingOgOppgave, fetchOppgave } = props

  return (
    <AktivitetspliktOppgaveContext.Provider value={{ ...vurderingOgOppgave, oppdater: fetchOppgave }}>
      <StatusBar ident={vurderingOgOppgave.oppgave.fnr} />
      <AktivitetspliktStegmeny />

      <GridContainer>
        <MainContent>
          <Routes>
            <Route path={AktivitetspliktSteg.VURDERING} element={<VurderAktivitet />} />
            <Route path={AktivitetspliktSteg.OPPSUMMERING_OG_BREV} element={<VurderingInfoBrevOgOppsummering />} />
            <Route path="*" element={<Navigate to={AktivitetspliktSteg.VURDERING} replace />} />
          </Routes>
        </MainContent>
        <AktivitetspliktSidemeny />
      </GridContainer>
    </AktivitetspliktOppgaveContext.Provider>
  )
}

export const useAktivitetspliktOppgaveVurdering = (): AktivitetspliktOppgaveVurderingProvider => {
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
