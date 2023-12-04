import {
  TilbakekrevingBehandling,
  TilbakekrevingBeloep,
  TilbakekrevingPeriode,
  TilbakekrevingResultat,
  TilbakekrevingSkyld,
} from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreTilbakekrevingsperioder } from '~shared/api/tilbakekreving'
import React, { useState } from 'react'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { Button, Heading, Select, Table, TextField } from '@navikt/ds-react'
import styled from 'styled-components'

import { isPending } from '~shared/api/apiUtils'

export function TilbakekrevingVurderingPerioder({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const dispatch = useAppDispatch()
  const [lagrePerioderStatus, lagrePerioderRequest] = useApiCall(lagreTilbakekrevingsperioder)
  const [perioder, setPerioder] = useState<TilbakekrevingPeriode[]>(behandling.tilbakekreving.perioder)

  const updateBeloeper = (index: number, oppdatertBeloep: TilbakekrevingBeloep) => {
    const oppdatert = perioder.map((periode, i) => (i === index ? { ...periode, ytelse: oppdatertBeloep } : periode))
    setPerioder(oppdatert)
  }

  const lagrePerioder = () => {
    // TODO validering?
    lagrePerioderRequest({ behandlingsId: behandling.id, perioder }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
    })
  }

  return (
    <InnholdPadding>
      <HeadingWrapper>
        <Heading level="2" size="medium">
          Utbetalinger
        </Heading>
      </HeadingWrapper>
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
            const beloeper = periode.ytelse
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
      <ButtonWrapper>
        <Button variant="primary" onClick={lagrePerioder} loading={isPending(lagrePerioderStatus)}>
          Lagre
        </Button>
      </ButtonWrapper>
    </InnholdPadding>
  )
}

const onChangeNumber = (e: React.ChangeEvent<HTMLInputElement>, onChange: (value: number | null) => void) => {
  onChange(isNaN(parseInt(e.target.value)) ? null : parseInt(e.target.value))
}

const ButtonWrapper = styled.div`
  margin-top: 1em;
`
