import { Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { FieldErrors, useForm } from 'react-hook-form'
import { Aktivitetsplikt } from '~shared/types/EtteroppgjoerForbehandling'
import {
  addDetaljertEtteroppgjoerForbehandling,
  useEtteroppgjoerForbehandling,
} from '~store/reducers/EtteroppgjoerReducer'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreAktivitetsplikt } from '~shared/api/etteroppgjoer'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { JaNei } from '~shared/types/ISvar'

interface Props {
  setAktivitetspliktSkjemaErAapen: (erAapen: boolean) => void
  erRedigerbar: boolean
  setAktivitetspliktSkjemaErrors: (errors: FieldErrors<Aktivitetsplikt> | undefined) => void
}

export const AktivitetspliktSkjema = ({
  setAktivitetspliktSkjemaErAapen,
  erRedigerbar,
  setAktivitetspliktSkjemaErrors,
}: Props) => {
  const { forbehandling } = useEtteroppgjoerForbehandling()
  const dispatch = useAppDispatch()

  const [lagreAktivitetspliktResult, lagreAktivitetspliktRequest] = useApiCall(lagreAktivitetsplikt)
  const [hentEtteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

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
    setAktivitetspliktSkjemaErAapen(false)
  }

  const submitAktivitetsplikt = (data: Aktivitetsplikt) => {
    setAktivitetspliktSkjemaErrors(undefined)
    lagreAktivitetspliktRequest({ forbehandlingId: forbehandling.id, aktivitetsplikt: data }, () => {
      hentEtteroppgjoerRequest(forbehandling.id, (etteroppgjoer) => {
        dispatch(addDetaljertEtteroppgjoerForbehandling(etteroppgjoer))
        setAktivitetspliktSkjemaErAapen(false)
      })
    })
  }

  return (
    <form>
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
    </form>
  )
}
