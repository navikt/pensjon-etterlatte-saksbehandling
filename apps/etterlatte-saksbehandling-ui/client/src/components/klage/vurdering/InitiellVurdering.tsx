import { Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import { Klage, Utfall } from '~shared/types/Klage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterInitieltUtfallForKlage } from '~shared/api/klage'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { updateInitieltUtfall } from '~store/reducers/KlageReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { InitiellVurderingVisningContent } from '~components/klage/vurdering/InitiellVurderingVisning'
import { VurderingWrapper } from '~components/klage/styled'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { PencilIcon } from '@navikt/aksel-icons'

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

export const InitiellVurdering = (props: { klage: Klage }) => {
  const klage = props.klage

  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [begrunnelse, setBegrunnelse] = useState<string>('')
  const [utfall, setUtfall] = useState<Utfall>()

  const harIkkeInitieltUtfall = !klage?.initieltUtfall
  const [redigerbar, setRedigerbar] = useState<boolean>(false)

  const [lagreInitiellStatus, lagreInitiellKlageUtfall] = useApiCall(oppdaterInitieltUtfallForKlage)
  const redigeringsModus = redigerbar || harIkkeInitieltUtfall

  const stoetterDelvisOmgjoering = useFeatureEnabledMedDefault('pensjon-etterlatte.klage-delvis-omgjoering', false)

  return (
    <>
      <Heading level="2" size="medium">
        Første vurdering
      </Heading>
      <>
        {redigeringsModus ? (
          <>
            <VurderingWrapper>
              <RadioGroup legend="" onChange={(e) => setUtfall(e as Utfall)}>
                <Radio value={Utfall.OMGJOERING}>Omgjøring av vedtak</Radio>
                {stoetterDelvisOmgjoering && <Radio value={Utfall.DELVIS_OMGJOERING}>Delvis omgjøring av vedtak</Radio>}
                <Radio value={Utfall.STADFESTE_VEDTAK}>Stadfeste vedtaket</Radio>
              </RadioGroup>
            </VurderingWrapper>
            {utfall && (
              <>
                <VurderingWrapper>
                  <Textarea
                    size="medium"
                    label={getTextFromutfall(utfall)}
                    value={begrunnelse}
                    onChange={(e) => setBegrunnelse(e.target.value)}
                  />
                </VurderingWrapper>
                <VurderingWrapper>
                  <Button
                    type="button"
                    variant="primary"
                    loading={isPending(lagreInitiellStatus)}
                    onClick={() => {
                      const utfallMedBegrunnelse = { utfall: utfall, begrunnelse: begrunnelse }
                      lagreInitiellKlageUtfall(
                        {
                          klageId: klage?.id,
                          utfallMedBegrunnelse,
                        },
                        () => {
                          dispatch(
                            updateInitieltUtfall({
                              utfallMedBegrunnelse: utfallMedBegrunnelse,
                              saksbehandler: innloggetSaksbehandler.ident,
                              tidspunkt: Date().toString(),
                            })
                          )
                          setRedigerbar(false)
                        }
                      )
                    }}
                  >
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
          </>
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
