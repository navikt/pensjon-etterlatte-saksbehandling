import React, { useEffect, useState } from 'react'
import {
  mapListeFraDto,
  mapListeTilDto,
  PeriodisertBeregningsgrunnlag,
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
import { addDays, format, startOfDay, startOfMonth } from 'date-fns'
import { FloppydiskIcon, PencilIcon, TrashIcon, XMarkIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { IBehandlingReducer, oppdaterBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { formaterNavn } from '~shared/types/Person'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { formaterDato } from '~utils/formatering/dato'

interface Props {
  behandling: IBehandlingReducer
  trygdetid: ITrygdetid
  redigerbar: boolean
  erEnesteJuridiskeForelder: boolean
}

export const BeregningsMetodeRadForAvdoed = ({
  behandling,
  trygdetid,
  redigerbar,
  erEnesteJuridiskeForelder,
}: Props) => {
  const dispatch = useAppDispatch()
  const [redigerModus, setRedigerModus] = useState<boolean>(false)
  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)
  const personopplysninger = usePersonopplysninger()

  const mapNavn = (fnr: string): string => {
    if (!personopplysninger) return fnr

    const opplysning = personopplysninger.avdoede.find(
      (personOpplysning) => personOpplysning.opplysning.foedselsnummer === fnr
    )?.opplysning

    if (!opplysning) {
      return fnr
    }
    return `${formaterNavn(opplysning)} (${fnr})`
  }

  const defaultFormdata = () => ({
    fom: new Date(),
    tom: undefined,
    data: {
      beregningsMetode: {
        beregningsMetode: null,
        begrunnelse: null,
      },
      avdoed: trygdetid.ident,
    },
    datoTilKunEnJuridiskForelder: behandling?.beregningsGrunnlag?.kunEnJuridiskForelder
      ? mapListeFraDto(behandling!!.beregningsGrunnlag!!.kunEnJuridiskForelder!!)[0].tom!!
      : undefined,
  })

  const beregningsmetodeFormdataForAvdoed = (): BeregningsmetodeForAvdoedForm => {
    const beregningsMetodeForAvdoed = finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)

    return beregningsMetodeForAvdoed
      ? {
          fom: beregningsMetodeForAvdoed.fom,
          tom: beregningsMetodeForAvdoed.tom,
          data: {
            beregningsMetode: beregningsMetodeForAvdoed.data.beregningsMetode,
            avdoed: trygdetid.ident,
          },
          datoTilKunEnJuridiskForelder: behandling?.beregningsGrunnlag?.kunEnJuridiskForelder
            ? mapListeFraDto(behandling.beregningsGrunnlag.kunEnJuridiskForelder)[0].tom
            : undefined,
        }
      : defaultFormdata()
  }

  const finnPeriodisertBeregningsmetodeForAvdoed = (
    ident: String
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
        setRedigerModus(false)
        !!onSuccess && onSuccess(result)
      }
    )
  }

  const oppdaterBeregningsMetodeForAvdoed = (formdata: BeregningsmetodeForAvdoedForm) => {
    const nyMetode = formdataToMetode(formdata)

    lagre({
      ...toLagreBeregningsGrunnlagDto(behandling?.beregningsGrunnlag),
      beregningsMetodeFlereAvdoede: !!behandling?.beregningsGrunnlag?.beregningsMetodeFlereAvdoede?.length
        ? behandling?.beregningsGrunnlag.beregningsMetodeFlereAvdoede
            .filter((metode) => metode.data.avdoed !== nyMetode.data.avdoed)
            .concat(mapListeTilDto([nyMetode]))
        : mapListeTilDto([nyMetode]),
      kunEnJuridiskForelder: mapListeTilDto([
        { fom: nyMetode.fom, tom: formdata.datoTilKunEnJuridiskForelder, data: true },
        { fom: addDays(formdata.datoTilKunEnJuridiskForelder!!, 1), tom: undefined, data: false },
      ]),
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
    })
  }

  const beregningsMetodeForAvdoed: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined =
    finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)

  const { register, control, getValues, handleSubmit, reset } = useForm<BeregningsmetodeForAvdoedForm>({
    defaultValues: beregningsmetodeFormdataForAvdoed(),
  })

  useEffect(() => {
    if (!beregningsMetodeForAvdoed) {
      reset(defaultFormdata())
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

  function tagForKunEnJuridiskForelder() {
    const datoTomKunEnJuridiskForelder = behandling?.beregningsGrunnlag?.kunEnJuridiskForelder?.[0].tom

    return datoTomKunEnJuridiskForelder
      ? `Kun én juridisk forelder til og med ${formaterDato(datoTomKunEnJuridiskForelder)}`
      : `Kun én juridisk forelder`
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
              <VStack gap="4">
                {erEnesteJuridiskeForelder && (
                  <ControlledMaanedVelger
                    name="datoTilKunEnJuridiskForelder"
                    label="Til og med dato for kun én juridisk forelder(Valgfritt)"
                    description="Siste måneden med kun én juridisk forelder"
                    control={control}
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
                <HStack gap="4">
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
                <HStack gap="4">
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
        {mapNavn(trygdetid.ident)}{' '}
        {erEnesteJuridiskeForelder && (
          <Tag variant="alt1" size="small">
            {tagForKunEnJuridiskForelder()}
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

const formdataToMetode = (
  formdata: BeregningsmetodeForAvdoedForm
): PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> => {
  return {
    fom: formdata.fom,
    tom: formdata.tom,
    data: formdata.data,
  }
}
