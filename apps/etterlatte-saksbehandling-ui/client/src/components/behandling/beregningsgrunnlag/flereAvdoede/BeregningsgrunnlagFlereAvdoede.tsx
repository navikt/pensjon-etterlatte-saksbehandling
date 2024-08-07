import React, { useState } from 'react'
import { Box, Button, Heading, HStack, Tabs, VStack } from '@navikt/ds-react'
import { PencilIcon, PersonIcon, PlusIcon, TagIcon, TrashIcon } from '@navikt/aksel-icons'
import { ITrygdetid } from '~shared/api/trygdetid'
import { formaterNavn } from '~shared/types/Person'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { BeregningsMetodeForAvdoded } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeForAvdoded'
import {
  mapListeFraDto,
  mapListeTilDto,
  PeriodisertBeregningsgrunnlag,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsMetode, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { SammendragAvBeregningsMetodeForAvdoed } from '~components/behandling/beregningsgrunnlag/flereAvdoede/SammendragAvBeregningsMetodeForAvdoed'
import { isPending } from '~shared/api/apiUtils'
import { useBehandling } from '~components/behandling/useBehandling'
import { oppdaterBeregingsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { useAppDispatch } from '~store/Store'
import { ApiErrorAlert } from '~ErrorBoundary'

interface Props {
  redigerbar: boolean
  trygdetider: ITrygdetid[]
}

export const BeregningsgrunnlagFlereAvdoede = ({ redigerbar, trygdetider }: Props) => {
  const dispatch = useAppDispatch()
  const personopplysninger = usePersonopplysninger()
  const behandling = useBehandling()

  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)

  const [redigerTrydgetidMetodeBrukt, setRedigerTrygdetidMetodeBrukt] = useState<boolean>(false)

  if (!behandling) return <ApiErrorAlert>Ingen behandling</ApiErrorAlert>

  const mapNavn = (fnr: string): string => {
    const opplysning = personopplysninger?.avdoede?.find(
      (personOpplysning) => personOpplysning.opplysning.foedselsnummer === fnr
    )?.opplysning

    if (!opplysning) {
      return fnr
    }

    return `${formaterNavn(opplysning)} (${fnr})`
  }

  const finnPeriodisertBeregningsmetodeForAvdoed = (
    ident: String
  ): PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined => {
    if (behandling?.beregningsGrunnlag && !!behandling?.beregningsGrunnlag.begegningsmetodeFlereAvdoede?.length) {
      return mapListeFraDto(behandling?.beregningsGrunnlag.begegningsmetodeFlereAvdoede)?.find(
        (grunnlag) => grunnlag?.data.avdoed === ident
      )
    }
    return undefined
  }

  const slettBeregningsMetodeForAvdoed = (avdoed: string) => {
    const grunnlag = {
      ...behandling?.beregningsGrunnlag,
      soeskenMedIBeregning: behandling?.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
      institusjonsopphold: behandling?.beregningsGrunnlag?.institusjonsopphold ?? [],
      beregningsMetode: behandling?.beregningsGrunnlag?.beregningsMetode ?? {
        beregningsMetode: BeregningsMetode.NASJONAL,
      },
      begegningsmetodeFlereAvdoede: !!behandling?.beregningsGrunnlag?.begegningsmetodeFlereAvdoede?.length
        ? behandling?.beregningsGrunnlag.begegningsmetodeFlereAvdoede.filter((metode) => metode.data.avdoed !== avdoed)
        : [],
    }

    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag,
      },
      () => {
        dispatch(oppdaterBeregingsGrunnlag(grunnlag))
        setRedigerTrygdetidMetodeBrukt(false)
      }
    )
  }

  const oppdaterBeregninggsMetodeForAvdoed = (nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) => {
    const grunnlag = {
      ...behandling?.beregningsGrunnlag,
      soeskenMedIBeregning: behandling?.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
      institusjonsopphold: behandling?.beregningsGrunnlag?.institusjonsopphold ?? [],
      beregningsMetode: behandling?.beregningsGrunnlag?.beregningsMetode ?? {
        beregningsMetode: BeregningsMetode.NASJONAL,
      },
      begegningsmetodeFlereAvdoede: !!behandling?.beregningsGrunnlag?.begegningsmetodeFlereAvdoede?.length
        ? behandling?.beregningsGrunnlag.begegningsmetodeFlereAvdoede
            .filter((metode) => metode.data.avdoed !== nyMetode.data.avdoed)
            .concat(mapListeTilDto([nyMetode]))
        : mapListeTilDto([nyMetode]),
    }

    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag,
      },
      () => {
        dispatch(oppdaterBeregingsGrunnlag(grunnlag))
        setRedigerTrygdetidMetodeBrukt(false)
      }
    )
  }

  return (
    <VStack gap="4">
      <HStack gap="2">
        <TagIcon aria-hidden fontSize="1.5rem" />
        <Heading size="small" level="3">
          Trygdetid-metode brukt for flere avdøde
        </Heading>
      </HStack>
      <Box maxWidth="fit-content">
        <Tabs defaultValue={trygdetider[0].ident} onChange={() => setRedigerTrygdetidMetodeBrukt(false)}>
          <Tabs.List>
            {trygdetider.map((trygdetid: ITrygdetid) => (
              <Tabs.Tab
                key={trygdetid.ident}
                value={trygdetid.ident}
                icon={<PersonIcon aria-hidden />}
                label={mapNavn(trygdetid.ident)}
              />
            ))}
          </Tabs.List>
          {trygdetider.map((trygdetid: ITrygdetid) => (
            <Tabs.Panel value={trygdetid.ident} key={trygdetid.ident}>
              <Box paddingBlock="4 0">
                {!redigerTrydgetidMetodeBrukt && (
                  <VStack gap="4">
                    <SammendragAvBeregningsMetodeForAvdoed
                      beregningsMetodeForAvdoed={finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)}
                    />

                    {redigerbar && (
                      <HStack gap="4">
                        <Button
                          type="button"
                          variant="secondary"
                          size="small"
                          icon={
                            finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident) ? (
                              <PencilIcon aria-hidden />
                            ) : (
                              <PlusIcon aria-hidden />
                            )
                          }
                          onClick={() => setRedigerTrygdetidMetodeBrukt(true)}
                        >
                          {finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident) ? 'Rediger' : 'Legg til'}
                        </Button>
                        {finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident) && (
                          <Button
                            type="button"
                            variant="secondary"
                            size="small"
                            icon={<TrashIcon aria-hidden />}
                            onClick={() => slettBeregningsMetodeForAvdoed(trygdetid.ident)}
                            loading={isPending(lagreBeregningsgrunnlagResult)}
                          >
                            Slett
                          </Button>
                        )}
                      </HStack>
                    )}
                  </VStack>
                )}
                {redigerbar && redigerTrydgetidMetodeBrukt && (
                  <BeregningsMetodeForAvdoded
                    ident={trygdetid.ident}
                    navn={mapNavn(trygdetid.ident)}
                    eksisterendeMetode={finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)}
                    oppdaterBeregningsMetodeForAvdoed={oppdaterBeregninggsMetodeForAvdoed}
                    paaAvbryt={() => setRedigerTrygdetidMetodeBrukt(false)}
                    lagreBeregningsgrunnlagResult={lagreBeregningsgrunnlagResult}
                  />
                )}
              </Box>
            </Tabs.Panel>
          ))}
        </Tabs>
      </Box>
    </VStack>
  )
}
