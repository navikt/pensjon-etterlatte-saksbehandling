import { Alert, VStack } from '@navikt/ds-react'
import React from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { IAvkorting } from '~shared/types/IAvkorting'
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

  return (
    <VStack maxWidth="70rem">
      {avkorting && avkorting.avkortingGrunnlag.length > 0 && (
        <VStack marginBlock="4">
          {fyller67 && (
            <Alert variant="warning">
              Bruker fyller 67 år i inntektsåret og antall innvilga måneder vil bli tilpasset deretter.
            </Alert>
          )}
          <AvkortingInntektTabell
            avkortingGrunnlagListe={avkorting.avkortingGrunnlag}
            fyller67={fyller67}
            erEtteroppgjoerRevurdering={behandling.revurderingsaarsak === Revurderingaarsak.ETTEROPPGJOER}
          />
        </VStack>
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
