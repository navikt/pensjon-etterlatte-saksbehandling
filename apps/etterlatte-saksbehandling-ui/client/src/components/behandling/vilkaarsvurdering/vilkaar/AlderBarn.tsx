import { Vilkaarsgrunnlag } from '~shared/api/vilkaarsvurdering'
import styled from 'styled-components'
import { VilkaarColumn } from '../styled'
import { hentKildenavn } from '../utils'
import { formaterStringDato } from '~utils/formattering'
import { KildeType } from '~shared/types/kilde'
import { Detail } from "@navikt/ds-react";

export const AlderBarn = ({ grunnlag }: { grunnlag: Vilkaarsgrunnlag<any>[] }) => {
  const foedselsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'SOEKER_FOEDSELSDATO')
  const doedsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'AVDOED_DOEDSDATO')
  const virkningstidspunktGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'VIRKNINGSTIDSPUNKT')

  const foedselsdatoKilde = foedselsdatoGrunnlag?.kilde
  const foedselsdato = foedselsdatoGrunnlag?.opplysning
  const doedsdato = doedsdatoGrunnlag?.opplysning
  const doedsdatoKilde = doedsdatoGrunnlag?.kilde
  const virkningstidspunkt = virkningstidspunktGrunnlag?.opplysning
  const virkningstidspunktKilde = virkningstidspunktGrunnlag?.kilde

  return (
    <>
      {doedsdato && doedsdatoKilde && doedsdatoKilde?.type === KildeType.pdl && (
        <VilkaarColumn>
            <div>
              <VilkaarStrong>Dødsfall</VilkaarStrong>
            </div>
            <span>{formaterStringDato(doedsdato)}</span>
            <KildeDatoOpplysning type={doedsdatoKilde.type} dato={doedsdatoKilde.tidspunktForInnhenting} />
        </VilkaarColumn>
      )}
      {foedselsdato && foedselsdatoKilde && foedselsdatoKilde?.type === KildeType.pdl && (
        <VilkaarColumn>
            <div>
              <VilkaarStrong>Barnets fødselsdato</VilkaarStrong>
            </div>
            <span>{formaterStringDato(foedselsdato)}</span>
            <KildeDatoOpplysning type={foedselsdatoKilde.type} dato={foedselsdatoKilde.tidspunktForInnhenting} />
        </VilkaarColumn>
      )}
      {virkningstidspunkt && virkningstidspunktKilde && virkningstidspunktKilde?.type == KildeType.saksbehandler && (
        <VilkaarColumn>
            <div>
              <VilkaarStrong>Virkningstidspunkt</VilkaarStrong>
            </div>
            <span>{formaterStringDato(virkningstidspunkt)}</span>
            <KildeDatoOpplysning type={virkningstidspunktKilde.type} dato={virkningstidspunktKilde.tidspunkt} />
        </VilkaarColumn>
      )}
    </>
  )
}

const KildeDatoOpplysning = ({ type, dato }: { type?: KildeType; dato?: string }) => {
  if (!dato) {
    return <div />
  }
  const dataDato = formaterStringDato(dato)
  const kilde = hentKildenavn(type)

  return (
    <Detail>
      {kilde} {dataDato}
    </Detail>
  )
}

export const VilkaarStrong = styled.strong`
    white-space: nowrap;
`
