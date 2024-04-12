import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { isPending, mapApiResult } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { BodyShort, Button, Detail, Heading, HStack, Table, TextField, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import { formaterStringDato } from '~utils/formattering'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { useForm } from 'react-hook-form'
import { startOfDay, formatISO } from 'date-fns'
import { hentSanksjon, lagreSanksjon } from '~shared/api/sanksjon'
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

export const Sanksjon = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const [lagreSanksjonResponse, lagreSanksjonRequest] = useApiCall(lagreSanksjon)
  const [sanksjonStatus, hentSanksjonRequest] = useApiCall(hentSanksjon)
  const [sanksjoner, setSanksjoner] = useState<ISanksjon[]>()
  const [visForm, setVisForm] = useState(false)
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
    getValues,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      datoFom: undefined,
      datoTom: undefined,
      beskrivelse: '',
    },
  })

  const validerFom = (value: Date): string | undefined => {
    const fom = startOfDay(new Date(value))
    const tom = startOfDay(new Date(getValues().datoTom!))

    if (!value) {
      return 'Fra-måned må settes'
    } else if (fom > tom) {
      return 'Fra-måned kan ikke være etter til-måned.'
    } else if (behandling.virkningstidspunkt?.dato && fom < startOfDay(new Date(behandling.virkningstidspunkt.dato))) {
      return 'Fra-måned før virkningstidspunkt.'
    }
    return undefined
  }

  const validerTom = (value: Date): string | undefined => {
    const fom = startOfDay(new Date(getValues().datoFom!))
    const tom = startOfDay(new Date(value))

    if (fom > tom) {
      return 'Til-måned kan ikke være før Fra-måned'
    }
    return undefined
  }

  const submitSanksjon = (data: { datoFom?: Date; datoTom?: Date; beskrivelse: string }) => {
    const lagreSanksjon: ISanksjonLagre = {
      id: undefined,
      sakId: behandling.sakId,
      fom: formatISO(data.datoFom!, { representation: 'date' }),
      tom: data.datoTom ? formatISO(data.datoTom!, { representation: 'date' }) : undefined,
      beskrivelse: data.beskrivelse,
    }

    lagreSanksjonRequest(
      {
        behandlingId: behandling.id,
        sanksjon: lagreSanksjon,
      },
      () => {
        reset()
        hentSanksjoner()
        setVisForm(false)
      }
    )
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

  return (
    <SanksjonWrapper>
      {mapApiResult(
        sanksjonStatus,
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
                        </Table.Row>
                      ))}
                    </>
                  ) : (
                    <Table.Row>
                      <Table.DataCell align="center" colSpan={5}>
                        Bruker har ingen sanksjoner
                      </Table.DataCell>
                    </Table.Row>
                  )}
                </Table.Body>
              </Table>
            </TableWrapper>

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
                      validate={validerFom}
                      required
                    />
                    <ControlledMaanedVelger
                      label="Dato til og med"
                      name="datoTom"
                      control={control}
                      validate={validerTom}
                    />
                    <TextField
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
                      icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
                      onClick={(e) => {
                        e.preventDefault()
                        setVisForm(false)
                      }}
                    >
                      Avbryt
                    </Button>
                    <Button
                      size="small"
                      variant="primary"
                      icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
                      type="submit"
                    >
                      Lagre
                    </Button>
                  </HStack>
                </VStack>
              </form>
            )}

            {!visForm && redigerbar && (
              <HStack>
                <Button
                  size="small"
                  variant="secondary"
                  icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
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
