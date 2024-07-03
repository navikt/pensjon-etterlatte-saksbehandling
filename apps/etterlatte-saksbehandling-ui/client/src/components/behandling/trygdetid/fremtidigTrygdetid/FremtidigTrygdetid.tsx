import React, { useState } from 'react'
import { Button, Heading, HStack, ReadMore, VStack } from '@navikt/ds-react'
import { CalendarIcon, PlusIcon } from '@navikt/aksel-icons'
import { ILand, ITrygdetid, ITrygdetidGrunnlagType, slettTrygdetidsgrunnlag } from '~shared/api/trygdetid'
import { FremtidigTrygdetidTable } from '~components/behandling/trygdetid/fremtidigTrygdetid/FremtidigTrygdetidTable'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  initialVisRedigerTrygdetid,
  VisRedigerTrygdetid,
} from '~components/behandling/trygdetid/faktiskTrygdetid/FaktiskTrygdetid'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'

interface Props {
  trygdetid: ITrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  landListe: ILand[]
  redigerbar: boolean
}

export const FremtidigTrygdetid = ({ trygdetid, oppdaterTrygdetid, redigerbar, landListe }: Props) => {
  const [slettTrygdetidResult, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)

  const [visRedigerTrydgetid, setVisRedigerTrydgetid] = useState<VisRedigerTrygdetid>(initialVisRedigerTrygdetid)

  const fremtidigTrygdetidPerioder = trygdetid.trygdetidGrunnlag
    .filter((trygdetid) => trygdetid.type === ITrygdetidGrunnlagType.FREMTIDIG)
    .sort((a, b) => (a.periodeFra > b.periodeFra ? 1 : -1))

  const kanLeggeTilNyPeriode = redigerbar && !visRedigerTrydgetid.vis && !(fremtidigTrygdetidPerioder.length > 0)

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
        <Heading size="small">Fremtidig trygdetid</Heading>
      </HStack>
      <ReadMore header="Mer om fremtidig trygdetid">
        Det registreres maks fremtidig trygdetid fra dødsdato til og med kalenderåret avdøde hadde blitt 66 år. Denne
        vil automatisk bli justert i beregningen hvis faktisk trygdetid er mindre enn 4/5 av opptjeningstiden. Hvis det
        er annen grunn for reduksjon av fremtidig trygdetid må perioden redigeres.
      </ReadMore>

      <FremtidigTrygdetidTable
        fremtidigTrygdetidPerioder={fremtidigTrygdetidPerioder}
        slettTrygdetid={slettTrygdetid}
        slettTrygdetidResult={slettTrygdetidResult}
        setVisRedigerTrydgetid={setVisRedigerTrydgetid}
        redigerbar={redigerbar}
        landListe={landListe}
      />

      {isFailureHandler({ apiResult: slettTrygdetidResult, errorMessage: 'Feil oppstått i slettingen av trygdetid' })}

      {visRedigerTrydgetid.vis && (
        <TrygdetidGrunnlag
          eksisterendeGrunnlag={fremtidigTrygdetidPerioder.find(
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
