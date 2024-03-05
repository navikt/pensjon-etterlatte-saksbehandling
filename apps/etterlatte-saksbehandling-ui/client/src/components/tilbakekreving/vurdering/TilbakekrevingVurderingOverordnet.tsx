import { Alert, Button, Radio, RadioGroup, Select, Textarea, VStack } from '@navikt/ds-react'
import {
  teksterTilbakekrevingAarsak,
  teksterTilbakekrevingAktsomhet,
  teksterTilbakekrevingHjemmel,
  TilbakekrevingAarsak,
  TilbakekrevingAktsomhet,
  TilbakekrevingBehandling,
  TilbakekrevingHjemmel,
  TilbakekrevingVurdering,
} from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { lagreTilbakekrevingsvurdering } from '~shared/api/tilbakekreving'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'

import { isPending } from '~shared/api/apiUtils'
import { InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'

export function TilbakekrevingVurderingOverordnet({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const dispatch = useAppDispatch()
  const [lagreVurderingStatus, lagreVurderingRequest] = useApiCall(lagreTilbakekrevingsvurdering)
  const [visLagret, setVisLagret] = useState(false)
  const [vurdering, setVurdering] = useState<TilbakekrevingVurdering>(behandling.tilbakekreving.vurdering)

  const lagreVurdering = () => {
    // TODO validering?
    lagreVurderingRequest({ behandlingsId: behandling.id, vurdering }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
      setVisLagret(true)
    })
  }

  useEffect(() => {
    if (visLagret) {
      setTimeout(() => {
        setVisLagret(false)
      }, 5000)
    }
  }, [visLagret])

  return (
    <InnholdPadding>
      <VStack gap="8" style={{ width: '30em' }}>
        <Select
          label="Årsak"
          value={vurdering.aarsak ?? ''}
          onChange={(e) => {
            if (e.target.value == '') return
            setVurdering({
              ...vurdering,
              aarsak: TilbakekrevingAarsak[e.target.value as TilbakekrevingAarsak],
            })
          }}
        >
          <option value="">Velg..</option>
          {Object.values(TilbakekrevingAarsak).map((aarsak) => (
            <option key={aarsak} value={aarsak}>
              {teksterTilbakekrevingAarsak[aarsak]}
            </option>
          ))}
        </Select>

        <Textarea
          label="Beskriv feilutbetalingen"
          description="Gi en kort beskrivelse av bakgrunnen for feilutbetalingen og når ble den oppdaget."
          value={vurdering.beskrivelse ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              beskrivelse: e.target.value,
            })
          }
        />

        <RadioGroup
          legend={<div style={{ fontSize: 'large' }}>Vurder uaktsomhet</div>}
          size="small"
          className="radioGroup"
          value={vurdering.aktsomhet.aktsomhet ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              aktsomhet: {
                aktsomhet: TilbakekrevingAktsomhet[e as TilbakekrevingAktsomhet],
              },
            })
          }
        >
          <div className="flex">
            {Object.values(TilbakekrevingAktsomhet).map((aktsomhet) => (
              <Radio key={aktsomhet} value={aktsomhet}>
                {teksterTilbakekrevingAktsomhet[aktsomhet]}
              </Radio>
            ))}
          </div>
        </RadioGroup>

        {vurdering.aktsomhet.aktsomhet === TilbakekrevingAktsomhet.GROV_UAKTSOMHET && (
          <Textarea
            label="Gjør en strafferettslig vurdering (Valgfri)"
            value={vurdering.aktsomhet.strafferettsligVurdering ?? ''}
            onChange={(e) =>
              setVurdering({
                ...vurdering,
                aktsomhet: {
                  ...vurdering.aktsomhet,
                  strafferettsligVurdering: e.target.value,
                },
              })
            }
          />
        )}

        {vurdering.aktsomhet.aktsomhet &&
          [TilbakekrevingAktsomhet.SIMPEL_UAKTSOMHET, TilbakekrevingAktsomhet.GROV_UAKTSOMHET].includes(
            vurdering.aktsomhet.aktsomhet
          ) && (
            <>
              <Textarea
                label="Redusering av kravet"
                description="Finnes det grunner til å redusere kravet?"
                value={vurdering.aktsomhet.reduseringAvKravet ?? ''}
                onChange={(e) =>
                  setVurdering({
                    ...vurdering,
                    aktsomhet: {
                      ...vurdering.aktsomhet,
                      reduseringAvKravet: e.target.value,
                    },
                  })
                }
              />
              <Textarea
                label="Rentevurdering"
                value={vurdering.aktsomhet.rentevurdering ?? ''}
                onChange={(e) =>
                  setVurdering({
                    ...vurdering,
                    aktsomhet: {
                      ...vurdering.aktsomhet,
                      rentevurdering: e.target.value,
                    },
                  })
                }
              />
            </>
          )}

        <Textarea
          label="Konklusjon"
          value={vurdering.konklusjon ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              konklusjon: e.target.value,
            })
          }
        />

        <Select
          label="Hjemmel"
          value={vurdering.hjemmel ?? ''}
          onChange={(e) => {
            if (e.target.value == '') return
            setVurdering({
              ...vurdering,
              hjemmel: TilbakekrevingHjemmel[e.target.value as TilbakekrevingHjemmel],
            })
          }}
        >
          <option value="">Velg hjemmel</option>
          {Object.values(TilbakekrevingHjemmel).map((hjemmel) => (
            <option key={hjemmel} value={hjemmel}>
              {teksterTilbakekrevingHjemmel[hjemmel]}
            </option>
          ))}
        </Select>

        {redigerbar && (
          <VStack gap="5">
            <Button
              variant="primary"
              size="small"
              onClick={lagreVurdering}
              loading={isPending(lagreVurderingStatus)}
              style={{ maxWidth: '7.5em' }}
            >
              Lagre vurdering
            </Button>
            {visLagret && (
              <Alert size="small" variant="success" style={{ maxWidth: 'fit-content' }}>
                Vurdering lagret.
              </Alert>
            )}
          </VStack>
        )}
      </VStack>
    </InnholdPadding>
  )
}
