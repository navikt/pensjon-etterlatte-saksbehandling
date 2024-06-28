import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato } from '~utils/formatering/dato'
import {
  IGrunnlagOpplysninger,
  IOpplysningsgrunnlag,
  ITrygdetid,
  oppdaterOpplysningsgrunnlag,
} from '~shared/api/trygdetid'
import styled from 'styled-components'
import { Alert, Button, Heading } from '@navikt/ds-react'
import { isPending } from '~shared/api/apiUtils'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'

const InfoWrapperWithGap = styled.div`
  display: flex;
  gap: 20px;
  padding: 2em 0 2em 0;
`

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
  return visDifferanse ? (
    <DifferanseVisning trygdetid={trygdetid} onOppdatert={onOppdatert} />
  ) : (
    <OpplysningerTabell opplysninger={trygdetid.opplysninger} />
  )
}

const OpplysningerTabell = ({ opplysninger }: { opplysninger: IGrunnlagOpplysninger }) => (
  <InfoWrapperWithGap>
    <Opplysningsgrunnlag label="Fødselsdato" opplysningsgrunnlag={opplysninger.avdoedFoedselsdato} />
    <Opplysningsgrunnlag label="16 år" opplysningsgrunnlag={opplysninger.avdoedFylteSeksten} />
    <Opplysningsgrunnlag label="Dødsdato" opplysningsgrunnlag={opplysninger.avdoedDoedsdato} />
    <Opplysningsgrunnlag label="66 år" opplysningsgrunnlag={opplysninger.avdoedFyllerSeksti} />
  </InfoWrapperWithGap>
)

const Opplysningsgrunnlag = ({
  label,
  opplysningsgrunnlag,
}: {
  label: string
  opplysningsgrunnlag: IOpplysningsgrunnlag | undefined
}) => (
  <Info
    label={label}
    tekst={opplysningsgrunnlag?.opplysning ? formaterDato(opplysningsgrunnlag.opplysning) : 'Ikke registrert'}
    undertekst={
      opplysningsgrunnlag?.kilde
        ? opplysningsgrunnlag?.kilde.type + ': ' + formaterDato(opplysningsgrunnlag?.kilde.tidspunkt)
        : 'Ikke registrert'
    }
  />
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
        OBS! Grunnlaget for trygdetiden har blitt oppdatert. <br />
        Sjekk at både faktisk og fremtidig trygdetid er korrekt.
      </OppdatertGrunnlagAlert>

      <Heading size="small" level="4">
        Eksisterende grunnlag
      </Heading>
      <OpplysningerTabell opplysninger={trygdetid.opplysninger} />

      <Heading size="small" level="4">
        Nytt grunnlag
      </Heading>
      <OpplysningerTabell opplysninger={opplysningerDifferanse.oppdaterteGrunnlagsopplysninger} />

      <Button
        loading={isPending(oppdatertTrygdetid)}
        variant="primary"
        onClick={() =>
          oppdaterTrygdetidOpplysningsgrunnlag(trygdetid.behandlingId, (oppdatertTrygdetid) => {
            onOppdatert(oppdatertTrygdetid)
          })
        }
      >
        Bruk nytt grunnlag
      </Button>
    </>
  )
}

export const OppdatertGrunnlagAlert = styled(Alert)`
  margin: 2em 4em 0 4em;
  max-width: fit-content;
`
