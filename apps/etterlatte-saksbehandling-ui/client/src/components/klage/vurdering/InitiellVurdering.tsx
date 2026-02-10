import { Alert, BodyLong, Button, Heading, Radio, Textarea, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { IniteltUtfallMedBegrunnelseDto, Klage, teksterKlageutfall, Utfall } from '~shared/types/Klage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterInitieltUtfallForKlage } from '~shared/api/klage'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { addKlage } from '~store/reducers/KlageReducer'
import { useAppDispatch } from '~store/Store'
import { InitiellVurderingVisningContent } from '~components/klage/vurdering/InitiellVurderingVisning'
import { PencilIcon } from '@navikt/aksel-icons'
import { FieldOrNull } from '~shared/types/util'
import { useForm } from 'react-hook-form'
import { ButtonNavigerTilBrev } from '~components/klage/vurdering/KlageVurderingFelles'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

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

  const [redigeres, setRedigeres] = useState<boolean>(!klage?.initieltUtfall)

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
        setRedigeres(false)
      }
    )
  }

  const stoetterDelvisOmgjoering = useFeaturetoggle(FeatureToggle.pensjon_etterlatte_klage_delvis_omgjoering)

  return (
    <VStack gap="space-4" width="30rem">
      <Heading level="2" size="medium" spacing>
        Første vurdering
      </Heading>
      <>
        {redigeres ? (
          <form onSubmit={handleSubmit(lagreInitieltUtfall)}>
            <VStack gap="space-4" width="41.5rem">
              <ControlledRadioGruppe
                name="utfall"
                control={control}
                legend=""
                errorVedTomInput="Du må velge et initielt utfall"
                radios={
                  <>
                    <Radio value={Utfall.OMGJOERING}>Omgjøring av vedtak</Radio>
                    {stoetterDelvisOmgjoering && (
                      <Radio value={Utfall.DELVIS_OMGJOERING}>Delvis omgjøring av vedtak</Radio>
                    )}
                    <Radio value={Utfall.STADFESTE_VEDTAK}>Stadfeste vedtaket</Radio>
                  </>
                }
              />
              {formUtfall && (
                <>
                  <Textarea size="medium" label={getTextFromutfall(formUtfall)} {...register('begrunnelse')} />
                  <div>
                    <Button type="submit" variant="primary" loading={isPending(lagreInitiellStatus)} size="small">
                      Lagre vurdering
                    </Button>
                  </div>
                  {isFailureHandler({
                    apiResult: lagreInitiellStatus,
                    errorMessage:
                      'Kunne ikke lagre initielt utfallet av klagen. Prøv igjen senere, og meld sak hvis problemet vedvarer.',
                  })}
                </>
              )}
            </VStack>
          </form>
        ) : (
          <div>
            <InitiellVurderingVisningContent klage={klage} />
            <Button
              type="button"
              size="small"
              icon={<PencilIcon aria-hidden />}
              variant="secondary"
              onClick={() => setRedigeres(true)}
            >
              Rediger
            </Button>
          </div>
        )}
        {klage.initieltUtfall?.utfallMedBegrunnelse?.utfall === Utfall.STADFESTE_VEDTAK && !klage.utfall && (
          <Alert variant="info">
            <Heading level="2" size="small">
              Informer om saksbehandlingstid
            </Heading>
            <BodyLong spacing>
              Siden vurderingen er satt til &quot;{teksterKlageutfall[Utfall.STADFESTE_VEDTAK].toLowerCase()}&quot; må
              du manuelt informere klager om saksbehandlingstid hvis den går ut over 4 uker.
            </BodyLong>
            <ButtonNavigerTilBrev klage={klage} />
          </Alert>
        )}
      </>
    </VStack>
  )
}
