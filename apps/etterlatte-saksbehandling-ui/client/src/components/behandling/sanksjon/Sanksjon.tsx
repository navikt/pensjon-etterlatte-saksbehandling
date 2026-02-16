import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingReducer, oppdaterAvkorting } from '~store/reducers/BehandlingReducer'
import { behandlingErRedigerbar, hasValue } from '~components/behandling/felles/utils'
import { isFailure, isPending, mapResult } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import {
  Alert,
  BodyShort,
  Box,
  Button,
  Detail,
  Heading,
  HStack,
  ReadMore,
  Select,
  Table,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import { aarFraDatoString, formaterDato, formaterMaanednavnAar } from '~utils/formatering/dato'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { useForm } from 'react-hook-form'
import { formatISO, isBefore, startOfDay } from 'date-fns'
import { hentSanksjon, lagreSanksjon, slettSanksjon } from '~shared/api/sanksjon'
import { TableBox } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'
import {
  ISanksjon,
  ISanksjonLagre,
  SanksjonType,
  tekstSanksjon,
  valgbareSanksjonstyper,
  visbareSanksjonstyper,
} from '~shared/types/sanksjon'
import { useAppDispatch } from '~store/Store'
import { hentAvkorting } from '~shared/api/avkorting'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { IBehandlingsType, virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

interface SanksjonDefaultValue {
  datoFom?: Date
  datoTom?: Date | null
  beskrivelse: string
  type: SanksjonType | ''
}

const sanksjonDefaultValue: SanksjonDefaultValue = {
  datoFom: undefined,
  datoTom: undefined,
  beskrivelse: '',
  type: '',
}

function tidligstSanksjonFom(sanksjoner?: ISanksjon[], behandling?: IBehandlingReducer): Date | undefined {
  if (behandling?.behandlingType === IBehandlingsType.REVURDERING && sanksjoner?.length) {
    // Gyldige fra og med er tidligste fom i eksisterende sanksjoner, eller virk hvis det er før
    const tidligsteFomEllerVirk = [...sanksjoner.map((sanksjon) => sanksjon.fom), behandling.virkningstidspunkt?.dato]
      .filter(hasValue)
      .sort((a, b) => new Date(a).getMilliseconds() - new Date(b).getMilliseconds())[0]
    return new Date(tidligsteFomEllerVirk)
  }
  if (behandling?.virkningstidspunkt?.dato) {
    return new Date(behandling.virkningstidspunkt.dato)
  }
  return undefined
}

export const Sanksjon = ({
  behandling,
  manglerInntektVirkAar,
}: {
  behandling: IBehandlingReducer
  manglerInntektVirkAar: boolean
}) => {
  const [lagreSanksjonResponse, lagreSanksjonRequest] = useApiCall(lagreSanksjon)
  const [hentSanksjonStatus, hentSanksjonRequest] = useApiCall(hentSanksjon)
  const [slettSanksjonStatus, slettSanksjonRequest] = useApiCall(slettSanksjon)
  const [sanksjoner, setSanksjoner] = useState<ISanksjon[]>()
  const [visForm, setVisForm] = useState(false)
  const [redigerSanksjonId, setRedigerSanksjonId] = useState('')
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [avkortingStatus, fetchAvkorting] = useApiCall(hentAvkorting)
  const dispatch = useAppDispatch()

  const redigerbar =
    behandlingErRedigerbar(behandling.status, behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter) &&
    behandling.revurderingsaarsak != Revurderingaarsak.ETTEROPPGJOER

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
      type: data.type as SanksjonType,
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
        fetchAvkorting(behandling.id, (hentetAvkorting) => dispatch(oppdaterAvkorting(hentetAvkorting)))
      }
    )
  }

  const slettEnkeltSanksjon = (behandlingId: string, sanksjonId: string) => {
    slettSanksjonRequest({ behandlingId, sanksjonId }, () => {
      hentSanksjoner()
      fetchAvkorting(behandling.id, (hentetAvkorting) => dispatch(oppdaterAvkorting(hentetAvkorting)))
    })
  }

  const hentSanksjoner = () => {
    hentSanksjonRequest(behandling.id, (res) => {
      setSanksjoner(res.filter((sanksjon) => visbareSanksjonstyper.includes(sanksjon.type)))
    })
  }

  useEffect(() => {
    if (!sanksjoner) {
      hentSanksjoner()
    }
  }, [])

  const validerFom = (value: Date): string | undefined => {
    const fom = new Date(value)
    const skjemaTom = getValues('datoTom')
    const tom = skjemaTom ? new Date(skjemaTom) : null

    if (tom && isBefore(tom, fom)) {
      return 'Fra-dato kan ikke være etter til-dato'
    } else if (
      behandling.behandlingType !== IBehandlingsType.REVURDERING &&
      behandling.virkningstidspunkt?.dato &&
      isBefore(startOfDay(fom), startOfDay(new Date(behandling.virkningstidspunkt.dato)))
    ) {
      return 'Fra-dato kan ikke være før virkningstidspunkt'
    }
    return undefined
  }

  const validerTom = (value: Date): string | undefined => {
    const tom = value ? new Date(value) : null
    const skjemaFom = getValues('datoFom')
    const fom = skjemaFom ? new Date(skjemaFom) : null

    if (fom && tom && isBefore(tom, fom)) {
      return 'Til-dato kan ikke være før fra-dato'
    }
    return undefined
  }

  return (
    <TableBox>
      {mapResult(hentSanksjonStatus, {
        pending: <Spinner label="Henter sanksjoner" />,
        error: <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>,
        success: () => (
          <VStack gap="4">
            <Heading size="small" level="2">
              Sanksjoner
            </Heading>
            {sanksjoner &&
              sanksjoner.some(
                (sanksjon) => aarFraDatoString(sanksjon.fom) === aarFraDatoString(virkningstidspunkt(behandling).dato)
              ) &&
              manglerInntektVirkAar && (
                <Alert variant="warning">
                  Det er ikke lagt inn en inntekt for året virkningstidspunkt gjelder fra. Hvis det ikke går å legge inn
                  inntekt for inneværende år over må sanksjon som har fra og med / til og med i virk år slettes før
                  inntekt kan legges inn, og så kan sanksjon legges inn.
                </Alert>
              )}

            <Box>
              <HjemmelLenke tittel="Folketrygdloven § 17-8" lenke="https://lovdata.no/pro/lov/1997-02-28-19/§17-8" />
              <BodyShort spacing>
                Når en bruker har en sanksjon for en periode, vil ikke omstillingsstønaden bli utbetalt. Sanksjon kan
                kun brukes frem i tid. Hvis det er restanse fra endringer i forventet årsinntekt vil heller ikke den bli
                hentet inn i sanksjonsperioden, men omfordelt på måneder etter sanksjon.
              </BodyShort>
              <ReadMore header="Når skal sanksjoner gis?">
                <BodyShort spacing>
                  Dersom den gjenlevende ikke følger opp aktivitetskravet i{' '}
                  <HjemmelLenke tittel="§ 17-7" lenke="https://lovdata.no/pro/lov/1997-02-28-19/§17-7" />, skal
                  omstillingsstønaden stanses inntil vilkårene for å motta ytelsen igjen er oppfylt.
                </BodyShort>
                <BodyShort spacing>
                  Dersom den gjenlevende uten rimelig grunn sier opp sin stilling, nekter å ta imot tilbudt arbeid,
                  unnlater å gjenoppta sitt arbeidsforhold etter endt foreldrepermisjon, nekter å delta i
                  arbeidsmarkedstiltak eller unnlater å møte ved innkalling til arbeids- og velferdsetaten, faller
                  omstillingsstønaden bort én måned.
                </BodyShort>
                <BodyShort>
                  Dersom den gjenlevende har gitt uriktige opplysninger om forhold som har betydning for retten til
                  ytelser etter dette kapitlet, og han eller hun var klar over eller burde vært klar over dette, kan
                  vedkommende utestenges fra rett til stønad i inntil tre måneder første gang og inntil seks måneder ved
                  gjentakelser. Det samme gjelder dersom den gjenlevende har unnlatt å gi opplysninger av betydning for
                  retten til ytelser.
                </BodyShort>
              </ReadMore>
            </Box>

            <TableBox>
              <Table className="table" zebraStripes size="medium">
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell>Fra dato</Table.HeaderCell>
                    <Table.HeaderCell>Til dato</Table.HeaderCell>
                    <Table.HeaderCell>Type sanksjon</Table.HeaderCell>
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
                          <Table.DataCell>{formaterMaanednavnAar(lagretSanksjon.fom)}</Table.DataCell>
                          <Table.DataCell>
                            {lagretSanksjon.tom ? formaterMaanednavnAar(lagretSanksjon.tom) : '-'}
                          </Table.DataCell>
                          <Table.DataCell>{tekstSanksjon[lagretSanksjon.type]}</Table.DataCell>
                          <Table.DataCell>{lagretSanksjon.beskrivelse}</Table.DataCell>
                          <Table.DataCell>
                            <BodyShort>{lagretSanksjon.opprettet.ident}</BodyShort>
                            <Detail>{`saksbehandler: ${formaterDato(lagretSanksjon.opprettet.tidspunkt)}`}</Detail>
                          </Table.DataCell>
                          <Table.DataCell>
                            {lagretSanksjon.endret ? (
                              <>
                                <BodyShort>{lagretSanksjon.endret.ident}</BodyShort>
                                <Detail>{`saksbehandler: ${formaterDato(lagretSanksjon.endret.tidspunkt)}`}</Detail>
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
                                      type: lagretSanksjon.type,
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
                      <Table.DataCell align="center" colSpan={redigerbar ? 7 : 6}>
                        Bruker har ingen sanksjoner
                      </Table.DataCell>
                    </Table.Row>
                  )}
                </Table.Body>
              </Table>
            </TableBox>

            {mapResult(avkortingStatus, {
              pending: 'Henter oppdatert avkortet ytelse',
            })}
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
                <VStack gap="4" align="start">
                  <HStack gap="4">
                    <ControlledMaanedVelger
                      label="Dato fra og med"
                      name="datoFom"
                      control={control}
                      fromDate={tidligstSanksjonFom(sanksjoner, behandling)}
                      validate={validerFom}
                      required
                    />
                    <ControlledMaanedVelger
                      label="Dato til og med (valgfri)"
                      name="datoTom"
                      control={control}
                      fromDate={tidligstSanksjonFom(sanksjoner, behandling)}
                      validate={validerTom}
                    />
                  </HStack>
                  <Select
                    {...register('type', {
                      required: { value: true, message: 'Du må velge sanksjonstype' },
                    })}
                    label="Type sanksjon"
                    error={errors.type?.message}
                  >
                    <option value="">Velg sanksjon</option>
                    {valgbareSanksjonstyper.map((type, index) => (
                      <option key={index} value={type}>
                        {tekstSanksjon[type as SanksjonType]}
                      </option>
                    ))}
                  </Select>
                  <Textarea
                    {...register('beskrivelse', {
                      required: { value: true, message: 'Må fylles ut' },
                    })}
                    label="Beskrivelse"
                    error={errors.beskrivelse?.message}
                  />
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
        ),
      })}
    </TableBox>
  )
}
