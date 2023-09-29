import { Button, Label, Radio, RadioGroup, Select } from '@navikt/ds-react'
import {
  Tilbakekreving,
  TilbakekrevingAarsak,
  TilbakekrevingAktsomhet,
  TilbakekrevingVurdering,
} from '~shared/types/Tilbakekreving'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import React, { useState } from 'react'
import { lagreTilbakekrevingsvurdering } from '~shared/api/tilbakekreving'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import styled from 'styled-components'
import { useAppDispatch } from '~store/Store'

export function TilbakekrevingVurderingOverordnet({ tilbakekreving }: { tilbakekreving: Tilbakekreving }) {
  const dispatch = useAppDispatch()
  const [lagreVurderingStatus, lagreVurderingRequest] = useApiCall(lagreTilbakekrevingsvurdering)
  const [vurdering, setVurdering] = useState<TilbakekrevingVurdering>(tilbakekreving.vurdering)

  const lagreVurdering = () => {
    // TODO validering?
    lagreVurderingRequest({ tilbakekrevingsId: tilbakekreving.id, vurdering }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
    })
  }

  return (
    <InnholdForm>
      <>
        <Label>Årsak</Label>
        <Select
          label="Aarsak"
          hideLabel={true}
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
          <option value={TilbakekrevingAarsak.ANNET}>Annet</option>
          <option value={TilbakekrevingAarsak.ARBHOYINNT}>Inntekt</option>
          <option value={TilbakekrevingAarsak.BEREGNFEIL}>Beregningsfeil</option>
          <option value={TilbakekrevingAarsak.DODSFALL}>Dødsfall</option>
          <option value={TilbakekrevingAarsak.EKTESKAP}>Eksteskap/Samboer med felles barn</option>
          <option value={TilbakekrevingAarsak.FEILREGEL}>Feil regelbruk</option>
          <option value={TilbakekrevingAarsak.FEILUFOREG}>TODO - Sanksjoner/opphør</option>
          <option value={TilbakekrevingAarsak.FLYTTUTLAND}>Flyttet utland</option>
          <option value={TilbakekrevingAarsak.IKKESJEKKYTELSE}>Ikke sjekket mot andre ytelse</option>
          <option value={TilbakekrevingAarsak.OVERSETTMLD}>Oversett melding fra bruker</option>
          <option value={TilbakekrevingAarsak.SAMLIV}>TODO - Samliv</option>
          <option value={TilbakekrevingAarsak.UTBFEILMOT}>Utbetaling til feil mottaker</option>
        </Select>
      </>
      <TextAreaWrapper>
        <>
          <Label>Beskriv feilutbetalingen</Label>
          <Beskrivelse>Gi en kort beskrivelse av bakgrunnen for feilutbetalingen og når ble den oppdaget.</Beskrivelse>
        </>
        <textarea
          value={vurdering.beskrivelse ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              beskrivelse: e.target.value,
            })
          }
        />
      </TextAreaWrapper>
      <>
        <Label>Vurder uaktsomhet</Label>
        <RadioGroup
          legend=""
          size="small"
          className="radioGroup"
          value={vurdering.aktsomhet ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              aktsomhet: TilbakekrevingAktsomhet[e as TilbakekrevingAktsomhet],
            })
          }
        >
          <div className="flex">
            <Radio value={TilbakekrevingAktsomhet.GOD_TRO}>God tro</Radio>
            <Radio value={TilbakekrevingAktsomhet.SIMPEL_UAKTSOMHET}>Simpel uaktsomhet</Radio>
            <Radio value={TilbakekrevingAktsomhet.GROV_UAKTSOMHET}>Grov uaktsomhet</Radio>
          </div>
        </RadioGroup>
      </>
      <TextAreaWrapper>
        <Label>Konklusjon</Label>
        <textarea
          value={vurdering.konklusjon ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              konklusjon: e.target.value,
            })
          }
        />
      </TextAreaWrapper>
      <Button variant="primary" onClick={lagreVurdering} loading={isPending(lagreVurderingStatus)}>
        Lagre vurdering
      </Button>
    </InnholdForm>
  )
}
const InnholdForm = styled.div`
  padding: 2em 2em 2em 4em;
  width: 25em;
`
const TextAreaWrapper = styled.div`
  display: grid;
  align-items: flex-end;
  margin-top: 1em;
  margin-bottom: 1em;

  textArea {
    margin-top: 1em;
    border-width: 1px;
    border-radius: 4px 4px 0 4px;
    height: 98px;
    text-indent: 4px;
    resize: none;
  }
`
const Beskrivelse = styled.div`
  margin: 10px 0;
`
