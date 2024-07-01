import { formaterDato } from '~utils/formatering/dato'
import {
  IGrunnlagOpplysninger,
  IOpplysningsgrunnlag,
  ITrygdetid,
  oppdaterOpplysningsgrunnlag,
} from '~shared/api/trygdetid'
import styled from 'styled-components'
import { Alert, BodyShort, Button, Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { isPending } from '~shared/api/apiUtils'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'

export const Grunnlagopplysninger = ({
  trygdetid,
  onOppdatert,
  redigerbar,
}: {
  trygdetid: ITrygdetid
  onOppdatert: (trygdetid: ITrygdetid) => void
  redigerbar: boolean
}) => {
  const visDifferanse = redigerbar ? trygdetid.opplysningerDifferanse?.differanse : false
  return (
    <VStack gap="4">
      <Heading size="medium">Grunnlagsopplysninger</Heading>
      {visDifferanse ? (
        <DifferanseVisning trygdetid={trygdetid} onOppdatert={onOppdatert} />
      ) : (
        <OpplysningerTabell opplysninger={trygdetid.opplysninger} />
      )}
    </VStack>
  )
}

const OpplysningerTabell = ({ opplysninger }: { opplysninger: IGrunnlagOpplysninger }) => (
  <HStack gap="24">
    <Opplysningsgrunnlag label="Fødselsdato" opplysningsgrunnlag={opplysninger.avdoedFoedselsdato} />
    <Opplysningsgrunnlag label="16 år" opplysningsgrunnlag={opplysninger.avdoedFylteSeksten} />
    <Opplysningsgrunnlag label="Dødsdato" opplysningsgrunnlag={opplysninger.avdoedDoedsdato} />
    <Opplysningsgrunnlag label="66 år" opplysningsgrunnlag={opplysninger.avdoedFyllerSeksti} />
  </HStack>
)

const Opplysningsgrunnlag = ({
  label,
  opplysningsgrunnlag,
}: {
  label: string
  opplysningsgrunnlag: IOpplysningsgrunnlag | undefined
}) => (
  <>
    <VStack>
      <Label size="small">{label}</Label>
      <BodyShort>
        {opplysningsgrunnlag?.opplysning ? formaterDato(opplysningsgrunnlag.opplysning) : 'Ikke registrert'}
      </BodyShort>
      <Detail>
        {opplysningsgrunnlag?.kilde
          ? opplysningsgrunnlag?.kilde.type + ': ' + formaterDato(opplysningsgrunnlag?.kilde.tidspunkt)
          : 'Ikke registrert'}
      </Detail>
    </VStack>
  </>
)

const DifferanseVisning = ({
  trygdetid,
  onOppdatert,
}: {
  trygdetid: ITrygdetid
  onOppdatert: (trygdetid: ITrygdetid) => void
}) => {
  const [oppdatertTrygdetid, oppdaterTrygdetidOpplysningsgrunnlag] = useApiCall(oppdaterOpplysningsgrunnlag)
  const opplysningerDifferanse = trygdetid.opplysningerDifferanse!!

  return (
    <>
      <OppdatertGrunnlagAlert variant="warning">
        OBS! Grunnlaget for trygdetiden har blitt oppdatert. Sjekk at både faktisk og fremtidig trygdetid er korrekt.
      </OppdatertGrunnlagAlert>

      <Heading size="small" level="4">
        Eksisterende grunnlag
      </Heading>
      <OpplysningerTabell opplysninger={trygdetid.opplysninger} />

      <Heading size="small" level="4">
        Nytt grunnlag
      </Heading>
      <OpplysningerTabell opplysninger={opplysningerDifferanse.oppdaterteGrunnlagsopplysninger} />

      <div>
        <Button
          loading={isPending(oppdatertTrygdetid)}
          variant="primary"
          size="small"
          onClick={() =>
            oppdaterTrygdetidOpplysningsgrunnlag(trygdetid.behandlingId, (oppdatertTrygdetid) => {
              onOppdatert(oppdatertTrygdetid)
            })
          }
        >
          Bruk nytt grunnlag
        </Button>
      </div>
    </>
  )
}

export const OppdatertGrunnlagAlert = styled(Alert)`
  max-width: 42.5rem;
`
