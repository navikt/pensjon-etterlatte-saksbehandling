import { Button, Heading, Label, Radio, RadioGroup, Select, Table, TextField } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React, { useState } from 'react'
import {
  Tilbakekreving,
  TilbakekrevingAarsak,
  TilbakekrevingAktsomhet,
  TilbakekrevingBeloep,
  TilbakekrevingPeriode,
  TilbakekrevingResultat,
  TilbakekrevingSkyld,
  TilbakekrevingVurdering,
} from '~shared/types/Tilbakekreving'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreTilbakekrevingsperioder, lagreTilbakekrevingsvurdering } from '~shared/api/tilbakekreving'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import styled from 'styled-components'

export function TilbakekrevingVurdering({ tilbakekreving }: { tilbakekreving: Tilbakekreving }) {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const [lagreVurderingStatus, lagreVurderingRequest] = useApiCall(lagreTilbakekrevingsvurdering)
  const [vurdering, setVurdering] = useState<TilbakekrevingVurdering>(tilbakekreving.vurdering)
  const [lagrePerioderStatus, lagrePerioderRequest] = useApiCall(lagreTilbakekrevingsperioder)
  const [perioder, setPerioder] = useState<TilbakekrevingPeriode[]>(tilbakekreving.perioder)

  const updateBeloeper = (index: number, oppdatertBeloep: TilbakekrevingBeloep) => {
    const oppdatert = perioder.map((periode, i) =>
      i === index ? { ...periode, ytelsebeloeper: oppdatertBeloep } : periode
    )
    setPerioder(oppdatert)
  }

  const lagreVurdering = () => {
    // TODO validering?
    lagreVurderingRequest({ tilbakekrevingsId: tilbakekreving.id, vurdering }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
    })
  }

  const lagrePerioder = () => {
    // TODO validering?
    lagrePerioderRequest({ tilbakekrevingsId: tilbakekreving.id, perioder }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
    })
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
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
            <Beskrivelse>
              Gi en kort beskrivelse av bakgrunnen for feilutbetalingen og når ble den oppdaget.
            </Beskrivelse>
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
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="2" size="medium">
            Utbetalinger
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <Table className="table" zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Måned</Table.HeaderCell>
              <Table.HeaderCell>Brutto utbetaling</Table.HeaderCell>
              <Table.HeaderCell>Ny brutto utbetaling</Table.HeaderCell>
              <Table.HeaderCell>Beregnet feilutbetaling</Table.HeaderCell>
              <Table.HeaderCell>Skatteprosent</Table.HeaderCell>
              <Table.HeaderCell>Brutto tilbakekreving</Table.HeaderCell>
              <Table.HeaderCell>Netto tilbakekreving</Table.HeaderCell>
              <Table.HeaderCell>Skatt</Table.HeaderCell>
              <Table.HeaderCell>Skyld</Table.HeaderCell>
              <Table.HeaderCell>Resultat</Table.HeaderCell>
              <Table.HeaderCell>Tilbakekrevingsprosent</Table.HeaderCell>
              <Table.HeaderCell>Rentetillegg</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {perioder.map((periode, index) => {
              const beloeper = periode.ytelsebeloeper
              return (
                <Table.Row key={'beloeperRad' + index}>
                  <Table.DataCell key="maaned">{periode.maaned.toString()}</Table.DataCell>
                  <Table.DataCell key="bruttoUtbetaling">{beloeper.bruttoUtbetaling}</Table.DataCell>
                  <Table.DataCell key="nyBruttoUtbetaling">{beloeper.nyBruttoUtbetaling}</Table.DataCell>
                  <Table.DataCell key="beregnetFeilutbetaling">
                    <TextField
                      label=""
                      placeholder="Beregnet feilutbetaling"
                      value={beloeper.beregnetFeilutbetaling ?? ''}
                      pattern="[0-9]{11}"
                      maxLength={10}
                      onChange={(e) =>
                        onChangeNumber(e, (value) => {
                          updateBeloeper(index, {
                            ...beloeper,
                            beregnetFeilutbetaling: value,
                          })
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skatteprosent">{beloeper.skatteprosent}</Table.DataCell>
                  <Table.DataCell key="bruttoTilbakekreving">
                    <TextField
                      label=""
                      placeholder="Brutto tilbakekreving"
                      value={beloeper.bruttoTilbakekreving ?? ''}
                      pattern="[0-9]"
                      onChange={(e) =>
                        onChangeNumber(e, (value) => {
                          updateBeloeper(index, {
                            ...beloeper,
                            bruttoTilbakekreving: value,
                          })
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="nettoTilbakekreving">
                    <TextField
                      label=""
                      placeholder="Netto tilbakekreving"
                      value={beloeper.nettoTilbakekreving ?? ''}
                      pattern="[0-9]"
                      onChange={(e) =>
                        onChangeNumber(e, (value) => {
                          updateBeloeper(index, {
                            ...beloeper,
                            nettoTilbakekreving: value,
                          })
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skatt">
                    <TextField
                      label=""
                      placeholder="Skatt"
                      value={beloeper.skatt ?? ''}
                      pattern="[0-9]"
                      onChange={(e) =>
                        onChangeNumber(e, (value) => {
                          updateBeloeper(index, {
                            ...beloeper,
                            skatt: value,
                          })
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skyld">
                    <Select
                      label="Skyld"
                      hideLabel={true}
                      value={beloeper.skyld ?? ''}
                      onChange={(e) => {
                        if (e.target.value === '') return
                        updateBeloeper(index, {
                          ...beloeper,
                          skyld: TilbakekrevingSkyld[e.target.value as TilbakekrevingSkyld],
                        })
                      }}
                    >
                      <option value="">Velg..</option>
                      <option value={TilbakekrevingSkyld.IKKE_FORDELT}>Ikke fordelt</option>
                      <option value={TilbakekrevingSkyld.BRUKER}>Bruker</option>
                      <option value={TilbakekrevingSkyld.NAV}>Nav</option>
                      <option value={TilbakekrevingSkyld.SKYLDDELING}>Skylddeling</option>
                    </Select>
                  </Table.DataCell>
                  <Table.DataCell key="resultat">
                    <Select
                      label="Resultat"
                      hideLabel={true}
                      value={beloeper.resultat ?? ''}
                      onChange={(e) => {
                        if (e.target.value === '') return
                        updateBeloeper(index, {
                          ...beloeper,
                          resultat: TilbakekrevingResultat[e.target.value as TilbakekrevingResultat],
                        })
                      }}
                    >
                      <option value="">Velg..</option>
                      <option value={TilbakekrevingResultat.FORELDET}>Foreldet</option>
                      <option value={TilbakekrevingResultat.INGEN_TILBAKEKREV}>Ingen tilbakekreving</option>
                      <option value={TilbakekrevingResultat.DELVIS_TILBAKEKREV}>Delvis tilbakekreving</option>
                      <option value={TilbakekrevingResultat.FULL_TILBAKEKREV}>Full tilbakekreving</option>
                    </Select>
                  </Table.DataCell>
                  <Table.DataCell key="tilbakekrevingsprosent">
                    <TextField
                      label=""
                      placeholder="Tilbakekrevingsprosent"
                      value={beloeper.tilbakekrevingsprosent ?? ''}
                      pattern="[0-9]"
                      maxLength={3}
                      onChange={(e) =>
                        onChangeNumber(e, (value) => {
                          updateBeloeper(index, {
                            ...beloeper,
                            tilbakekrevingsprosent: value,
                          })
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="rentetillegg">
                    <TextField
                      label=""
                      placeholder="Rentetillegg"
                      value={beloeper.rentetillegg ?? ''}
                      pattern="[0-9]"
                      onChange={(e) =>
                        onChangeNumber(e, (value) => {
                          updateBeloeper(index, {
                            ...beloeper,
                            rentetillegg: value,
                          })
                        })
                      }
                    />
                  </Table.DataCell>
                </Table.Row>
              )
            })}
          </Table.Body>
        </Table>
        <Button variant="primary" onClick={lagrePerioder} loading={isPending(lagrePerioderStatus)}>
          Lagre
        </Button>
      </InnholdPadding>
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/vedtak`)}>
          Gå til vedtak
        </Button>
      </FlexRow>
    </Content>
  )
}

const onChangeNumber = (e: React.ChangeEvent<HTMLInputElement>, onChange: (value: number | null) => void) => {
  onChange(isNaN(parseInt(e.target.value)) ? null : parseInt(e.target.value))
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
