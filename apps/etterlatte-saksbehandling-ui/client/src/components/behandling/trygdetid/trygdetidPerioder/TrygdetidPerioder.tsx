import React, { useState } from 'react'
import { ILand, ITrygdetid, ITrygdetidGrunnlagType, slettTrygdetidsgrunnlag } from '~shared/api/trygdetid'
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

export interface VisRedigerTrygdetid {
  vis: boolean
  trydgetidGrunnlagId: string
}

export const initialVisRedigerTrygdetid = {
  vis: false,
  trydgetidGrunnlagId: '',
}

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

  const [visRedigerTrydgetid, setVisRedigerTrydgetid] = useState<VisRedigerTrygdetid>(initialVisRedigerTrygdetid)

  const trygdetidPerioder = trygdetid.trygdetidGrunnlag
    .filter((trygdetid) => trygdetid.type === trygdetidGrunnlagType)
    .sort((a, b) => (a.periodeFra > b.periodeFra ? 1 : -1))

  const kanLeggeTilNyPeriode =
    redigerbar &&
    !visRedigerTrydgetid.vis &&
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
        trygdetidPerioder={trygdetidPerioder}
        trygdetidGrunnlagType={trygdetidGrunnlagType}
        slettTrygdetid={slettTrygdetid}
        slettTrygdetidResult={slettTrygdetidResult}
        setVisRedigerTrydgetid={setVisRedigerTrydgetid}
        landListe={landListe}
        redigerbar={redigerbar}
      />

      {isFailureHandler({ apiResult: slettTrygdetidResult, errorMessage: 'Feil oppstått i slettingen av trygdetid' })}

      {visRedigerTrydgetid.vis && (
        <TrygdetidGrunnlag
          eksisterendeGrunnlag={trygdetidPerioder.find(
            (trygdetid) => trygdetid.id === visRedigerTrydgetid.trydgetidGrunnlagId
          )}
          trygdetidId={trygdetid.id}
          setTrygdetid={(trygdetid) => {
            setVisRedigerTrydgetid(initialVisRedigerTrygdetid)
            oppdaterTrygdetid(trygdetid)
          }}
          avbryt={() => setVisRedigerTrydgetid(initialVisRedigerTrygdetid)}
          trygdetidGrunnlagType={ITrygdetidGrunnlagType.FREMTIDIG}
          landListe={landListe}
        />
      )}

      {kanLeggeTilNyPeriode && (
        <div>
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            onClick={() => setVisRedigerTrydgetid({ vis: true, trydgetidGrunnlagId: '' })}
          >
            Ny periode
          </Button>
        </div>
      )}
    </VStack>
  )
}
