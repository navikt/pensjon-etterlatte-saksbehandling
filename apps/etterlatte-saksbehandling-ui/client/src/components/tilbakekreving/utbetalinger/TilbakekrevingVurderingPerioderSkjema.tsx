import {
  teksterTilbakekrevingResultat,
  teksterTilbakekrevingSkyld,
  TilbakekrevingBehandling,
  TilbakekrevingPeriode,
  TilbakekrevingResultat,
  TilbakekrevingSkyld,
} from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreTilbakekrevingsperioder, oppdaterTilbakekrevingKravgrunnlag } from '~shared/api/tilbakekreving'
import React, { useEffect } from 'react'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import { Box, Button, HStack, Select, Table, TextField, VStack } from '@navikt/ds-react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { Toast } from '~shared/alerts/Toast'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { FixedAlert } from '~shared/alerts/FixedAlert'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'
import { formaterMaanedDato } from '~utils/formatering/dato'

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
  const [oppdaterKravgrunnlagStatus, oppdaterKravgrunnlagRequest] = useApiCall(oppdaterTilbakekrevingKravgrunnlag)
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    formState,
    reset,
    getValues,
  } = useForm<{ values: TilbakekrevingPeriode[] }>({
    defaultValues: { values: behandling.tilbakekreving.perioder },
  })

  useEffect(() => {
    if (formState.isDirty && Object.keys(formState.dirtyFields).length) {
      const delay = setTimeout(handleSubmit(autolagrePerioder), 2000)

      return () => {
        clearTimeout(delay)
      }
    }
  }, [formState])

  const autolagrePerioder = (data: { values: TilbakekrevingPeriode[] }) => {
    lagrePerioder(data)
  }

  const lagrePerioderOgFortsett = (data: { values: TilbakekrevingPeriode[] }) => {
    lagrePerioder(data, () => navigate(`/tilbakekreving/${behandling?.id}/oppsummering`))
  }

  const lagrePerioder = (data: { values: TilbakekrevingPeriode[] }, onSuccess?: () => void) => {
    reset(getValues())
    lagrePerioderRequest(
      { tilbakekrevingId: behandling.id, perioder: data.values },
      (lagretTilbakekreving) => {
        dispatch(addTilbakekreving(lagretTilbakekreving))
        reset({ values: lagretTilbakekreving.tilbakekreving.perioder }, { keepDirtyValues: true })
        onSuccess?.()
      },
      () => {
        reset(getValues())
      }
    )
  }

  const oppdaterKravgrunnlag = () => {
    reset(getValues())
    oppdaterKravgrunnlagRequest(
      { tilbakekrevingId: behandling.id },
      (oppdatertTilbakekreving) => {
        dispatch(addTilbakekreving(oppdatertTilbakekreving))
        reset({ values: oppdatertTilbakekreving.tilbakekreving.perioder })
      },
      () => {
        reset(getValues())
      }
    )
  }

  return (
    <>
      <Box paddingBlock="8" paddingInline="16 8">
        <VStack gap="10">
          <div>
            <Button
              variant="secondary"
              icon={<ArrowsCirclepathIcon />}
              iconPosition="right"
              loading={isPending(oppdaterKravgrunnlagStatus)}
              size="small"
              onClick={oppdaterKravgrunnlag}
            >
              Oppdater perioder fra kravgrunnlag
            </Button>
          </div>
          {mapResult(oppdaterKravgrunnlagStatus, {
            success: () => <Toast melding="Periodene ble oppdatert" position="bottom-center"></Toast>,
            error: (error) => (
              <ApiErrorAlert>En feil oppstod under henting av perioder fra kravgrunnlag: {error.detail}</ApiErrorAlert>
            ),
          })}

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
                    <Table.DataCell key="maaned">{formaterMaanedDato(periode.maaned)}</Table.DataCell>
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
                      <Select
                        {...register(`values.${index}.ytelse.resultat`, {
                          validate: (value) => {
                            return value
                              ? Object.values(TilbakekrevingResultat).includes(value) || `Feil type: ${value}`
                              : 'Må velge verdi'
                          },
                        })}
                        label="Resultat"
                        hideLabel={true}
                        error={errors.values && errors.values[index]?.ytelse?.resultat?.message}
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
        </VStack>
        {redigerbar && (
          <VStack gap="5">
            {mapResult(lagrePerioderStatus, {
              success: () => <Toast melding="Perioder lagret" position="bottom-center" />,
              error: (error) => (
                <FixedAlert
                  variant="error"
                  melding={`Kunne ikke lagre perioder: ${error.detail}`}
                  position="bottom-center"
                />
              ),
            })}
          </VStack>
        )}
      </Box>
      <Box paddingBlock="12 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        <HStack justify="center">
          {redigerbar ? (
            <Button
              style={{ marginTop: '1em' }}
              variant="primary"
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
        </HStack>
      </Box>
    </>
  )
}
