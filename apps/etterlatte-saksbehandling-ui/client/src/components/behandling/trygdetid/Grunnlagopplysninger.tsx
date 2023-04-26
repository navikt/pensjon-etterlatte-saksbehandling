import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import React from 'react'
import { IGrunnlagOpplysninger, IOpplysningsgrunnlag } from '~shared/api/trygdetid'
import styled from 'styled-components'
import { ManglerRegelspesifikasjon } from '~components/behandling/felles/ManglerRegelspesifikasjon'

export const Grunnlagopplysninger: React.FC<{ opplysninger: IGrunnlagOpplysninger }> = ({ opplysninger }) => (
  <InfoWrapper>
    <Opplysningsgrunnlag label={'Fødselsdato'} opplysningsgrunnlag={opplysninger.avdoedFoedselsdato} />
    <ManglerRegelspesifikasjon>
      <Opplysningsgrunnlag label={'16 år'} opplysningsgrunnlag={opplysninger.avdoedFylteSeksten} />
    </ManglerRegelspesifikasjon>
    <Opplysningsgrunnlag label={'Dødsdato'} opplysningsgrunnlag={opplysninger.avdoedDoedsdato} />
    <ManglerRegelspesifikasjon>
      <Opplysningsgrunnlag label={'66 år'} opplysningsgrunnlag={opplysninger.avdoedFyllerSeksti} />
    </ManglerRegelspesifikasjon>
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
