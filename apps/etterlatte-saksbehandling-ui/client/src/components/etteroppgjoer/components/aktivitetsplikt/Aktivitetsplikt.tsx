import { useState } from 'react'
import { Alert, BodyShort, Box, Button, Heading, HStack, Label, Link, Radio, Textarea, VStack } from '@navikt/ds-react'
import { FieldErrors, useForm } from 'react-hook-form'
import { PencilIcon } from '@navikt/aksel-icons'
import {
  addDetaljertEtteroppgjoerForbehandling,
  useEtteroppgjoerForbehandling,
} from '~store/reducers/EtteroppgjoerReducer'
import { Aktivitetsplikt } from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreAktivitetsplikt } from '~shared/api/etteroppgjoer'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'

interface Props {
  erRedigerbar: boolean
  setAktivitetspliktSkjemaErrors: (errors: FieldErrors<Aktivitetsplikt> | undefined) => void
}

export const AktivitetspliktSpørsmål = ({ erRedigerbar, setAktivitetspliktSkjemaErrors }: Props) => {
  const { forbehandling } = useEtteroppgjoerForbehandling()
  const dispatch = useAppDispatch()

  const [lagreAktivitetspliktResult, lagreAktivitetspliktRequest] = useApiCall(lagreAktivitetsplikt)
  const [hentEtteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  const [skjemaErAapent, setSkjemaErAapent] = useState<boolean>(erRedigerbar && !forbehandling.aktivitetspliktOverholdt)

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<Aktivitetsplikt>({
    defaultValues: {
      aktivitetspliktOverholdt: forbehandling.aktivitetspliktOverholdt,
      begrunnelse: forbehandling.aktivitetspliktBegrunnelse ?? '',
    },
  })

  const avbryt = () => {
    setAktivitetspliktSkjemaErrors(undefined)
    setSkjemaErAapent(false)
  }

  const submitAktivitetsplikt = (data: Aktivitetsplikt) => {
    setAktivitetspliktSkjemaErrors(undefined)
    lagreAktivitetspliktRequest({ forbehandlingId: forbehandling.id, aktivitetsplikt: data }, () => {
      hentEtteroppgjoerRequest(forbehandling.id, (etteroppgjoer) => {
        dispatch(addDetaljertEtteroppgjoerForbehandling(etteroppgjoer))
        setSkjemaErAapent(false)
      })
    })
  }

  return (
    <form>
      <VStack gap="4">
        <Heading size="large">Aktivitetsplikt</Heading>
        <BodyShort>
          Vurder om aktivitetsplikten er overholdt i etteroppgjørsåret.{' '}
          <Link href="https://lovdata.no/pro/lov/1997-02-28-19/§17-7" target="_blank">
            Se folketrygdloven § 17-7
          </Link>
        </BodyShort>

        {skjemaErAapent && erRedigerbar ? (
          <VStack gap="4">
            <ControlledRadioGruppe
              name="aktivitetspliktOverholdt"
              control={control}
              legend="Er aktivitetsplikten overholdt i etteroppgjørsåret?"
              errorVedTomInput="Du må svare på om aktivitetsplikten er overholdt"
              readOnly={!erRedigerbar}
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />

            <Box maxWidth="30rem">
              <Textarea
                {...register('begrunnelse', {
                  required: {
                    value: true,
                    message: 'Du må oppgi en begrunnelse',
                  },
                })}
                label="Begrunnelse"
                readOnly={!erRedigerbar}
                error={errors.begrunnelse?.message}
              />
            </Box>

            {isFailureHandler({
              apiResult: lagreAktivitetspliktResult,
              errorMessage: 'Kunne ikke lagre svar om aktivitetsplikt',
            })}

            {isFailureHandler({
              apiResult: hentEtteroppgjoerResult,
              errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
            })}

            <HStack gap="4">
              <Button
                size="small"
                loading={isPending(lagreAktivitetspliktResult) || isPending(hentEtteroppgjoerResult)}
                onClick={handleSubmit(submitAktivitetsplikt, () => setAktivitetspliktSkjemaErrors(errors))}
              >
                Lagre
              </Button>

              {!!forbehandling.aktivitetspliktOverholdt && (
                <Button
                  type="button"
                  variant="secondary"
                  size="small"
                  disabled={isPending(lagreAktivitetspliktResult) || isPending(hentEtteroppgjoerResult)}
                  onClick={avbryt}
                >
                  Avbryt
                </Button>
              )}
            </HStack>
          </VStack>
        ) : (
          <VStack gap="4">
            {!forbehandling.aktivitetspliktOverholdt ? (
              !erRedigerbar ? (
                <VStack gap="2">
                  <Label>Aktivitetsplikt</Label>
                  <BodyShort>
                    Spørsmålet om aktivitetsplikt ble lagt til i ettertid, og er ikke svart på i denne behandlingen.
                  </BodyShort>
                </VStack>
              ) : (
                <Heading size="small">Spørsmål om aktivitetsplikt er ikke besvart</Heading>
              )
            ) : (
              <>
                <VStack gap="2">
                  <Label>Er aktivitetsplikten overholdt i etteroppgjørsåret?</Label>
                  <BodyShort>{forbehandling.aktivitetspliktOverholdt === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
                </VStack>
                {forbehandling.aktivitetspliktBegrunnelse && (
                  <VStack gap="2" maxWidth="30rem">
                    <Label>Begrunnelse</Label>
                    <BodyShort>{forbehandling.aktivitetspliktBegrunnelse}</BodyShort>
                  </VStack>
                )}
              </>
            )}
            {erRedigerbar && (
              <div>
                <Button
                  size="small"
                  variant="secondary"
                  icon={<PencilIcon aria-hidden />}
                  onClick={() => setSkjemaErAapent(true)}
                >
                  Rediger
                </Button>
              </div>
            )}
          </VStack>
        )}

        {forbehandling.aktivitetspliktOverholdt === JaNei.NEI && (
          <Alert variant="warning">
            Aktivitetsplikten er ikke overholdt. Etteroppgjøret kan ikke gjennomføres før aktivitetsplikten for
            etteroppgjørsåret er vurdert på nytt.
          </Alert>
        )}
      </VStack>
    </form>
  )
}
