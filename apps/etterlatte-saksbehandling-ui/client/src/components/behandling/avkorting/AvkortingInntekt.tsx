import { Alert, Button, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { IAvkorting } from '~shared/types/IAvkorting'
import { ChevronDownIcon, ChevronUpIcon } from '@navikt/aksel-icons'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { AvkortingInntektTabell } from '~components/behandling/avkorting/AvkortingInntektTabell'
import { AvkortingInntektForm } from '~components/behandling/avkorting/AvkortingInntektForm'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

export const AvkortingInntekt = ({
  behandling,
  avkorting,
  skalHaInntektNesteAar,
  redigerbar,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  avkorting: IAvkorting
  skalHaInntektNesteAar: boolean
  redigerbar: boolean
  resetInntektsavkortingValidering: () => void
}) => {
  const [visHistorikk, setVisHistorikk] = useState(behandling.revurderingsaarsak === Revurderingaarsak.ETTEROPPGJOER)
  const erFoerstegangsbehandling = behandling.revurderingsaarsak == null

  // TODO sjekke begge/alle?
  const personopplysning = usePersonopplysninger()
  const fyller67 =
    personopplysning != null &&
    personopplysning.soeker != undefined &&
    ((avkorting?.redigerbarForventetInntekt != undefined &&
      new Date(avkorting.redigerbarForventetInntekt.fom).getFullYear() -
        personopplysning.soeker.opplysning.foedselsaar ===
        67) ||
      (avkorting?.redigerbarForventetInntektNesteAar != undefined &&
        new Date(avkorting.redigerbarForventetInntektNesteAar.fom).getFullYear() -
          personopplysning.soeker.opplysning.foedselsaar ===
          67))

  const listeVisningAvkortingGrunnlag = () => {
    if (visHistorikk) {
      return avkorting.avkortingGrunnlag
    }
    if (erFoerstegangsbehandling) {
      return [avkorting.redigerbarForventetInntekt ?? [], avkorting.redigerbarForventetInntektNesteAar ?? []].flat()
    } else {
      const siste =
        avkorting.redigerbarForventetInntekt ?? avkorting.avkortingGrunnlag[avkorting.avkortingGrunnlag.length - 1]
      return [siste]
    }
  }

  return (
    <VStack maxWidth="70rem">
      {avkorting && avkorting.avkortingGrunnlag.length > 0 && (
        <VStack marginBlock="4">
          {fyller67 && (
            <Alert variant="warning">
              Bruker fyller 67 år i inntektsåret og antall innvilga måneder vil bli tilpasset deretter.
            </Alert>
          )}
          <AvkortingInntektTabell avkortingGrunnlagListe={listeVisningAvkortingGrunnlag()} fyller67={fyller67} />
        </VStack>
      )}
      {avkorting && !erFoerstegangsbehandling && avkorting.avkortingGrunnlag.length > 1 && (
        <Button variant="tertiary" onClick={() => setVisHistorikk(!visHistorikk)}>
          Historikk{' '}
          {visHistorikk ? <ChevronUpIcon className="dropdownIcon" /> : <ChevronDownIcon className="dropdownIcon" />}
        </Button>
      )}
      <AvkortingInntektForm
        behandling={behandling}
        redigerbartGrunnlag={avkorting?.redigerbarForventetInntekt}
        historikk={avkorting?.avkortingGrunnlag ?? []}
        erInnevaerendeAar={true}
        redigerbar={redigerbar}
        resetInntektsavkortingValidering={resetInntektsavkortingValidering}
      />
      {skalHaInntektNesteAar && (
        <AvkortingInntektForm
          behandling={behandling}
          redigerbartGrunnlag={avkorting?.redigerbarForventetInntektNesteAar}
          historikk={avkorting?.avkortingGrunnlag ?? []}
          erInnevaerendeAar={false}
          redigerbar={redigerbar}
          resetInntektsavkortingValidering={resetInntektsavkortingValidering}
        />
      )}
    </VStack>
  )
}
