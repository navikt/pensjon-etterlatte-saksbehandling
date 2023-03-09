import { useAppSelector } from '~store/Store'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import BeregningsgrunnlagBarnepensjon from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjon'
import BeregningsgrunnlagOmstillingsstoenad from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagOmstillingsstoenad'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Heading } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Content, ContentHeader } from '~shared/styled'
import Trygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'

const Beregningsgrunnlag = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

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
          [ISaksType.BARNEPENSJON]: <BeregningsgrunnlagBarnepensjon />,
          [ISaksType.OMSTILLINGSSTOENAD]: <BeregningsgrunnlagOmstillingsstoenad />,
        }[behandling.sakType]
      }
    </Content>
  )
}

export default Beregningsgrunnlag
