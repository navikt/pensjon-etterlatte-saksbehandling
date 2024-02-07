import { Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import { Klage, Utfall } from '~shared/types/Klage'
import { KlageVurderingRedigering } from '~components/klage/vurdering/KlageVurderingRedigering'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterInitieltUtfallForKlage } from '~shared/api/klage'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { ContentHeader } from '~shared/styled'
import { HeadingWrapper } from '~components/person/SakOversikt'
import { InitiellVurderingVisning } from '~components/klage/vurdering/KlageInitiellVurderingVisning'
import { updateInitieltUtfall } from '~store/reducers/KlageReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'

export const KlageInitiellVurdering = (props: { klage: Klage }) => {
  const klage = props.klage

  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [begrunnelse, setBegrunnelse] = useState<string>('')
  const [utfall, setUtfall] = useState<Utfall>()

  const harIkkeInitieltUtfall = !klage?.initieltUtfall
  const [redigerbar, setRedigerbar] = useState<boolean>(false)

  const [lagreInitiellStatus, lagreInitiellKlageUtfall] = useApiCall(oppdaterInitieltUtfallForKlage)
  const redigeringsModus = redigerbar || harIkkeInitieltUtfall

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

  return (
    <>
      <>
        <ContentHeader>
          <HeadingWrapper>
            <Heading level="2" size="large">
              Første vurdering av utfall saksbehandler
            </Heading>
          </HeadingWrapper>
        </ContentHeader>
        <InnholdPadding>
          {redigeringsModus ? (
            <>
              <RadioGroup legend="" onChange={(e) => setUtfall(e as Utfall)}>
                <Radio value={Utfall.OMGJOERING}>Omgjøring av vedtak</Radio>
                <Radio value={Utfall.DELVIS_OMGJOERING}>Delvis omgjøring av vedtak</Radio>
                <Radio value={Utfall.STADFESTE_VEDTAK}>Stadfeste vedtaket</Radio>
              </RadioGroup>

              {utfall && (
                <>
                  <Textarea
                    size="medium"
                    label={getTextFromutfall(utfall)}
                    value={begrunnelse}
                    onChange={(e) => setBegrunnelse(e.target.value)}
                  />
                  <Button
                    type="button"
                    variant="secondary"
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
                    Lagre
                  </Button>
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
              <InitiellVurderingVisning klage={klage} />
              <Button
                style={{ marginBottom: '3em' }}
                type="button"
                variant="secondary"
                onClick={() => setRedigerbar(true)}
              >
                Rediger
              </Button>
            </>
          )}
        </InnholdPadding>
      </>

      <KlageVurderingRedigering klage={klage} />
    </>
  )
}
