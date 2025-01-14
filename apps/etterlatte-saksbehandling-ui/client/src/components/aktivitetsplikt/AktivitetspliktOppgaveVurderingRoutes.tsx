import { StatusBar } from '~shared/statusbar/Statusbar'
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
import { useAktivitetspliktOppgaveVurderingState } from '~store/reducers/AktivitetsplikReducer'
import { ValgForInfobrev } from '~components/aktivitetsplikt/brev/ValgForInfobrev'
import { Box, HStack } from '@navikt/ds-react'

const AktivitetspliktOppgaveContext = createContext<AktivitetspliktOppgaveVurdering>(
  {} as AktivitetspliktOppgaveVurdering
)

export function AktivitetspliktOppgaveVurderingRoutes(props: { vurderingOgOppgave: AktivitetspliktOppgaveVurdering }) {
  const { vurderingOgOppgave } = props

  return (
    <AktivitetspliktOppgaveContext.Provider value={{ ...useAktivitetspliktOppgaveVurderingState() }}>
      <StatusBar ident={vurderingOgOppgave.oppgave.fnr} />
      <AktivitetspliktStegmeny />

      <HStack height="100%" minHeight="100vh" wrap={false}>
        <Box width="100%">
          <Routes>
            <Route path={AktivitetspliktSteg.VURDERING} element={<VurderAktivitet />} />
            <Route path={AktivitetspliktSteg.BREVVALG} element={<ValgForInfobrev />} />
            <Route path={AktivitetspliktSteg.OPPSUMMERING_OG_BREV} element={<VurderingInfoBrevOgOppsummering />} />
            <Route path="*" element={<Navigate to={AktivitetspliktSteg.VURDERING} replace />} />
          </Routes>
        </Box>
        <AktivitetspliktSidemeny />
      </HStack>
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
    const msg = 'Kan ikke bruke useOppgaveForVurdering utenfor AktivitetspliktOppgaveVurderingRoutes-treet'
    console.error(msg)
    throw new Error(msg, { cause: e })
  }
}
