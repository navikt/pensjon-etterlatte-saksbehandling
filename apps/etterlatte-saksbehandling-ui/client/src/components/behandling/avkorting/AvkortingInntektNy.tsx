import { Alert, Button, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { IAvkortingNy } from '~shared/types/IAvkorting'
import { ChevronDownIcon, ChevronUpIcon } from '@navikt/aksel-icons'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { AvkortingInntektTabell } from '~components/behandling/avkorting/AvkortingInntektTabell'
import { AvkortingInntektFormNy } from '~components/behandling/avkorting/AvkortingInntektFormNy'

export const AvkortingInntektNy = ({
  behandling,
  avkorting,
  skalHaInntektNesteAar,
  redigerbar,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  avkorting: IAvkortingNy
  skalHaInntektNesteAar: boolean
  redigerbar: boolean
  resetInntektsavkortingValidering: () => void
}) => {
  const [visHistorikk, setVisHistorikk] = useState(false)
  const erFoerstegangsbehandling = behandling.revurderingsaarsak == null
  console.log(erFoerstegangsbehandling, behandling.revurderingsaarsak)

  // TODO sjekke begge/alle?
  const personopplysning = usePersonopplysninger()
  const fyller67 =
    personopplysning != null &&
    personopplysning.soeker != undefined &&
    avkorting?.redigerbarForventetInntekt != undefined &&
    new Date(avkorting.redigerbarForventetInntekt.fom).getFullYear() -
      personopplysning.soeker.opplysning.foedselsaar ===
      67

  const listeVisningAvkortingGrunnlag = () => {
    // TODO hvis historikk=false men inntekt neste år? Vis alle fra og med nyligster i virk år?

    if (erFoerstegangsbehandling || visHistorikk) {
      return avkorting.avkortingGrunnlag
    } else {
      return [avkorting.redigerbarForventetInntekt ?? [], avkorting.redigerbarForventetInntektNesteAar ?? []].flat()
    }
  }

  return (
    <VStack maxWidth="70rem">
      {avkorting.avkortingGrunnlag.length > 0 && (
        <VStack marginBlock="4">
          {fyller67 && (
            <Alert variant="warning">
              Bruker fyller 67 år i inntektsåret og antall innvilga måneder vil bli tilpasset deretter.
            </Alert>
          )}
          <AvkortingInntektTabell avkortingGrunnlagListe={listeVisningAvkortingGrunnlag()} fyller67={fyller67} />
        </VStack>
      )}
      {!erFoerstegangsbehandling && avkorting.avkortingGrunnlag.length > 1 && (
        <Button variant="tertiary" onClick={() => setVisHistorikk(!visHistorikk)}>
          Historikk{' '}
          {visHistorikk ? <ChevronUpIcon className="dropdownIcon" /> : <ChevronDownIcon className="dropdownIcon" />}
        </Button>
      )}
      <AvkortingInntektFormNy
        behandling={behandling}
        redigerbartGrunnlag={avkorting.redigerbarForventetInntekt}
        historikk={avkorting.avkortingGrunnlag}
        erInnevaerendeAar={true}
        redigerbar={redigerbar}
        resetInntektsavkortingValidering={resetInntektsavkortingValidering}
      />
      {skalHaInntektNesteAar && (
        <AvkortingInntektFormNy
          behandling={behandling}
          redigerbartGrunnlag={avkorting.redigerbarForventetInntektNesteAar}
          historikk={avkorting.avkortingGrunnlag}
          erInnevaerendeAar={false}
          redigerbar={redigerbar}
          resetInntektsavkortingValidering={resetInntektsavkortingValidering}
        />
      )}
    </VStack>
  )
}
