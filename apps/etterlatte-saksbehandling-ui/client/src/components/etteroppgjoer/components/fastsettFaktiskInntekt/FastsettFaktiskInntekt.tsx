import { Alert, BodyShort, Button, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { formaterMaanednavnAar } from '~utils/formatering/dato'
import { FaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { FaktiskInntektVisning } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektVisning'
import { PencilIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { FieldErrors } from 'react-hook-form'
import { IInformasjonFraBruker } from '~shared/types/EtteroppgjoerForbehandling'
import { finnInnvilgedePerioder } from './innvilgedePerioder'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

interface Props {
  erRedigerbar: boolean
  setFastsettFaktiskInntektSkjemaErrors: (errors: FieldErrors<IInformasjonFraBruker> | undefined) => void
}

export const FastsettFaktiskInntekt = ({ erRedigerbar, setFastsettFaktiskInntektSkjemaErrors }: Props) => {
  const beregnOverFlerePerioder = useFeaturetoggle(FeatureToggle.beregn_over_flere_perioder)
  const { forbehandling, faktiskInntekt, opplysninger } = useEtteroppgjoerForbehandling()

  const [faktiskInntektSkjemaErAapen, setFaktiskInntektSkjemaErAapen] = useState<boolean>(
    erRedigerbar && !faktiskInntekt
  )

  const tidligereYtelser = opplysninger.tidligereAvkorting?.avkortetYtelse
  const innvilgedePerioder = tidligereYtelser
    ? finnInnvilgedePerioder(tidligereYtelser, forbehandling.innvilgetPeriode)
    : []
  if (innvilgedePerioder.length === 0) {
    return (
      <Alert variant="error">
        Kan ikke fastsette faktisk inntekt fordi innvilgede perioder mangler i beregningsgrunnlaget.
      </Alert>
    )
  }

  if (erRedigerbar && innvilgedePerioder.length > 1 && !beregnOverFlerePerioder) {
    return <Alert variant="error">Etteroppgjør med flere innvilgede perioder er ikke aktivert.</Alert>
  }

  const maanederMedIngenBeregnetYtelseEtteroppgjoeret =
    opplysninger.tidligereAvkorting?.avkortetYtelse
      ?.filter(
        (ytelse) => ytelse.fom >= forbehandling.innvilgetPeriode.fom && ytelse.tom <= forbehandling.innvilgetPeriode.tom
      )
      ?.filter((ytelse) => ytelse.ytelseFoerAvkorting === 0) ?? []

  return (
    <VStack gap="space-16">
      <Heading size="medium" level="2">
        Fastsett faktisk inntekt
      </Heading>
      <BodyShort>
        {innvilgedePerioder.length > 1
          ? 'Fastsett samlet faktisk inntekt for de innvilgede periodene nedenfor. Inntekt i opphørsperioder mellom dem skal ikke tas med.'
          : 'Fastsett den faktiske inntekten for bruker i den innvilgede perioden.'}
      </BodyShort>
      <VStack gap="space-16" maxWidth="42.5rem">
        <HStack gap="space-8">
          {innvilgedePerioder.map((periode) => (
            <Tag variant="neutral" key={periode.fom}>
              {formaterMaanednavnAar(periode.fom)} - {formaterMaanednavnAar(periode.tom)}
            </Tag>
          ))}
        </HStack>
        {maanederMedIngenBeregnetYtelseEtteroppgjoeret.length > 0 && (
          <Alert variant="warning">
            <VStack gap="space-8">
              <BodyShort>
                Bruker har ikke hatt innvilget omstillingsstønad hele året, på grunn av sanksjon, stans eller fengsel.
                Dette gjelder følgende perioder:
              </BodyShort>
              <div>
                <ul>
                  {maanederMedIngenBeregnetYtelseEtteroppgjoeret.map((maaned) => (
                    <li key={maaned.fom}>
                      {formaterMaanednavnAar(maaned.fom)} - {formaterMaanednavnAar(maaned.tom)}
                    </li>
                  ))}
                </ul>
              </div>
            </VStack>
          </Alert>
        )}
      </VStack>
      {faktiskInntektSkjemaErAapen && erRedigerbar ? (
        <FaktiskInntektSkjema
          setFaktiskInntektSkjemaErAapen={setFaktiskInntektSkjemaErAapen}
          setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
        />
      ) : (
        <VStack gap="space-16">
          <FaktiskInntektVisning />
          {erRedigerbar && (
            <div>
              <Button
                size="small"
                variant="secondary"
                icon={<PencilIcon aria-hidden />}
                onClick={() => setFaktiskInntektSkjemaErAapen(true)}
              >
                Rediger
              </Button>
            </div>
          )}
        </VStack>
      )}
    </VStack>
  )
}
