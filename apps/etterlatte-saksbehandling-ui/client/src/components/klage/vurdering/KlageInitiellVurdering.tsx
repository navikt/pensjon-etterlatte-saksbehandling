import { Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import { Klage, Utfall } from '~shared/types/Klage'
import { KlageVurderingRedigering } from '~components/klage/vurdering/KlageVurderingRedigering'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterInitieltUtfallForKlage } from '~shared/api/klage'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'

export const KlageInitiellVurdering = (props: { klage: Klage }) => {
  const klage = props.klage

  const [begrunnelse, setBegrunnelse] = useState<string>('')
  const [utfall, setUtfall] = useState<Utfall>()
  const [redigerbar, setRedigerbar] = useState<boolean>(!klage?.initieltUtfall)

  const [lagreInitiellStatus, lagreInitiellKlageUtfall] = useApiCall(oppdaterInitieltUtfallForKlage)
  const harInitieltUtfall = klage.initieltUtfall

  const getTextFromutfall = (utfall: Utfall): string => {
    switch (utfall) {
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
      {redigerbar && (
        <>
          <Heading level="1" size="large">
            Første vurdering av utfall saksbehandler
          </Heading>
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
                  lagreInitiellKlageUtfall({
                    klageId: klage?.id,
                    utfallMedBegrunnelse: { utfall: utfall, begrunnelse: begrunnelse },
                  })
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
      )}
      <Button type="button" variant="secondary" onClick={() => setRedigerbar(true)}>
        Rediger
      </Button>

      {harInitieltUtfall && <KlageVurderingRedigering klage={klage} />}
    </>
  )
}
