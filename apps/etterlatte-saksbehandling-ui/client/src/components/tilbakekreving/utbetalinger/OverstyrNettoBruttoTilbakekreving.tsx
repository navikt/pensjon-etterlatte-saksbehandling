import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { SakType } from '~shared/types/sak'
import { BodyLong, BodyShort, Box, Button, Heading, HStack, Radio, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { DocPencilIcon, FloppydiskIcon, PencilIcon, PlusIcon, TrashIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { JaNei } from '~shared/types/ISvar'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterOverstyringNettoBrutto } from '~shared/api/tilbakekreving'
import { useAppDispatch } from '~store/Store'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

const teksterOverstyring: Record<JaNei, string> = {
  JA: 'Ja, netto tilbakekreving skal sendes som brutto av nettobeløpet',
  NEI: 'Nei, tilbakekrevingen skal sendes slik som den er behandlet',
}

export function OverstyrNettoBruttoTilbakekreving(props: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const { behandling, redigerbar } = props
  const dispatch = useAppDispatch()
  const overstyringEnabled = useFeaturetoggle(FeatureToggle.overstyr_netto_brutto_tilbakekreving)
  const [lagreOverstringResult, lagreOverstyringFetch] = useApiCall(oppdaterOverstyringNettoBrutto)
  const [redigerOverstyring, setRedigerOverstyring] = useState<boolean>(false)
  const { control, getValues, reset, handleSubmit } = useForm<{ overstyr: JaNei }>({
    defaultValues: {
      overstyr: behandling.tilbakekreving.overstyrBehandletNettoTilBruttoMotTilbakekreving,
    },
  })

  if (!overstyringEnabled) {
    return null
  }
  if (behandling.sak.sakType !== SakType.BARNEPENSJON) {
    return null
  }

  function lagreSkjema(data: { overstyr?: JaNei }) {
    lagreOverstyringFetch(
      {
        tilbakekrevingId: behandling.id,
        overstyrNettoBrutto: data.overstyr,
      },
      (oppdatertTilbakekreving) => {
        dispatch(addTilbakekreving(oppdatertTilbakekreving))
        setRedigerOverstyring(false)
      }
    )
  }

  const overstyringSkjemaVerdi = getValues('overstyr')

  return (
    <form onSubmit={handleSubmit(lagreSkjema)}>
      <VStack gap="4">
        <HStack gap="2" align="center">
          <DocPencilIcon aria-hidden height="1.5rem" width="1.5rem" />
          <Heading size="small" level="3">
            Overstyr netto til brutto mot tilbakekrevingskomponenten
          </Heading>
        </HStack>
        {!redigerOverstyring && (
          <>
            <VStack gap="4">
              {overstyringSkjemaVerdi ? (
                <BodyLong>{teksterOverstyring[overstyringSkjemaVerdi]}</BodyLong>
              ) : (
                <BodyShort>Ingen vurdering av overstyring lagt inn.</BodyShort>
              )}

              {redigerbar && (
                <HStack gap="4">
                  <Button
                    type="button"
                    variant="secondary"
                    size="small"
                    icon={overstyringSkjemaVerdi ? <PencilIcon aria-hidden /> : <PlusIcon aria-hidden />}
                    onClick={() => setRedigerOverstyring(true)}
                  >
                    {overstyringSkjemaVerdi ? 'Rediger' : 'Legg til'}
                  </Button>
                  {overstyringSkjemaVerdi && (
                    <Button
                      type="button"
                      variant="secondary"
                      size="small"
                      loading={isPending(lagreOverstringResult)}
                      icon={<TrashIcon aria-hidden />}
                      onClick={() => {
                        reset({ overstyr: undefined })
                        lagreSkjema({ overstyr: undefined })
                      }}
                    >
                      Slett
                    </Button>
                  )}
                </HStack>
              )}
            </VStack>
          </>
        )}
        {redigerbar && redigerOverstyring && (
          <VStack gap="2">
            <Box width="35rem">
              <ControlledRadioGruppe
                name="overstyr"
                control={control}
                legend="Skal netto overstyres til brutto"
                radios={
                  <>
                    <Radio value={JaNei.JA}>Ja</Radio>
                    <Radio value={JaNei.NEI}>Nei</Radio>
                  </>
                }
              />
            </Box>
            {mapFailure(lagreOverstringResult, (error) => (
              <ApiErrorAlert>
                Kunne ikke lagre om kravgrunnlaget skal overstyres til tilbakekrevingskomponenten, på grunn av feil:{' '}
                {error.detail}
              </ApiErrorAlert>
            ))}
            <HStack gap="4">
              <Button
                type="button"
                variant="secondary"
                size="small"
                icon={<XMarkIcon aria-hidden />}
                onClick={() => {
                  setRedigerOverstyring(false)
                  reset()
                }}
              >
                Avbryt
              </Button>
              <Button size="small" icon={<FloppydiskIcon aria-hidden />} loading={isPending(lagreOverstringResult)}>
                Lagre
              </Button>
            </HStack>
          </VStack>
        )}
      </VStack>
    </form>
  )
}
