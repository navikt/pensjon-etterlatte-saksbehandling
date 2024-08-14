import React, { useState } from 'react'
import {
  mapListeFraDto,
  mapListeTilDto,
  PeriodisertBeregningsgrunnlag,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsGrunnlagPostDto, BeregningsMetode, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { Button, Table } from '@navikt/ds-react'
import { format, startOfMonth } from 'date-fns'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { BeregningsMetodeSkjemaForAvdoed } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeSkjemaForAvdoed'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { IBehandlingReducer, oppdaterBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { formaterNavn } from '~shared/types/Person'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { ITrygdetid } from '~shared/api/trygdetid'

interface Props {
  behandling: IBehandlingReducer
  trygdetid: ITrygdetid
  redigerbar: boolean
}

export const BeregningsMetodeRadForAvdoed = ({ behandling, trygdetid, redigerbar }: Props) => {
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

  function lagre(grunnlag: BeregningsGrunnlagPostDto) {
    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag,
      },
      () => {
        dispatch(oppdaterBeregningsGrunnlag(grunnlag))
        setRedigerModus(false)
      }
    )
  }

  function oppdaterBeregningsMetodeForAvdoed(nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) {
    lagre({
      ...behandling?.beregningsGrunnlag,
      soeskenMedIBeregning: behandling?.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
      institusjonsopphold: behandling?.beregningsGrunnlag?.institusjonsopphold ?? [],
      beregningsMetode: behandling?.beregningsGrunnlag?.beregningsMetode ?? {
        beregningsMetode: BeregningsMetode.NASJONAL,
      },
      beregningsMetodeFlereAvdoede: !!behandling?.beregningsGrunnlag?.beregningsMetodeFlereAvdoede?.length
        ? behandling?.beregningsGrunnlag.beregningsMetodeFlereAvdoede
            .filter((metode) => metode.data.avdoed !== nyMetode.data.avdoed)
            .concat(mapListeTilDto([nyMetode]))
        : mapListeTilDto([nyMetode]),
    })
  }

  function slettBeregningsMetodeForAvdoed() {
    lagre({
      ...behandling?.beregningsGrunnlag,
      soeskenMedIBeregning: behandling?.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
      institusjonsopphold: behandling?.beregningsGrunnlag?.institusjonsopphold ?? [],
      beregningsMetode: behandling?.beregningsGrunnlag?.beregningsMetode ?? {
        beregningsMetode: BeregningsMetode.NASJONAL,
      },
      beregningsMetodeFlereAvdoede: !!behandling?.beregningsGrunnlag?.beregningsMetodeFlereAvdoede?.length
        ? behandling?.beregningsGrunnlag.beregningsMetodeFlereAvdoede.filter(
            (metode) => metode.data.avdoed !== trygdetid.ident
          )
        : [],
    })
  }

  const navn = mapNavn(trygdetid.ident)
  const beregningsMetodeForAvdoed = finnPeriodisertBeregningsmetodeForAvdoed(trygdetid.ident)
  return (
    <Table.ExpandableRow
      open={redigerModus}
      onOpenChange={(open) => {
        setRedigerModus(open)
      }}
      key={trygdetid.ident}
      content={
        redigerModus ? (
          <BeregningsMetodeSkjemaForAvdoed
            ident={trygdetid.ident}
            navn={navn}
            eksisterendeMetode={beregningsMetodeForAvdoed}
            paaAvbryt={() => {
              setRedigerModus(false)
            }}
            oppdaterBeregningsMetodeForAvdoed={oppdaterBeregningsMetodeForAvdoed}
            lagreBeregningsgrunnlagResult={lagreBeregningsgrunnlagResult}
          />
        ) : (
          ''
        )
      }
    >
      <Table.DataCell>{navn}</Table.DataCell>
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
