import { Alert, BodyShort, Button, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { AvkortingInntektTabell } from '~components/behandling/avkorting/AvkortingInntektTabell'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { useBehandling } from '~components/behandling/useBehandling'
import { useAppSelector } from '~store/Store'
import { ApiErrorAlert } from '~ErrorBoundary'
import { aarFraDatoString } from '~utils/formatering/dato'
import { PencilIcon } from '@navikt/aksel-icons'
import { IAvkorting, IAvkortingGrunnlag, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'
import { Virkningstidspunkt, virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentManglendeInntektsaar } from '~shared/api/avkorting'
import { mapResult, mapSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { AvkortingInntektForm } from '~components/behandling/avkorting/AvkortingInntektForm'
import { ogSeparertListe } from '../felles/utils'

function tomInntektForRedigering(aar: number, virkAar: number, virk: Virkningstidspunkt): IAvkortingGrunnlagLagre {
  return {
    spesifikasjon: '',
    fom: aar === virkAar ? virk.dato : `${aar}-01`,
  }
}
function redigerbareInntekterForAvkorting(avkorting?: IAvkorting): IAvkortingGrunnlag[] {
  if (!avkorting) {
    return []
  }
  if (avkorting.redigerbareInntekter) {
    return avkorting.redigerbareInntekter
  }
  return [avkorting.redigerbarForventetInntekt, avkorting.redigerbarForventetInntektNesteAar].filter(
    (inntekt) => !!inntekt
  )
}

export const AvkortingInntekt = ({ redigerbar }: { redigerbar: boolean }) => {
  const behandling = useBehandling()!
  const virk = virkningstidspunkt(behandling)
  const virkAar = aarFraDatoString(virk.dato)
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting)
  const personopplysning = usePersonopplysninger()
  const [inntekterForRedigering, setInntekterForRedigering] = useState<IAvkortingGrunnlagLagre[]>([])
  const [statusHentManglendeInntektsaar, fetchHentManglendeInntektsaar] = useApiCall(hentManglendeInntektsaar)

  useEffect(() => {
    fetchHentManglendeInntektsaar(behandling.id, (paakrevdeAar) => {
      setInntekterForRedigering(paakrevdeAar.map((aar) => tomInntektForRedigering(aar, virkAar, virk)))
    })
  }, [avkorting])

  function vedLagring() {
    fetchHentManglendeInntektsaar(behandling.id)
    setInntekterForRedigering([])
  }

  const paakrevdeAar = mapSuccess(statusHentManglendeInntektsaar, (aar) => aar) ?? []

  const avbrytRedigering = paakrevdeAar.length > 0 ? undefined : () => setInntekterForRedigering([])
  const fyller67 =
    paakrevdeAar.some((aar) => personopplysning?.soeker?.opplysning.foedselsaar === aar) ||
    !!avkorting?.avkortingGrunnlag.some(
      (inntekt) => personopplysning?.soeker?.opplysning?.foedselsaar === aarFraDatoString(inntekt.fom)
    )
  const redigerbareInntekter = redigerbareInntekterForAvkorting(avkorting)

  function redigerEllerOpprettInntekt() {
    if (redigerbareInntekter.length > 0) {
      setInntekterForRedigering(redigerbareInntekter)
    } else {
      setInntekterForRedigering([tomInntektForRedigering(virkAar, virkAar, virk)])
    }
  }

  return (
    <VStack maxWidth="70rem">
      {avkorting && avkorting.avkortingGrunnlag.length > 0 && (
        <VStack marginBlock="4">
          {fyller67 && (
            <Alert variant="warning">
              Bruker fyller 67 år i beregnet periode og antall innvilgede måneder vil bli tilpasset deretter.
            </Alert>
          )}
          <AvkortingInntektTabell
            avkortingGrunnlagListe={avkorting.avkortingGrunnlag}
            fyller67={fyller67}
            erEtteroppgjoerRevurdering={behandling.revurderingsaarsak === Revurderingaarsak.ETTEROPPGJOER}
          />
        </VStack>
      )}
      {mapResult(statusHentManglendeInntektsaar, {
        pending: <Spinner label="Henter påkrevde år for inntektsavkorting" />,
        error: (e) => <ApiErrorAlert>Kunne ikke hente påkrevde år for avkorting: {e.detail}</ApiErrorAlert>,
      })}
      {redigerbar && (
        <>
          {paakrevdeAar.length > 0 && (
            <BodyShort spacing>
              Siden det er beregnet ytelse for år(ene) {ogSeparertListe(paakrevdeAar)} må forventet inntekt for bruker i
              ytelsesperioden registreres.
            </BodyShort>
          )}
          {inntekterForRedigering.length > 0 ? (
            <AvkortingInntektForm
              alleInntektsgrunnlag={avkorting?.avkortingGrunnlag ?? []}
              inntekterForRedigering={inntekterForRedigering}
              vedLagring={vedLagring}
              avbrytRedigering={avbrytRedigering}
            />
          ) : (
            <HStack marginBlock="4 0">
              <Button
                size="small"
                variant="secondary"
                icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
                onClick={redigerEllerOpprettInntekt}
              >
                {redigerbareInntekter.length > 0 ? 'Rediger inntekt' : 'Legg til inntekt'}
              </Button>
            </HStack>
          )}
        </>
      )}
    </VStack>
  )
}
