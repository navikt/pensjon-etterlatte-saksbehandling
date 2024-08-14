import React from 'react'
import { Box, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { TagIcon } from '@navikt/aksel-icons'
import { ITrygdetid } from '~shared/api/trygdetid'
import { formaterNavn } from '~shared/types/Person'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import {
  mapListeFraDto,
  mapListeTilDto,
  PeriodisertBeregningsgrunnlag,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsMetode, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { useBehandling } from '~components/behandling/useBehandling'
import { oppdaterBeregingsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { useAppDispatch } from '~store/Store'
import { ApiErrorAlert } from '~ErrorBoundary'
import { BeregningsMetodeRadForAvdoed } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeRadForAvdoed'

interface Props {
  redigerbar: boolean
  trygdetider: ITrygdetid[]
}

export const BeregningsgrunnlagFlereAvdoede = ({ redigerbar, trygdetider }: Props) => {
  const dispatch = useAppDispatch()
  const personopplysninger = usePersonopplysninger()
  const behandling = useBehandling()

  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)

  if (!behandling) return <ApiErrorAlert>Ingen behandling</ApiErrorAlert>

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

  const finnPeriodisertBeregningsmetodeForAvdoed = (
    ident: String
  ): PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined => {
    if (behandling?.beregningsGrunnlag && !!behandling?.beregningsGrunnlag.begegningsmetodeFlereAvdoede?.length) {
      return mapListeFraDto(behandling.beregningsGrunnlag.begegningsmetodeFlereAvdoede)?.find(
        (grunnlag) => grunnlag?.data.avdoed === ident
      )
    }
    return undefined
  }

  const slettBeregningsMetodeForAvdoed = (avdoed: string, onSuccess: () => void) => {
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
        onSuccess()
      }
    )
  }

  const oppdaterBeregninggsMetodeForAvdoed = (
    nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>,
    onSuccess: () => void
  ) => {
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
        onSuccess()
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
        <Table>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Forelder</Table.HeaderCell>
            <Table.HeaderCell scope="col">Trygdetid brukt i beregningen</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            <Table.HeaderCell />
            <Table.HeaderCell />
          </Table.Row>
          {trygdetider.map((trygdetid: ITrygdetid) => (
            <>
              <BeregningsMetodeRadForAvdoed
                beregningsMetodeForAvdoed={finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)}
                oppdaterBeregningsMetodeForAvdoed={oppdaterBeregninggsMetodeForAvdoed}
                slettBeregningsMetodeForAvdoed={slettBeregningsMetodeForAvdoed}
                lagreBeregningsgrunnlagResult={lagreBeregningsgrunnlagResult}
                navn={mapNavn(trygdetid.ident)}
                redigerbar={redigerbar}
              />
            </>
          ))}
        </Table>
      </Box>
    </VStack>
  )
}
