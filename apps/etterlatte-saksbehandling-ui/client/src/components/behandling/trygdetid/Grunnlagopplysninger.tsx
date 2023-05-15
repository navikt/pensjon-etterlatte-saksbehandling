import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import React from 'react'
import { IGrunnlagOpplysninger, IOpplysningsgrunnlag } from '~shared/api/trygdetid'
import styled from 'styled-components'

export const Grunnlagopplysninger: React.FC<{ opplysninger: IGrunnlagOpplysninger }> = ({ opplysninger }) => (
  <InfoWrapper>
    <Opplysningsgrunnlag label={'Fødselsdato'} opplysningsgrunnlag={opplysninger.avdoedFoedselsdato} />
    <Opplysningsgrunnlag label={'16 år'} opplysningsgrunnlag={opplysninger.avdoedFylteSeksten} />
    <Opplysningsgrunnlag label={'Dødsdato'} opplysningsgrunnlag={opplysninger.avdoedDoedsdato} />
    <Opplysningsgrunnlag label={'66 år'} opplysningsgrunnlag={opplysninger.avdoedFyllerSeksti} />
  </InfoWrapper>
)

const Opplysningsgrunnlag: React.FC<{
  label: string
  opplysningsgrunnlag: IOpplysningsgrunnlag
}> = ({ label, opplysningsgrunnlag }) => (
  <Info
    label={label}
    tekst={opplysningsgrunnlag.opplysning ? formaterStringDato(opplysningsgrunnlag.opplysning) : 'n/a'}
    undertekst={opplysningsgrunnlag.kilde.type + ': ' + formaterStringDato(opplysningsgrunnlag.kilde.tidspunkt)}
  />
)

export const InfoWrapper = styled.div`
  display: flex;
  gap: 20px;
  padding: 2em 0 2em 0;
`
