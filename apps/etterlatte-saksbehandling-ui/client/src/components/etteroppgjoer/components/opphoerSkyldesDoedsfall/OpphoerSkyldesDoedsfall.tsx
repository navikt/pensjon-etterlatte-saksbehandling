import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useState } from 'react'
import { BodyShort, Button, Heading, HStack, Label, Radio, VStack } from '@navikt/ds-react'
import { FieldErrors, useForm } from 'react-hook-form'
import { JaNei } from '~shared/types/ISvar'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { PencilIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreOmOpphoerSkyldesDoedsfall } from '~shared/api/etteroppgjoer'
import { useAppDispatch } from '~store/Store'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'

export interface OpphoerSkyldesDoedsfallSkjema {
  opphoerSkyldesDoedsfall: JaNei
}

interface Props {
  erRedigerbar: boolean
  setOpphoerSkyldesDoedsfallSkjemaErrors: (errors: FieldErrors<OpphoerSkyldesDoedsfallSkjema> | undefined) => void
}

export const OpphoerSkyldesDoedsfall = ({ erRedigerbar, setOpphoerSkyldesDoedsfallSkjemaErrors }: Props) => {
  const { behandling } = useEtteroppgjoer()
  const dispatch = useAppDispatch()

  const [lagreOmOpphoerSkyldesDoedsfallResult, lagreOmOpphoerSkyldesDoedsfallRequest] =
    useApiCall(lagreOmOpphoerSkyldesDoedsfall)
  const [hentEtteroppgjoerForbehandlingResult, hentEtteroppgjoerForbehandlingFetch] =
    useApiCall(hentEtteroppgjoerForbehandling)

  const [opphoerSkyldesDoedsfallSkjemaErAapen, setOpphoerSkyldesDoedsfallSkjemaErAapen] = useState<boolean>(
    erRedigerbar && !behandling.opphoerSkyldesDoedsfall
  )

  const { control, handleSubmit } = useForm<OpphoerSkyldesDoedsfallSkjema>({
    defaultValues: {
      opphoerSkyldesDoedsfall: behandling.opphoerSkyldesDoedsfall,
    },
  })

  const avbryt = () => {
    setOpphoerSkyldesDoedsfallSkjemaErrors(undefined)
    setOpphoerSkyldesDoedsfallSkjemaErAapen(false)
  }

  const submitOmOpphoerSkyldesDoedsfall = (data: OpphoerSkyldesDoedsfallSkjema) => {
    setOpphoerSkyldesDoedsfallSkjemaErrors(undefined)
    lagreOmOpphoerSkyldesDoedsfallRequest(
      { forbehandlingId: behandling.id, opphoerSkyldesDoedsfall: data.opphoerSkyldesDoedsfall },
      () => {
        hentEtteroppgjoerForbehandlingFetch(behandling.id, (etteroppgjoerForbehandling) => {
          dispatch(addEtteroppgjoer(etteroppgjoerForbehandling))
          setOpphoerSkyldesDoedsfallSkjemaErAapen(false)
        })
      }
    )
  }

  return (
    <form>
      <VStack gap="4">
        <Heading size="large">Opphoer skyldes doedsfall</Heading>
        <BodyShort>
          Det er registrert et opphør på saken, du må derfor svare på om opphøret skyldes et dødsfall.
        </BodyShort>

        {opphoerSkyldesDoedsfallSkjemaErAapen && erRedigerbar ? (
          <VStack gap="4">
            <ControlledRadioGruppe
              name="opphoerSkyldesDoedsfall"
              control={control}
              legend="Skyldes opphøret et dødsfall?"
              errorVedTomInput="Du må svare på om opphøret gjelder et dødsfall"
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />

            {isFailureHandler({
              apiResult: lagreOmOpphoerSkyldesDoedsfallResult,
              errorMessage: 'Kunne ikke lagre om opphoer skyldes dødsfall',
            })}
            {isFailureHandler({
              apiResult: hentEtteroppgjoerForbehandlingResult,
              errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
            })}

            <HStack gap="4">
              <Button
                size="small"
                onClick={handleSubmit(submitOmOpphoerSkyldesDoedsfall, setOpphoerSkyldesDoedsfallSkjemaErrors)}
                loading={
                  isPending(lagreOmOpphoerSkyldesDoedsfallResult) || isPending(hentEtteroppgjoerForbehandlingResult)
                }
              >
                Lagre
              </Button>
              {!!behandling.opphoerSkyldesDoedsfall && (
                <Button
                  type="button"
                  size="small"
                  variant="secondary"
                  onClick={avbryt}
                  disabled={
                    isPending(lagreOmOpphoerSkyldesDoedsfallResult) || isPending(hentEtteroppgjoerForbehandlingResult)
                  }
                >
                  Avbryt
                </Button>
              )}
            </HStack>
          </VStack>
        ) : (
          <VStack gap="4">
            <VStack gap="2">
              <Label>Om opphør skyldes dødsfall</Label>
              <BodyShort>{behandling.opphoerSkyldesDoedsfall === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
            </VStack>
            {erRedigerbar && (
              <div>
                <Button
                  size="small"
                  variant="secondary"
                  icon={<PencilIcon aria-hidden />}
                  onClick={() => setOpphoerSkyldesDoedsfallSkjemaErAapen(true)}
                >
                  Rediger
                </Button>
              </div>
            )}
          </VStack>
        )}
      </VStack>
    </form>
  )
}
