import React, { useState } from 'react'
import {
  BeregningsGrunnlagDto,
  BeregningsMetode,
  BeregningsMetodeBeregningsgrunnlag,
  BeregningsMetodeBeregningsgrunnlagForm,
} from '~shared/types/Beregning'
import { BodyShort, Box, Button, Heading, HStack, Radio, Table, Tag, Textarea, VStack } from '@navikt/ds-react'
import { FloppydiskIcon, PencilIcon, TagIcon, TrashIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { isPending, Result } from '~shared/api/apiUtils'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { tagTekstForKunEnJuridiskForelder } from '~components/behandling/beregningsgrunnlag/Beregningsgrunnlag'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { AnnenForelderVurdering } from '~shared/types/grunnlag'
import { SakType } from '~shared/types/sak'

const defaultBeregningMetode: BeregningsMetodeBeregningsgrunnlag = {
  beregningsMetode: null,
  begrunnelse: '',
}

interface Props {
  redigerbar: boolean
  navn: string
  behandling: IBehandlingReducer
  oppdaterBeregningsgrunnlag: (beregningsmetodeForm: BeregningsMetodeBeregningsgrunnlagForm) => void
  lagreBeregningsGrunnlagResult: Result<BeregningsGrunnlagDto>
  kunEnJuridiskForelder?: boolean
}

export const BeregningsMetodeBrukt = ({
  navn,
  redigerbar,
  behandling,
  oppdaterBeregningsgrunnlag,
  lagreBeregningsGrunnlagResult,
}: Props) => {
  const [redigerTrydgetidMetodeBrukt, setRedigerTrygdetidMetodeBrukt] = useState<boolean>(false)
  const personopplysninger = usePersonopplysninger()

  const eksisterendeMetode = behandling.beregningsGrunnlag?.beregningsMetode
  const kunEnJuridiskForelder = !!behandling.beregningsGrunnlag?.kunEnJuridiskForelder
  const kunEnJuridiskForelderPersongalleri =
    personopplysninger?.annenForelder?.vurdering === AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER
  const datoTilKunEnJuridiskForelder = behandling?.beregningsGrunnlag?.kunEnJuridiskForelder?.tom
    ? new Date(behandling.beregningsGrunnlag.kunEnJuridiskForelder.tom)
    : undefined

  const toBeregningsmetodeForm = (
    beregningsMetodeBeregningsgrunnlag: BeregningsMetodeBeregningsgrunnlag
  ): BeregningsMetodeBeregningsgrunnlagForm => ({
    beregningsMetode: beregningsMetodeBeregningsgrunnlag.beregningsMetode,
    begrunnelse: beregningsMetodeBeregningsgrunnlag.begrunnelse,
    datoTilKunEnJuridiskForelder: datoTilKunEnJuridiskForelder,
  })

  const { register, control, reset, handleSubmit } = useForm<BeregningsMetodeBeregningsgrunnlagForm>({
    defaultValues: toBeregningsmetodeForm(eksisterendeMetode ? eksisterendeMetode : defaultBeregningMetode),
  })

  const slettBeregningsMetode = () => {
    oppdaterBeregningsgrunnlag({
      beregningsMetode: BeregningsMetode.NASJONAL,
      begrunnelse: '',
      datoTilKunEnJuridiskForelder: undefined,
    })
    reset(defaultBeregningMetode)
    setRedigerTrygdetidMetodeBrukt(false)
  }

  const lagreBeregningsMetode = (beregningsmetodeForm: BeregningsMetodeBeregningsgrunnlagForm) => {
    oppdaterBeregningsgrunnlag(beregningsmetodeForm)
    setRedigerTrygdetidMetodeBrukt(false)
  }

  const tidligereFamiliepleier = !!behandling.tidligereFamiliepleier?.svar

  const beregningsMetode = behandling?.beregningsGrunnlag?.beregningsMetode

  return (
    <VStack gap="space-4">
      <HStack gap="space-2" align="center">
        <TagIcon aria-hidden fontSize="1.5rem" />
        <Heading size="small" level="3">
          Trygdetid i beregning
        </Heading>
      </HStack>
      <Box maxWidth="fit-content">
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell />
              <Table.HeaderCell scope="col">
                {behandling.sakType === SakType.BARNEPENSJON
                  ? 'Forelder'
                  : tidligereFamiliepleier
                    ? 'Familiepleier'
                    : 'Avdøde'}
              </Table.HeaderCell>
              <Table.HeaderCell scope="col">Trygdetid i beregningen</Table.HeaderCell>
              <Table.HeaderCell />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.ExpandableRow
              open={true}
              expansionDisabled={true}
              key="1"
              content={
                redigerTrydgetidMetodeBrukt ? (
                  <>
                    <form onSubmit={handleSubmit(lagreBeregningsMetode)}>
                      <VStack gap="space-4">
                        {kunEnJuridiskForelderPersongalleri && (
                          <ControlledMaanedVelger
                            name="datoTilKunEnJuridiskForelder"
                            label="Til og med dato for kun én juridisk forelder(Valgfritt)"
                            description="Siste måneden med kun én juridisk forelder"
                            control={control}
                            fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
                          />
                        )}

                        <ControlledRadioGruppe
                          name="beregningsMetode"
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
                                Den som gir høyest verdi av nasjonal/prorata (EØS/avtale-land, der rettighet er oppfylt
                                etter nasjonale regler)
                              </Radio>
                            </>
                          }
                        />
                        <Box width="15rem">
                          <Textarea {...register('begrunnelse')} label="Begrunnelse (valgfritt)" />
                        </Box>
                        <HStack gap="space-4">
                          <Button
                            size="small"
                            icon={<FloppydiskIcon aria-hidden />}
                            loading={isPending(lagreBeregningsGrunnlagResult)}
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
                              setRedigerTrygdetidMetodeBrukt(false)
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
                    <BodyShort>{beregningsMetode?.begrunnelse || '-'}</BodyShort>
                  </>
                )
              }
            >
              <Table.DataCell>
                {navn}{' '}
                {kunEnJuridiskForelder && (
                  <Tag data-color="meta-purple" variant="outline" size="small">
                    {tagTekstForKunEnJuridiskForelder(behandling)}
                  </Tag>
                )}
              </Table.DataCell>
              <Table.DataCell>
                {beregningsMetode?.beregningsMetode
                  ? formaterEnumTilLesbarString(beregningsMetode.beregningsMetode)
                  : 'Metode er ikke satt'}
              </Table.DataCell>
              <Table.DataCell>
                <HStack gap="space-4">
                  {redigerbar && (
                    <>
                      <Button
                        type="button"
                        variant="secondary"
                        size="small"
                        icon={<PencilIcon aria-hidden />}
                        disabled={redigerTrydgetidMetodeBrukt}
                        onClick={() => setRedigerTrygdetidMetodeBrukt(true)}
                      >
                        {beregningsMetode ? 'Rediger' : 'Legg til'}
                      </Button>
                    </>
                  )}
                  {redigerbar && beregningsMetode && (
                    <Button
                      size="small"
                      variant="secondary"
                      icon={<TrashIcon aria-hidden />}
                      loading={isPending(lagreBeregningsGrunnlagResult)}
                      onClick={slettBeregningsMetode}
                    >
                      Slett
                    </Button>
                  )}
                </HStack>
              </Table.DataCell>
            </Table.ExpandableRow>
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  )
}
