import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
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
}: {
  trygdetid: ITrygdetid
  onOppdatert: (trygdetid: ITrygdetid) => void
}) => {
  const [oppdatertTrygdetid, oppdaterTrygdetidOpplysningsgrunnlag] = useApiCall(oppdaterOpplysningsgrunnlag)

  return (
    <>
      {!trygdetid.opplysningerDifferanse?.differanse && <OpplysningerTabell opplysninger={trygdetid.opplysninger} />}
      {trygdetid.opplysningerDifferanse?.differanse && (
        <>
          <Alert variant="info">
            OBS! Grunnlaget for trygdetiden har blitt oppdatert siden sist. <br />
            Du må se over periodene og lagre på nytt med oppdatert grunnlag.
          </Alert>
          <Heading size="small" level="4">
            Eksisterende grunnlag
          </Heading>

          <OpplysningerTabell opplysninger={trygdetid.opplysninger} />

          <Heading size="small" level="4">
            Nytt grunnlag
          </Heading>
          <OpplysningerTabell opplysninger={trygdetid.opplysningerDifferanse.oppdaterteGrunnlagsopplysninger} />
          <Button
            loading={isPending(oppdatertTrygdetid)}
            variant="primary"
            onClick={() =>
              oppdaterTrygdetidOpplysningsgrunnlag(trygdetid.behandlingId, (oppdatertTrygdetid) =>
                onOppdatert(oppdatertTrygdetid)
              )
            }
          >
            Bruk nytt grunnlag
          </Button>
        </>
      )}
    </>
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
  opplysningsgrunnlag: IOpplysningsgrunnlag
}) => (
  <Info
    label={label}
    tekst={opplysningsgrunnlag.opplysning ? formaterStringDato(opplysningsgrunnlag.opplysning) : 'n/a'}
    undertekst={opplysningsgrunnlag.kilde.type + ': ' + formaterStringDato(opplysningsgrunnlag.kilde.tidspunkt)}
  />
)
