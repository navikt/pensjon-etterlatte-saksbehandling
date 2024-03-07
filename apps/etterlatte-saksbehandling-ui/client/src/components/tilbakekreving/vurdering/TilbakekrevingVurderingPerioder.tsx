import {
  teksterTilbakekrevingResultat,
  teksterTilbakekrevingSkyld,
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
import { Button, Heading, Select, Table, TextField, VStack } from '@navikt/ds-react'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { Toast } from '~shared/alerts/Toast'

export function TilbakekrevingVurderingPerioder({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
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
            <Table.HeaderCell style={{ width: '5em' }}>Måned</Table.HeaderCell>
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
              <Table.Row key={'beloeperRad' + index} style={{ alignItems: 'start' }}>
                <Table.DataCell key="maaned">{periode.maaned.toString()}</Table.DataCell>
                <Table.DataCell key="bruttoUtbetaling">{beloeper.bruttoUtbetaling} kr</Table.DataCell>
                <Table.DataCell key="nyBruttoUtbetaling">{beloeper.nyBruttoUtbetaling} kr</Table.DataCell>
                <Table.DataCell key="beregnetFeilutbetaling">
                  <TextField
                    label=""
                    hideLabel={true}
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
                <Table.DataCell key="skatteprosent">{beloeper.skatteprosent} %</Table.DataCell>
                <Table.DataCell key="bruttoTilbakekreving">
                  <TextField
                    label=""
                    hideLabel={true}
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
                    hideLabel={true}
                    value={beloeper.nettoTilbakekreving ?? ''}
                    pattern="[0-9]"
                    readOnly={!redigerbar}
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
                    hideLabel={true}
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
                    {Object.values(TilbakekrevingSkyld).map((skyld) => (
                      <option key={skyld} value={skyld}>
                        {teksterTilbakekrevingSkyld[skyld]}
                      </option>
                    ))}
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
                    {Object.values(TilbakekrevingResultat).map((resultat) => (
                      <option key={resultat} value={resultat}>
                        {teksterTilbakekrevingResultat[resultat]}
                      </option>
                    ))}
                  </Select>
                </Table.DataCell>
                <Table.DataCell key="tilbakekrevingsprosent">
                  <TextField
                    label=""
                    hideLabel={true}
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
                    hideLabel={true}
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
      {redigerbar && (
        <VStack gap="5">
          <Button
            style={{ marginTop: '1em', maxWidth: '8em' }}
            variant="primary"
            size="small"
            onClick={lagrePerioder}
            loading={isPending(lagrePerioderStatus)}
          >
            Lagre perioder
          </Button>
          {isSuccess(lagrePerioderStatus) && <Toast melding="Perioder lagret" />}
        </VStack>
      )}
    </InnholdPadding>
  )
}

const onChangeNumber = (e: React.ChangeEvent<HTMLInputElement>, onChange: (value: number | null) => void) => {
  onChange(isNaN(parseInt(e.target.value)) ? null : parseInt(e.target.value))
}
