import { ISaksType } from '~components/behandling/fargetags/saksType'
import BeregningsgrunnlagBarnepensjon from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjon'
import BeregningsgrunnlagOmstillingsstoenad from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagOmstillingsstoenad'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Heading } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Content, ContentHeader } from '~shared/styled'
import Trygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

const Beregningsgrunnlag = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size={'large'} level={'1'}>
            Beregningsgrunnlag
          </Heading>
          <BodyShort spacing>
            Vilkårsresultat:{' '}
            <strong>
              Innvilget fra{' '}
              {behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt.dato) : 'ukjent dato'}
            </strong>
          </BodyShort>
        </HeadingWrapper>
      </ContentHeader>
      <Trygdetid />
      {
        {
          [ISaksType.BARNEPENSJON]: <BeregningsgrunnlagBarnepensjon behandling={behandling} />,
          [ISaksType.OMSTILLINGSSTOENAD]: <BeregningsgrunnlagOmstillingsstoenad behandling={behandling} />,
        }[behandling.sakType]
      }
    </Content>
  )
}

export default Beregningsgrunnlag
