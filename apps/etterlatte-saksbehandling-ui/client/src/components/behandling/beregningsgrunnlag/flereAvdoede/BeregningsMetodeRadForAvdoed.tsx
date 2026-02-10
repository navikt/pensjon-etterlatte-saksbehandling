import React, { useEffect, useState } from 'react'
import { addMonths, format, startOfDay, startOfMonth } from 'date-fns'
import {
  mapListeFraDto,
  mapListeTilDto,
  PeriodisertBeregningsgrunnlag,
  periodisertBeregningsgrunnlagFraDto,
  periodisertBeregningsgrunnlagTilDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import {
  BeregningsGrunnlagDto,
  BeregningsMetode,
  BeregningsmetodeForAvdoed,
  LagreBeregningsGrunnlagDto,
  toLagreBeregningsGrunnlagDto,
} from '~shared/types/Beregning'
import {
  BodyShort,
  Box,
  Button,
  Heading,
  HStack,
  Radio,
  ReadMore,
  Table,
  Tag,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import { FloppydiskIcon, PencilIcon, TrashIcon, XMarkIcon } from '@navikt/aksel-icons'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregningsGrunnlag,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { mapNavn, tagTekstForKunEnJuridiskForelder } from '~components/behandling/beregningsgrunnlag/Beregningsgrunnlag'
import { AnnenForelderVurdering } from '~shared/types/grunnlag'
import { ApiErrorAlert } from '~ErrorBoundary'

interface Props {
  behandling: IBehandlingReducer
  trygdetid: ITrygdetid
  redigerbar: boolean
  erTidligsteAvdoede: boolean
}

export const BeregningsMetodeRadForAvdoed = ({ behandling, trygdetid, redigerbar, erTidligsteAvdoede }: Props) => {
  const dispatch = useAppDispatch()
  const [redigerModus, setRedigerModus] = useState<boolean>(false)
  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)
  const personopplysninger = usePersonopplysninger()

  const erEnesteJuridiskeForelder = erTidligsteAvdoede && !!behandling.beregningsGrunnlag?.kunEnJuridiskForelder
  const kunEnJuridiskForelderPersongalleri =
    personopplysninger?.annenForelder?.vurdering === AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER

  const maanedEtterDoedsfall = (): Date | undefined => {
    const opplysning = personopplysninger?.avdoede?.find(
      (personOpplysning) => personOpplysning.opplysning.foedselsnummer === trygdetid.ident
    )?.opplysning

    return opplysning?.doedsdato ? addMonths(startOfMonth(opplysning.doedsdato), 1) : undefined
  }

  const defaultBeregningsMetodeFormData = (
    beregningsmetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined
  ): BeregningsmetodeForAvdoedForm => ({
    fom: beregningsmetode?.fom ?? maanedEtterDoedsfall() ?? new Date(),
    tom: beregningsmetode?.tom ?? undefined,
    data: {
      beregningsMetode: beregningsmetode?.data.beregningsMetode ?? {
        beregningsMetode: null,
        begrunnelse: null,
      },
      avdoed: trygdetid.ident,
    },
    datoTilKunEnJuridiskForelder:
      erTidligsteAvdoede && !!behandling?.beregningsGrunnlag?.kunEnJuridiskForelder
        ? periodisertBeregningsgrunnlagFraDto(behandling.beregningsGrunnlag.kunEnJuridiskForelder).tom
        : undefined,
  })

  const beregningsmetodeFormdataForAvdoed = () => {
    const metode = finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)
    return defaultBeregningsMetodeFormData(metode)
  }

  const finnPeriodisertBeregningsmetodeForAvdoed = (
    ident: string
  ): PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined => {
    if (behandling?.beregningsGrunnlag && !!behandling?.beregningsGrunnlag.beregningsMetodeFlereAvdoede?.length) {
      return mapListeFraDto(behandling.beregningsGrunnlag.beregningsMetodeFlereAvdoede)?.find(
        (grunnlag) => grunnlag?.data.avdoed === ident
      )
    }
    return undefined
  }

  function lagre(grunnlag: LagreBeregningsGrunnlagDto, onSuccess?: (grunnlag: BeregningsGrunnlagDto) => void) {
    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag,
      },
      (result) => {
        dispatch(oppdaterBeregningsGrunnlag(result))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
        setRedigerModus(false)
        if (!!onSuccess) onSuccess(result)
      }
    )
  }

  const beregningsmetodeFormdataToMetode = (
    formdata: BeregningsmetodeForAvdoedForm
  ): PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> => {
    return {
      fom: formdata.fom,
      tom: formdata.tom,
      data: formdata.data,
    }
  }

  const oppdaterBeregningsMetodeForAvdoed = (beregningsmetodeFormData: BeregningsmetodeForAvdoedForm) => {
    const nyMetode = beregningsmetodeFormdataToMetode(beregningsmetodeFormData)

    lagre({
      ...toLagreBeregningsGrunnlagDto(behandling?.beregningsGrunnlag),
      beregningsMetodeFlereAvdoede: !!behandling?.beregningsGrunnlag?.beregningsMetodeFlereAvdoede?.length
        ? behandling?.beregningsGrunnlag.beregningsMetodeFlereAvdoede
            .filter((metode) => metode.data.avdoed !== nyMetode.data.avdoed)
            .concat(mapListeTilDto([nyMetode]))
        : mapListeTilDto([nyMetode]),
      kunEnJuridiskForelder: kunEnJuridiskForelderPersongalleri
        ? erTidligsteAvdoede
          ? periodisertBeregningsgrunnlagTilDto({
              fom: nyMetode.fom,
              tom: beregningsmetodeFormData.datoTilKunEnJuridiskForelder,
              data: {},
            })
          : behandling.beregningsGrunnlag?.kunEnJuridiskForelder
        : undefined,
    })
  }

  function slettBeregningsMetodeForAvdoed() {
    lagre({
      ...toLagreBeregningsGrunnlagDto(behandling?.beregningsGrunnlag),
      beregningsMetodeFlereAvdoede: !!behandling?.beregningsGrunnlag?.beregningsMetodeFlereAvdoede?.length
        ? behandling?.beregningsGrunnlag.beregningsMetodeFlereAvdoede.filter(
            (metode) => metode.data.avdoed !== trygdetid.ident
          )
        : [],
      kunEnJuridiskForelder: erTidligsteAvdoede ? undefined : behandling?.beregningsGrunnlag?.kunEnJuridiskForelder,
    })
  }

  const beregningsMetodeForAvdoed: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined =
    finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)

  const { register, control, getValues, handleSubmit, reset } = useForm<BeregningsmetodeForAvdoedForm>({
    defaultValues: beregningsmetodeFormdataForAvdoed(),
  })

  useEffect(() => {
    if (!beregningsMetodeForAvdoed) {
      reset(defaultBeregningsMetodeFormData(undefined))
    }
  }, [behandling])

  const validerFom = (value: Date): string | undefined => {
    const fom = startOfDay(new Date(value))
    const tom = getValues().tom ? startOfDay(new Date(getValues().tom!)) : undefined

    if (tom && fom > tom) {
      return 'Fra-måned kan ikke være etter til-måned.'
    }

    return undefined
  }

  return (
    <Table.ExpandableRow
      open={redigerModus}
      onOpenChange={(open) => {
        setRedigerModus(open)
      }}
      key={trygdetid.ident}
      content={
        redigerbar ? (
          <>
            <form onSubmit={handleSubmit(oppdaterBeregningsMetodeForAvdoed)}>
              <VStack gap="space-4">
                {erTidligsteAvdoede && kunEnJuridiskForelderPersongalleri && (
                  <ControlledMaanedVelger
                    name="datoTilKunEnJuridiskForelder"
                    label="Til og med dato for kun én juridisk forelder(Valgfritt)"
                    description="Siste måneden med kun én juridisk forelder"
                    control={control}
                    fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
                  />
                )}

                <ControlledRadioGruppe
                  name="data.beregningsMetode.beregningsMetode"
                  control={control}
                  legend="Trygdetid i beregning"
                  errorVedTomInput="Du må velge en metode"
                  radios={
                    <>
                      <Radio value={BeregningsMetode.NASJONAL}>Nasjonal beregning (folketrygdberegning)</Radio>
                      <Radio value={BeregningsMetode.PRORATA}>
                        Prorata (EØS/avtaleland, der rettighet er oppfylt ved sammenlegging)
                      </Radio>
                      <Radio value={BeregningsMetode.BEST}>
                        Den som gir høyest verdi av nasjonal/prorata (EØS/avtale-land, der rettighet er oppfylt etter
                        nasjonale regler)
                      </Radio>
                    </>
                  }
                />
                <Heading size="xsmall" level="4">
                  Gyldig for beregning
                </Heading>
                <BodyShort>
                  Disse datoene brukes til å regne ut satsen for barnepensjon ut ifra om det er en eller to forelder
                  død.
                </BodyShort>
                <HStack gap="space-4">
                  <ControlledMaanedVelger
                    name="fom"
                    label="Fra og med"
                    description="Måned etter dødsfall"
                    control={control}
                    required
                    validate={validerFom}
                  />
                  <VStack>
                    <ControlledMaanedVelger
                      name="tom"
                      label="Til og med (valgfritt)"
                      description="Siste måneden med foreldrerett"
                      control={control}
                    />
                    <ReadMore header="Når skal du oppgi til og med dato">
                      <Box maxWidth="30rem">
                        Beregningen gjelder for perioden der den avdøde regnes som forelder for barnet. Hvis barnet har
                        blitt adoptert skal datoen “til og med” oppgis. Velg forelderen med dårligst trygdetid hvis det
                        kun er én adoptivforelder.
                      </Box>
                    </ReadMore>
                  </VStack>
                </HStack>
                <Box width="15rem">
                  <Textarea {...register('data.beregningsMetode.begrunnelse')} label="Begrunnelse (valgfritt)" />
                </Box>
                <HStack gap="space-4">
                  <Button
                    size="small"
                    icon={<FloppydiskIcon aria-hidden />}
                    loading={isPending(lagreBeregningsgrunnlagResult)}
                  >
                    Lagre
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    size="small"
                    icon={<XMarkIcon aria-hidden />}
                    onClick={() => {
                      reset()
                      setRedigerModus(false)
                    }}
                  >
                    Avbryt
                  </Button>
                </HStack>
              </VStack>
            </form>
          </>
        ) : (
          <>
            <Heading size="small" level="4">
              Begrunnelse
            </Heading>
            <BodyShort>{beregningsMetodeForAvdoed?.data.beregningsMetode.begrunnelse || '-'}</BodyShort>
          </>
        )
      }
    >
      <Table.DataCell>
        {mapNavn(trygdetid.ident, personopplysninger)}{' '}
        {erEnesteJuridiskeForelder && (
          <Tag data-color="meta-purple" variant="outline" size="small">
            {tagTekstForKunEnJuridiskForelder(behandling)}
          </Tag>
        )}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.data.beregningsMetode.beregningsMetode
          ? formaterEnumTilLesbarString(beregningsMetodeForAvdoed.data.beregningsMetode.beregningsMetode)
          : 'Metode er ikke satt'}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.fom ? format(startOfMonth(beregningsMetodeForAvdoed.fom), 'yyyy-MM-dd') : '-'}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.tom ? format(startOfMonth(beregningsMetodeForAvdoed.tom), 'yyyy-MM-dd') : '-'}
      </Table.DataCell>
      <Table.DataCell>
        {redigerbar && (
          <>
            <Button
              type="button"
              variant="secondary"
              size="small"
              icon={<PencilIcon aria-hidden />}
              disabled={redigerModus}
              onClick={() => setRedigerModus(true)}
            >
              {beregningsMetodeForAvdoed ? 'Rediger' : 'Legg til'}
            </Button>
          </>
        )}
      </Table.DataCell>
      <Table.DataCell>
        {redigerbar && beregningsMetodeForAvdoed && (
          <Button
            size="small"
            variant="secondary"
            icon={<TrashIcon aria-hidden />}
            loading={isPending(lagreBeregningsgrunnlagResult)}
            onClick={slettBeregningsMetodeForAvdoed}
          >
            Slett
          </Button>
        )}
        {mapFailure(lagreBeregningsgrunnlagResult, (error) => (
          <ApiErrorAlert>{error.detail || 'Kunne ikke lagre beregningsgrunnlag'}</ApiErrorAlert>
        ))}
      </Table.DataCell>
    </Table.ExpandableRow>
  )
}

interface BeregningsmetodeForAvdoedForm {
  fom: Date
  tom?: Date
  data: BeregningsmetodeForAvdoed
  datoTilKunEnJuridiskForelder?: Date
}
