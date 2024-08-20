import React, { useState } from 'react'
import { ITrygdetid, ITrygdetidGrunnlagType, slettTrygdetidsgrunnlag } from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { CalendarIcon, PlusIcon } from '@navikt/aksel-icons'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import {
  FaktiskTrygdetidHjelpeTekst,
  FremtidigTrygdetidHjelpeTekst,
} from '~components/behandling/trygdetid/trygdetidPerioder/components/HjelpeTekster'
import { TrygdetidPerioderTable } from '~components/behandling/trygdetid/trygdetidPerioder/TrygdetidPerioderTable'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { ILand } from '~utils/kodeverk'

interface Props {
  trygdetid: ITrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
  redigerbar: boolean
}

export const TrygdetidPerioder = ({
  redigerbar,
  trygdetid,
  oppdaterTrygdetid,
  trygdetidGrunnlagType,
  landListe,
}: Props) => {
  const [slettTrygdetidResult, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)

  const [visLeggTilTrygdetidPeriode, setVisLeggTilTrygdetidPeriode] = useState<boolean>(false)

  const trygdetidPerioder = trygdetid.trygdetidGrunnlag
    .filter((trygdetid) => trygdetid.type === trygdetidGrunnlagType)
    .sort((a, b) => (a.periodeFra > b.periodeFra ? 1 : -1))

  const kanLeggeTilNyPeriode =
    redigerbar &&
    !visLeggTilTrygdetidPeriode &&
    !(trygdetidGrunnlagType === ITrygdetidGrunnlagType.FREMTIDIG && trygdetidPerioder.length > 0)

  const slettTrygdetid = (trygdetidGrunnlagId: string) => {
    slettTrygdetidsgrunnlagRequest(
      {
        trygdetidId: trygdetid.id,
        behandlingId: trygdetid.behandlingId,
        trygdetidGrunnlagId,
      },
      oppdaterTrygdetid
    )
  }

  return (
    <VStack gap="4">
      <HStack gap="2" align="center">
        <CalendarIcon aria-hidden height="1.5rem" width="1.5rem" />
        <Heading size="small">{formaterEnumTilLesbarString(trygdetidGrunnlagType)} trygdetid</Heading>
      </HStack>

      {trygdetidGrunnlagType === ITrygdetidGrunnlagType.FAKTISK ? (
        <FaktiskTrygdetidHjelpeTekst />
      ) : (
        <FremtidigTrygdetidHjelpeTekst />
      )}

      <TrygdetidPerioderTable
        trygdetidId={trygdetid.id}
        trygdetidPerioder={trygdetidPerioder}
        trygdetidGrunnlagType={trygdetidGrunnlagType}
        oppdaterTrygdetid={oppdaterTrygdetid}
        slettTrygdetid={slettTrygdetid}
        slettTrygdetidResult={slettTrygdetidResult}
        landListe={landListe}
        redigerbar={redigerbar}
      />

      {isFailureHandler({ apiResult: slettTrygdetidResult, errorMessage: 'Feil oppstått i slettingen av trygdetid' })}

      {visLeggTilTrygdetidPeriode && (
        <TrygdetidGrunnlag
          eksisterendeGrunnlag={undefined}
          trygdetidId={trygdetid.id}
          setTrygdetid={(trygdetid) => {
            oppdaterTrygdetid(trygdetid)
            setVisLeggTilTrygdetidPeriode(false)
          }}
          avbryt={() => setVisLeggTilTrygdetidPeriode(false)}
          trygdetidGrunnlagType={trygdetidGrunnlagType}
          landListe={landListe}
        />
      )}

      {kanLeggeTilNyPeriode && (
        <div>
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            onClick={() => setVisLeggTilTrygdetidPeriode(true)}
          >
            Ny periode
          </Button>
        </div>
      )}
    </VStack>
  )
}
