import { BodyShort, Button, Label, Radio, RadioGroup, Select } from '@navikt/ds-react'
import {
  Tilbakekreving,
  TilbakekrevingAarsak,
  TilbakekrevingAktsomhet,
  TilbakekrevingHjemmel,
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
            <Radio value={TilbakekrevingAktsomhet.GOD_TRO}>God tro</Radio>
            <Radio value={TilbakekrevingAktsomhet.SIMPEL_UAKTSOMHET}>Simpel uaktsomhet</Radio>
            <Radio value={TilbakekrevingAktsomhet.GROV_UAKTSOMHET}>Grov uaktsomhet</Radio>
          </div>
        </RadioGroup>
      </>
      {vurdering.aktsomhet.aktsomhet === TilbakekrevingAktsomhet.GROV_UAKTSOMHET && (
        <TextAreaWrapper>
          <Label>Gjør en strafferettslig vurdering (Valgfri)</Label>
          <textarea
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
        </TextAreaWrapper>
      )}
      {vurdering.aktsomhet.aktsomhet &&
        [TilbakekrevingAktsomhet.SIMPEL_UAKTSOMHET, TilbakekrevingAktsomhet.GROV_UAKTSOMHET].includes(
          vurdering.aktsomhet.aktsomhet
        ) && (
          <>
            <TextAreaWrapper>
              <Label>Redusering av kravet</Label>
              <BodyShort>Finnes det grunner til å redusere kravet?</BodyShort>
              <textarea
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
            </TextAreaWrapper>
            <TextAreaWrapper>
              <Label>Rentevurdering</Label>
              <textarea
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
            </TextAreaWrapper>
          </>
        )}
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
      <DropdownWrapper>
        <Label>Rettslig grunnlag</Label>
        <Select
          label="Hjemmel"
          hideLabel={true}
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
          <option value={TilbakekrevingHjemmel.ULOVFESTET}>Lovfestet</option>
          <option value={TilbakekrevingHjemmel.TJUETO_FEMTEN_EN_LEDD_EN}>& 22-15 1. ledd, 1. punktum</option>
          <option value={TilbakekrevingHjemmel.TJUETO_FEMTEN_EN_LEDD_TO_FORSETT}>
            & 22-15 1. ledd, 2. punktum (forsett)
          </option>
          <option value={TilbakekrevingHjemmel.TJUETO_FEMTEN_EN_LEDD_TO_UAKTSOMT}>
            & 22-15 1. ledd, 2. punktum (uaktsomt)
          </option>
          <option value={TilbakekrevingHjemmel.TJUETO_FEMTEN_FEM}>& 22-15 5. ledd</option>
          <option value={TilbakekrevingHjemmel.TJUETO_FEMTEN_SEKS}>& 22-15 6. ledd</option>
          <option value={TilbakekrevingHjemmel.TJUETO_SEKSTEN}>& 22-16</option>
        </Select>
      </DropdownWrapper>

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
const DropdownWrapper = styled.div`
  margin-top: 2em;
  margin-bottom: 2em;
`

const Beskrivelse = styled.div`
  margin: 10px 0;
`
