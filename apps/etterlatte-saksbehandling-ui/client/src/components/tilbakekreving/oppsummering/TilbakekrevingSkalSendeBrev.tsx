import { Alert, Button, Radio, VStack } from '@navikt/ds-react'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreSkalSendeBrev } from '~shared/api/tilbakekreving'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { RadioGroupLegend } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingSkjema'
import { Toast } from '~shared/alerts/Toast'

interface ISkalSendeBrev {
  skalSendeBrev: boolean
}

export function TilbakekrevingSkalSendeBrev({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const dispatch = useAppDispatch()
  const [lagreSkalSendeBrevStatus, lagreSkalSendeBrevRequest] = useApiCall(lagreSkalSendeBrev)
  const { handleSubmit, control } = useForm<ISkalSendeBrev>({
    defaultValues: {
      skalSendeBrev: behandling.sendeBrev,
    },
  })

  const skalSendeBrev = (skalSendeBrev: ISkalSendeBrev) => {
    lagreSkalSendeBrevRequest(
      { behandlingsId: behandling.id, skalSendeBrev: skalSendeBrev.skalSendeBrev },
      (lagretTilbakekreving) => {
        dispatch(addTilbakekreving(lagretTilbakekreving))
      }
    )
  }

  return (
    <div style={{ marginTop: '3rem' }}>
      <form onSubmit={handleSubmit(skalSendeBrev)}>
        <VStack gap="6">
          <ControlledRadioGruppe
            name="skalSendeBrev"
            control={control}
            legend={<RadioGroupLegend label="Skal det sendes brev for tilbakekrevingen?" />}
            size="small"
            readOnly={!redigerbar}
            radios={
              <>
                <Radio value={true}>Ja</Radio>
                <Radio value={false}>Nei</Radio>
              </>
            }
          />
          <Button
            loading={isPending(lagreSkalSendeBrevStatus)}
            type="submit"
            variant="primary"
            size="small"
            style={{ maxWidth: '7.5em' }}
          >
            Lagre
          </Button>
          {mapResult(lagreSkalSendeBrevStatus, {
            success: () => <Toast melding="Brevvalg lagret" />,
            error: (error) => <Alert variant="error">Kunne ikke lagre brevvalg: {error.detail}</Alert>,
          })}
        </VStack>
      </form>
    </div>
  )
}
