import { Alert, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { AvkortingInntektForm } from '~components/behandling/avkorting/AvkortingInntektForm'
import { IAvkortingGrunnlagFrontend } from '~shared/types/IAvkorting'
import { ChevronDownIcon, ChevronUpIcon, PencilIcon } from '@navikt/aksel-icons'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { AvkortingInntektTabell } from '~components/behandling/avkorting/AvkortingInntektTabell'

export const AvkortingInntekt = ({
  behandling,
  avkortingGrunnlagFrontend,
  erInnevaerendeAar,
  redigerbar,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  avkortingGrunnlagFrontend: IAvkortingGrunnlagFrontend | undefined
  erInnevaerendeAar: boolean
  redigerbar: boolean
  resetInntektsavkortingValidering: () => void
}) => {
  const erRedigerbar = redigerbar && enhetErSkrivbar(behandling.sakEnhetId, useInnloggetSaksbehandler().skriveEnheter)
  const [visForm, setVisForm] = useState(false)
  const [visHistorikk, setVisHistorikk] = useState(false)

  const personopplysning = usePersonopplysninger()
  const fyller67 =
    personopplysning != null &&
    personopplysning.soeker != undefined &&
    avkortingGrunnlagFrontend != undefined &&
    avkortingGrunnlagFrontend.aar - personopplysning.soeker.opplysning.foedselsaar === 67

  const listeVisningAvkortingGrunnlag = () => {
    if (avkortingGrunnlagFrontend === undefined) {
      return []
    }
    if (visHistorikk) {
      return avkortingGrunnlagFrontend.fraVirk
        ? [avkortingGrunnlagFrontend.fraVirk].concat(avkortingGrunnlagFrontend.historikk)
        : avkortingGrunnlagFrontend.historikk
    } else {
      return [avkortingGrunnlagFrontend.fraVirk ?? avkortingGrunnlagFrontend.historikk[0]]
    }
  }

  const knappTekst = () => {
    if (erInnevaerendeAar) {
      if (avkortingGrunnlagFrontend?.fraVirk != null) {
        return 'Rediger'
      }
      return 'Legg til'
    } else {
      if (!!avkortingGrunnlagFrontend?.historikk?.length) {
        return 'Rediger'
      }
      return 'Legg til for neste år'
    }
  }

  return (
    <VStack maxWidth="70rem">
      {avkortingGrunnlagFrontend &&
        (avkortingGrunnlagFrontend.fraVirk || avkortingGrunnlagFrontend.historikk.length > 0) && (
          <VStack marginBlock="4">
            <Heading size="small">{avkortingGrunnlagFrontend.aar}</Heading>
            {fyller67 && (
              <Alert variant="warning">
                Bruker fyller 67 år i inntektsåret og antall innvilga måneder vil bli tilpasset deretter.
              </Alert>
            )}
            <AvkortingInntektTabell avkortingGrunnlagListe={listeVisningAvkortingGrunnlag()} fyller67={fyller67} />
          </VStack>
        )}
      {erInnevaerendeAar &&
        avkortingGrunnlagFrontend &&
        ((avkortingGrunnlagFrontend.fraVirk == null && avkortingGrunnlagFrontend.historikk.length > 1) ||
          (avkortingGrunnlagFrontend.fraVirk != null && avkortingGrunnlagFrontend.historikk.length > 0)) && (
          <Button variant="tertiary" onClick={() => setVisHistorikk(!visHistorikk)}>
            Historikk{' '}
            {visHistorikk ? <ChevronUpIcon className="dropdownIcon" /> : <ChevronDownIcon className="dropdownIcon" />}
          </Button>
        )}
      {erRedigerbar && visForm && (
        <AvkortingInntektForm
          behandling={behandling}
          avkortingGrunnlagFrontend={avkortingGrunnlagFrontend}
          erInnevaerendeAar={erInnevaerendeAar}
          setVisForm={setVisForm}
        />
      )}
      {erRedigerbar && !visForm && (
        <HStack marginBlock="4 0">
          <Button
            size="small"
            variant="secondary"
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            onClick={(e) => {
              e.preventDefault()
              setVisForm(true)
              resetInntektsavkortingValidering()
            }}
          >
            {knappTekst()}
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
