import React, { useState } from 'react'
import { BodyShort, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { CalendarIcon, PlusIcon } from '@navikt/aksel-icons'
import { ILand, ITrygdetid, ITrygdetidGrunnlagType, slettTrygdetidsgrunnlag } from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { FaktiskTrygdetidTable } from '~components/behandling/trygdetid/faktiskTrygdetid/FaktiskTrygdetidTable'

export interface VisRedigerTrygdetid {
  vis: boolean
  trydgetidGrunnlagId: string
}

const initialVisRedigerTrygdetid = {
  vis: false,
  trydgetidGrunnlagId: '',
}

interface Props {
  trygdetid: ITrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  landListe: ILand[]
  redigerbar: boolean
}

export const FaktiskTrygdetid = ({ redigerbar, trygdetid, oppdaterTrygdetid, landListe }: Props) => {
  const [slettTrygdetidResult, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)

  const [visRedigerTrydgetid, setVisRedigerTrydgetid] = useState<VisRedigerTrygdetid>(initialVisRedigerTrygdetid)

  const faktiskTrygdetidPerioder = trygdetid.trygdetidGrunnlag
    .filter((trygdetid) => trygdetid.type === ITrygdetidGrunnlagType.FAKTISK)
    .sort((a, b) => (a.periodeFra > b.periodeFra ? 1 : -1))

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
        <Heading size="small">Faktisk trygdetid</Heading>
      </HStack>
      <BodyShort>
        Legg til aktuell trygdetid fra aktuelle land (inkludert Norge) fra avdøde var 16 år frem til og med måneden før
        hen døde. Hvis trygdetid fra flere land med ulike avtaler, må det foretas beregning innen hver avtale. Huk da av
        for &quot;Ikke med i prorata&quot; for trygdetidsperioder i land som ikke skal med i de ulike beregningene. Velg
        beste alternativ for prorata-beregning.
      </BodyShort>

      <FaktiskTrygdetidTable
        faktiskTrygdetidPerioder={faktiskTrygdetidPerioder}
        slettTrygdetid={slettTrygdetid}
        slettTrygdetidResult={slettTrygdetidResult}
        setVisRedigerTrydgetid={setVisRedigerTrydgetid}
        landListe={landListe}
        redigerbar={redigerbar}
      />

      {isFailureHandler({ apiResult: slettTrygdetidResult, errorMessage: 'Feil oppstått i slettingen av trygdetid' })}

      {visRedigerTrydgetid.vis && (
        <TrygdetidGrunnlag
          trygdetidId={trygdetid.id}
          setTrygdetid={(trygdetid) => {
            setVisRedigerTrydgetid(initialVisRedigerTrygdetid)
            oppdaterTrygdetid(trygdetid)
          }}
          avbryt={() => setVisRedigerTrydgetid(initialVisRedigerTrygdetid)}
          eksisterendeGrunnlag={faktiskTrygdetidPerioder.find(
            (trygdetid) => trygdetid.id == visRedigerTrydgetid.trydgetidGrunnlagId
          )}
          trygdetidGrunnlagType={ITrygdetidGrunnlagType.FAKTISK}
          landListe={landListe}
        />
      )}

      {redigerbar && !visRedigerTrydgetid.vis && (
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
