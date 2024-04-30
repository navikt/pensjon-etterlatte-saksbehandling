import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { isFailure, isPending, mapApiResult } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { Alert, BodyShort, Button, Detail, Heading, HStack, Table, Textarea, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import { formaterStringDato } from '~utils/formattering'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { useForm } from 'react-hook-form'
import { formatISO, isBefore, startOfDay } from 'date-fns'
import { hentSanksjon, lagreSanksjon, slettSanksjon } from '~shared/api/sanksjon'
import { TableWrapper } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'

export interface ISanksjon {
  id?: string
  behandlingId: string
  sakId: number
  fom: string
  tom?: string
  opprettet: {
    tidspunkt: string
    ident: string
  }
  endret?: {
    tidspunkt: string
    ident: string
  }
  beskrivelse: string
}

export interface ISanksjonLagre {
  id?: string
  sakId: number
  fom: string
  tom?: string
  beskrivelse: string
}

interface SanksjonDefaultValue {
  datoFom?: Date
  datoTom?: Date | null
  beskrivelse: string
}

const sanksjonDefaultValue: SanksjonDefaultValue = {
  datoFom: undefined,
  datoTom: undefined,
  beskrivelse: '',
}

export const Sanksjon = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const [lagreSanksjonResponse, lagreSanksjonRequest] = useApiCall(lagreSanksjon)
  const [hentSanksjonStatus, hentSanksjonRequest] = useApiCall(hentSanksjon)
  const [slettSanksjonStatus, slettSanksjonRequest] = useApiCall(slettSanksjon)
  const [sanksjoner, setSanksjoner] = useState<ISanksjon[]>()
  const [visForm, setVisForm] = useState(false)
  const [redigerSanksjonId, setRedigerSanksjonId] = useState('')
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const {
    register,
    handleSubmit,
    control,
    reset,
    getValues,
    formState: { errors },
  } = useForm<SanksjonDefaultValue>({
    defaultValues: sanksjonDefaultValue,
  })

  const submitSanksjon = (data: SanksjonDefaultValue) => {
    const { datoFom, datoTom, beskrivelse } = data

    const lagreSanksjon: ISanksjonLagre = {
      id: redigerSanksjonId ? redigerSanksjonId : '',
      sakId: behandling.sakId,
      fom: formatISO(datoFom!, { representation: 'date' }),
      tom: datoTom ? formatISO(datoTom!, { representation: 'date' }) : undefined,
      beskrivelse: beskrivelse,
    }

    lagreSanksjonRequest(
      {
        behandlingId: behandling.id,
        sanksjon: lagreSanksjon,
      },
      () => {
        reset(sanksjonDefaultValue)
        hentSanksjoner()
        setRedigerSanksjonId('')
        setVisForm(false)
      }
    )
  }

  const slettEnkeltSanksjon = (behandlingId: string, sanksjonId: string) => {
    slettSanksjonRequest({ behandlingId, sanksjonId }, () => {
      hentSanksjoner()
    })
  }

  const hentSanksjoner = () => {
    hentSanksjonRequest(behandling.id, (res) => {
      setSanksjoner(res)
    })
  }

  useEffect(() => {
    if (!sanksjoner) {
      hentSanksjoner()
    }
  }, [])

  const validerFom = (value: Date): string | undefined => {
    const fom = new Date(value)
    const tom = getValues().datoTom ? new Date(getValues().datoTom!) : null

    if (tom && isBefore(tom, fom)) {
      return 'Til dato må være etter Fra dato'
    } else if (
      behandling.virkningstidspunkt?.dato &&
      isBefore(startOfDay(fom), startOfDay(new Date(behandling.virkningstidspunkt.dato)))
    ) {
      return 'Fra dato kan ikke være før virkningstidspunkt'
    }
    return undefined
  }

  const validerTom = (value: Date): string | undefined => {
    const tom = value ? new Date(value) : null
    const fom = getValues().datoFom ? new Date(getValues().datoFom!) : null

    if (fom && tom && isBefore(tom, fom)) {
      return 'Til dato må være etter Fra dato'
    }
    return undefined
  }

  const sanksjonFraDato = behandling.virkningstidspunkt?.dato ? new Date(behandling.virkningstidspunkt.dato) : undefined

  return (
    <SanksjonWrapper>
      {mapApiResult(
        hentSanksjonStatus,
        <Spinner visible label="Henter sanksjoner" />,
        () => (
          <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>
        ),
        () => (
          <VStack gap="4">
            <Heading spacing size="small" level="2">
              Sanksjoner
            </Heading>
            <BodyShort>Her kommer det informasjon om sanksjoner.</BodyShort>

            <TableWrapper marginBottom="1rem">
              <Table className="table" zebraStripes size="medium">
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell>Fra dato</Table.HeaderCell>
                    <Table.HeaderCell>Til dato</Table.HeaderCell>
                    <Table.HeaderCell>Beskrivelse</Table.HeaderCell>
                    <Table.HeaderCell>Registrert</Table.HeaderCell>
                    <Table.HeaderCell>Endret</Table.HeaderCell>
                    {redigerbar && <Table.HeaderCell />}
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {sanksjoner && sanksjoner.length > 0 ? (
                    <>
                      {sanksjoner.map((lagretSanksjon, index) => (
                        <Table.Row key={index}>
                          <Table.DataCell>{formaterStringDato(lagretSanksjon.fom)}</Table.DataCell>
                          <Table.DataCell>
                            {lagretSanksjon.tom ? formaterStringDato(lagretSanksjon.tom) : '-'}
                          </Table.DataCell>
                          <Table.DataCell>{lagretSanksjon.beskrivelse}</Table.DataCell>
                          <Table.DataCell>
                            <BodyShort>{lagretSanksjon.opprettet.ident}</BodyShort>
                            <Detail>{`saksbehandler: ${formaterStringDato(lagretSanksjon.opprettet.tidspunkt)}`}</Detail>
                          </Table.DataCell>
                          <Table.DataCell>
                            {lagretSanksjon.endret ? (
                              <>
                                <BodyShort>{lagretSanksjon.endret.ident}</BodyShort>
                                <Detail>{`saksbehandler: ${formaterStringDato(lagretSanksjon.endret.tidspunkt)}`}</Detail>
                              </>
                            ) : (
                              '-'
                            )}
                          </Table.DataCell>
                          {redigerbar && (
                            <Table.DataCell>
                              <HStack gap="2">
                                <Button
                                  size="small"
                                  variant="tertiary"
                                  onClick={() => {
                                    reset({
                                      datoFom: new Date(lagretSanksjon.fom),
                                      datoTom: lagretSanksjon.tom ? new Date(lagretSanksjon.tom) : null,
                                      beskrivelse: lagretSanksjon.beskrivelse,
                                    })
                                    setRedigerSanksjonId(lagretSanksjon.id!!)
                                    setVisForm(true)
                                  }}
                                >
                                  Rediger
                                </Button>
                                <Button
                                  size="small"
                                  variant="tertiary"
                                  onClick={() => {
                                    slettEnkeltSanksjon(lagretSanksjon.behandlingId, lagretSanksjon.id!!)
                                  }}
                                  loading={isPending(slettSanksjonStatus)}
                                >
                                  Slett
                                </Button>
                              </HStack>
                            </Table.DataCell>
                          )}
                        </Table.Row>
                      ))}
                    </>
                  ) : (
                    <Table.Row>
                      <Table.DataCell align="center" colSpan={6}>
                        Bruker har ingen sanksjoner
                      </Table.DataCell>
                    </Table.Row>
                  )}
                </Table.Body>
              </Table>
            </TableWrapper>

            {isFailure(slettSanksjonStatus) && (
              <Alert variant="error">
                {slettSanksjonStatus.error.detail || 'Det skjedde en feil ved sletting av sanksjon'}
              </Alert>
            )}

            {visForm && (
              <form onSubmit={handleSubmit(submitSanksjon)}>
                <Heading size="small" level="3" spacing>
                  Ny sanksjon
                </Heading>
                <VStack gap="4">
                  <HStack gap="4">
                    <ControlledMaanedVelger
                      label="Dato fra og med"
                      name="datoFom"
                      control={control}
                      fromDate={sanksjonFraDato}
                      validate={validerFom}
                      required
                    />
                    <ControlledMaanedVelger
                      label="Dato til og med (valgfri)"
                      name="datoTom"
                      control={control}
                      fromDate={sanksjonFraDato}
                      validate={validerTom}
                    />
                  </HStack>
                  <HStack>
                    <Textarea
                      {...register('beskrivelse', {
                        required: { value: true, message: 'Må fylles ut' },
                      })}
                      label="Beskrivelse"
                      error={errors.beskrivelse?.message}
                    />
                  </HStack>
                  <HStack gap="4">
                    <Button
                      size="small"
                      variant="secondary"
                      type="button"
                      onClick={(e) => {
                        e.preventDefault()
                        reset(sanksjonDefaultValue)
                        setRedigerSanksjonId('')
                        setVisForm(false)
                      }}
                    >
                      Avbryt
                    </Button>
                    <Button size="small" variant="primary" type="submit" loading={isPending(lagreSanksjonResponse)}>
                      Lagre
                    </Button>
                  </HStack>
                  {isFailure(lagreSanksjonResponse) && (
                    <Alert variant="error">
                      {lagreSanksjonResponse.error.detail || 'Det skjedde en feil ved lagring av sanksjon'}
                    </Alert>
                  )}
                </VStack>
              </form>
            )}

            {!visForm && redigerbar && (
              <HStack>
                <Button
                  size="small"
                  variant="secondary"
                  icon={<PencilIcon aria-hidden fontSize="1.5rem" />}
                  loading={isPending(lagreSanksjonResponse)}
                  onClick={(e) => {
                    e.preventDefault()
                    setVisForm(true)
                  }}
                >
                  Legg til sanksjon
                </Button>
              </HStack>
            )}
          </VStack>
        )
      )}
    </SanksjonWrapper>
  )
}

const SanksjonWrapper = styled.div`
  margin: 2em 0 1em 0;
`
