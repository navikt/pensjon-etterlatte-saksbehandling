import { Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import { IniteltUtfallMedBegrunnelseDto, Klage, Utfall } from '~shared/types/Klage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterInitieltUtfallForKlage } from '~shared/api/klage'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { addKlage } from '~store/reducers/KlageReducer'
import { useAppDispatch } from '~store/Store'
import { InitiellVurderingVisningContent } from '~components/klage/vurdering/InitiellVurderingVisning'
import { VurderingWrapper } from '~components/klage/styled'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { PencilIcon } from '@navikt/aksel-icons'
import { FieldOrNull } from '~shared/types/util'
import { Controller, useForm } from 'react-hook-form'

const getTextFromutfall = (utfall: Utfall): string => {
  switch (utfall) {
    case Utfall.AVVIST:
      return 'Hvorfor avvises det?'
    case Utfall.AVVIST_MED_OMGJOERING:
      return 'Hvorfor avvises det med omgjøring?'
    case Utfall.OMGJOERING:
      return 'Hvorfor skal det omgjøres?'
    case Utfall.DELVIS_OMGJOERING:
      return 'Hvorfor skal det delvis omgjøres?'
    case Utfall.STADFESTE_VEDTAK:
      return 'Hvorfor står vedtaket seg?'
  }
}

type InitiellVurderingForm = FieldOrNull<IniteltUtfallMedBegrunnelseDto>

function initiellVurderingErUtfylt(formdata: InitiellVurderingForm): formdata is IniteltUtfallMedBegrunnelseDto {
  return !!formdata.utfall
}

export const InitiellVurdering = (props: { klage: Klage }) => {
  const klage = props.klage

  const dispatch = useAppDispatch()
  const harIkkeInitieltUtfall = !klage?.initieltUtfall
  const [redigerbar, setRedigerbar] = useState<boolean>(false)

  const [lagreInitiellStatus, lagreInitiellKlageUtfall] = useApiCall(oppdaterInitieltUtfallForKlage)
  const { handleSubmit, control, register, watch } = useForm<InitiellVurderingForm>({
    defaultValues: klage.initieltUtfall?.utfallMedBegrunnelse ?? { begrunnelse: null, utfall: null },
  })

  const formUtfall = watch('utfall')

  function lagreInitieltUtfall(formdata: InitiellVurderingForm) {
    if (!klage) {
      return
    }
    if (!initiellVurderingErUtfylt(formdata)) {
      return
    }
    lagreInitiellKlageUtfall(
      {
        klageId: klage.id,
        utfallMedBegrunnelse: formdata,
      },
      (klage) => {
        dispatch(addKlage(klage))
        setRedigerbar(false)
      }
    )
  }

  const redigeringsModus = redigerbar || harIkkeInitieltUtfall

  const stoetterDelvisOmgjoering = useFeatureEnabledMedDefault('pensjon-etterlatte.klage-delvis-omgjoering', false)

  return (
    <>
      <Heading level="2" size="medium">
        Første vurdering
      </Heading>
      <>
        {redigeringsModus ? (
          <form onSubmit={handleSubmit(lagreInitieltUtfall)}>
            <Controller
              name="utfall"
              control={control}
              rules={{
                required: {
                  value: true,
                  message: 'Du må velge et initielt utfall.',
                },
              }}
              render={({ field, fieldState }) => {
                return (
                  <VurderingWrapper>
                    <RadioGroup legend="" {...field} error={fieldState.error?.message}>
                      <Radio value={Utfall.OMGJOERING}>Omgjøring av vedtak</Radio>
                      {stoetterDelvisOmgjoering && (
                        <Radio value={Utfall.DELVIS_OMGJOERING}>Delvis omgjøring av vedtak</Radio>
                      )}
                      <Radio value={Utfall.STADFESTE_VEDTAK}>Stadfeste vedtaket</Radio>
                    </RadioGroup>
                  </VurderingWrapper>
                )
              }}
            />
            {formUtfall && (
              <>
                <VurderingWrapper>
                  <Textarea size="medium" label={getTextFromutfall(formUtfall)} {...register('begrunnelse')} />
                </VurderingWrapper>
                <VurderingWrapper>
                  <Button type="submit" variant="primary" loading={isPending(lagreInitiellStatus)}>
                    Lagre vurdering
                  </Button>
                </VurderingWrapper>
                {isFailureHandler({
                  apiResult: lagreInitiellStatus,
                  errorMessage:
                    'Kunne ikke lagre initielt utfallet av klagen. Prøv igjen senere, og meld sak hvis problemet vedvarer.',
                })}
              </>
            )}
          </form>
        ) : (
          <>
            <InitiellVurderingVisningContent klage={klage} />
            <Button
              style={{ marginBottom: '3em' }}
              type="button"
              size="small"
              icon={<PencilIcon />}
              variant="secondary"
              onClick={() => setRedigerbar(true)}
            >
              Rediger
            </Button>
          </>
        )}
      </>
    </>
  )
}
