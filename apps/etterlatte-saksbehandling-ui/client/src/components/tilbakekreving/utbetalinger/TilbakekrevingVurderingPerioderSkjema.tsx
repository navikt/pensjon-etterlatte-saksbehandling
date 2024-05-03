import {
  teksterTilbakekrevingResultat,
  teksterTilbakekrevingSkyld,
  TilbakekrevingBehandling,
  TilbakekrevingPeriode,
  TilbakekrevingResultat,
  TilbakekrevingSkyld,
} from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreTilbakekrevingsperioder } from '~shared/api/tilbakekreving'
import React, { useEffect } from 'react'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import { Border, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { Alert, Button, Select, Table, TextField, VStack } from '@navikt/ds-react'

import { isPending, mapResult } from '~shared/api/apiUtils'
import { Toast } from '~shared/alerts/Toast'
import { format } from 'date-fns'
import { useForm } from 'react-hook-form'
import { FlexRow } from '~shared/styled'
import { useNavigate } from 'react-router'

export function TilbakekrevingVurderingPerioderSkjema({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [lagrePerioderStatus, lagrePerioderRequest] = useApiCall(lagreTilbakekrevingsperioder)
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    formState,
    reset,
  } = useForm<{ values: TilbakekrevingPeriode[] }>({
    defaultValues: { values: behandling.tilbakekreving.perioder },
  })

  useEffect(() => {
    if (formState.isDirty && Object.keys(formState.dirtyFields).length) {
      const delay = setTimeout(handleSubmit(lagrePerioder), 2000)

      return () => {
        clearTimeout(delay)
      }
    }
  }, [formState])

  const lagrePerioderOgFortsett = (data: { values: TilbakekrevingPeriode[] }) => {
    lagrePerioderRequest({ behandlingsId: behandling.id, perioder: data.values }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
      reset({ values: lagretTilbakekreving.tilbakekreving.perioder })
      navigate(`/tilbakekreving/${behandling?.id}/oppsummering`)
    })
  }

  const lagrePerioder = (data: { values: TilbakekrevingPeriode[] }) => {
    lagrePerioderRequest({ behandlingsId: behandling.id, perioder: data.values }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
      reset({ values: lagretTilbakekreving.tilbakekreving.perioder })
    })
  }

  return (
    <>
      <InnholdPadding>
        <Table className="table" zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell style={{ minWidth: '6em' }}>Måned</Table.HeaderCell>
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
            {watch().values.map((periode, index) => {
              const beloeper = periode.ytelse
              return (
                <Table.Row key={'beloeperRad' + index} style={{ alignItems: 'start' }}>
                  <Table.DataCell key="maaned">{format(periode.maaned, 'MMMM yyyy')}</Table.DataCell>
                  <Table.DataCell key="bruttoUtbetaling">{beloeper.bruttoUtbetaling} kr</Table.DataCell>
                  <Table.DataCell key="nyBruttoUtbetaling">{beloeper.nyBruttoUtbetaling} kr</Table.DataCell>
                  <Table.DataCell key="beregnetFeilutbetaling">
                    <TextField
                      {...register(`values.${index}.ytelse.beregnetFeilutbetaling`, {
                        pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                        maxLength: { value: 10, message: 'Beløp kan ikke ha flere enn 10 siffer' },
                      })}
                      label=""
                      error={errors.values && errors.values[index]?.ytelse?.beregnetFeilutbetaling?.message}
                      inputMode="numeric"
                      hideLabel={true}
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skatteprosent">{beloeper.skatteprosent} %</Table.DataCell>
                  <Table.DataCell key="bruttoTilbakekreving">
                    <TextField
                      {...register(`values.${index}.ytelse.bruttoTilbakekreving`, {
                        pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                        maxLength: { value: 10, message: 'Beløp kan ikke ha flere enn 10 siffer' },
                      })}
                      label=""
                      error={errors.values && errors.values[index]?.ytelse?.bruttoTilbakekreving?.message}
                      inputMode="numeric"
                      hideLabel={true}
                    />
                  </Table.DataCell>
                  <Table.DataCell key="nettoTilbakekreving">
                    <TextField
                      {...register(`values.${index}.ytelse.nettoTilbakekreving`, {
                        pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                        maxLength: { value: 10, message: 'Beløp kan ikke ha flere enn 10 siffer' },
                      })}
                      label=""
                      error={errors.values && errors.values[index]?.ytelse?.nettoTilbakekreving?.message}
                      hideLabel={true}
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skatt">
                    <TextField
                      {...register(`values.${index}.ytelse.skatt`, {
                        pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                        maxLength: { value: 10, message: 'Beløp kan ikke ha flere enn 10 siffer' },
                      })}
                      label=""
                      error={errors.values && errors.values[index]?.ytelse?.skatt?.message}
                      hideLabel={true}
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skyld">
                    <Select {...register(`values.${index}.ytelse.skyld`)} label="Skyld" hideLabel={true}>
                      <option value="">Velg..</option>
                      {Object.values(TilbakekrevingSkyld).map((skyld) => (
                        <option key={skyld} value={skyld}>
                          {teksterTilbakekrevingSkyld[skyld]}
                        </option>
                      ))}
                    </Select>
                  </Table.DataCell>
                  <Table.DataCell key="resultat">
                    <Select {...register(`values.${index}.ytelse.resultat`)} label="Resultat" hideLabel={true}>
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
                      {...register(`values.${index}.ytelse.tilbakekrevingsprosent`, {
                        pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                        min: { value: 0, message: 'Må være større enn 0' },
                        max: { value: 100, message: 'Kan ikke være større enn 100' },
                      })}
                      label=""
                      error={errors.values && errors.values[index]?.ytelse?.tilbakekrevingsprosent?.message}
                      hideLabel={true}
                    />
                  </Table.DataCell>
                  <Table.DataCell key="rentetillegg">
                    <TextField
                      {...register(`values.${index}.ytelse.rentetillegg`, {
                        pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                        maxLength: { value: 10, message: 'Beløp kan ikke ha flere enn 10 siffer' },
                      })}
                      label=""
                      error={errors.values && errors.values[index]?.ytelse?.rentetillegg?.message}
                      hideLabel={true}
                    />
                  </Table.DataCell>
                </Table.Row>
              )
            })}
          </Table.Body>
        </Table>
        {redigerbar && (
          <VStack gap="5">
            {mapResult(lagrePerioderStatus, {
              success: () => <Toast melding="Perioder lagret" position="bottom-center" />,
              error: (error) => <Alert variant="error">Kunne ikke lagre perioder: {error.detail}</Alert>,
            })}
          </VStack>
        )}
      </InnholdPadding>
      <Border style={{ marginTop: '3em' }} />
      <FlexRow $spacing={true} justify="center">
        {redigerbar ? (
          <Button
            style={{ marginTop: '1em', maxWidth: '8em' }}
            variant="primary"
            size="small"
            onClick={handleSubmit(lagrePerioderOgFortsett)}
            loading={isPending(lagrePerioderStatus)}
          >
            Lagre og fortsett
          </Button>
        ) : (
          <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/oppsummering`)}>
            Neste
          </Button>
        )}
      </FlexRow>
    </>
  )
}
