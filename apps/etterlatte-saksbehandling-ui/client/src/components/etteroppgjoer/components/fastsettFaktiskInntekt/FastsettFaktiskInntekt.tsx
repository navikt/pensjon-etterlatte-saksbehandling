import { Alert, BodyShort, Button, Heading, Tag, VStack } from '@navikt/ds-react'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { formaterMaanednavnAar, maanedNavn } from '~utils/formatering/dato'
import { FaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { FaktiskInntektVisning } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektVisning'
import { PencilIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { FieldErrors } from 'react-hook-form'
import { IInformasjonFraBruker } from '~shared/types/EtteroppgjoerForbehandling'

interface Props {
  erRedigerbar: boolean
  setFastsettFaktiskInntektSkjemaErrors: (errors: FieldErrors<IInformasjonFraBruker> | undefined) => void
}

export const FastsettFaktiskInntekt = ({ erRedigerbar, setFastsettFaktiskInntektSkjemaErrors }: Props) => {
  const { forbehandling, faktiskInntekt, opplysninger } = useEtteroppgjoerForbehandling()

  const [faktiskInntektSkjemaErAapen, setFaktiskInntektSkjemaErAapen] = useState<boolean>(
    erRedigerbar && !faktiskInntekt
  )

  const maanederMedIngenBeregnetYtelseEtteroppgjoeret =
    opplysninger.tidligereAvkorting?.avkortetYtelse
      ?.filter(
        (ytelse) => ytelse.fom >= forbehandling.innvilgetPeriode.fom && ytelse.tom <= forbehandling.innvilgetPeriode.tom
      )
      ?.filter((ytelse) => ytelse.ytelseFoerAvkorting === 0) ?? []

  return (
    <VStack gap="4">
      <Heading size="large">Fastsett faktisk inntekt</Heading>
      <BodyShort>Fastsett den faktiske inntekten for bruker i den innvilgede perioden.</BodyShort>
      <VStack gap="4" maxWidth="42.5rem">
        <div>
          <Tag variant="neutral">
            {maanedNavn(forbehandling.innvilgetPeriode.fom)} - {maanedNavn(forbehandling.innvilgetPeriode.tom)}
          </Tag>
        </div>
        {maanederMedIngenBeregnetYtelseEtteroppgjoeret.length > 0 && (
          <Alert variant="warning">
            <VStack gap="2">
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
        <VStack gap="4">
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
