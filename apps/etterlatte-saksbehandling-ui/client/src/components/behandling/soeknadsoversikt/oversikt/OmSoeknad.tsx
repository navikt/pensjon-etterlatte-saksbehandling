import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'
import { Detail, Heading } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper, HeadingWrapper, WarningText } from '../styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../../behandlings-type'
import { format } from 'date-fns'
import { sjekkDataFraSoeknadMotPdl, WarningAlert } from './utils'
import {
  IKriterie,
  Kriterietype,
  VilkaarsType,
  VilkaarVurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'

export const OmSoeknad = () => {
  const { soekerPdl, soekerSoknad, dodsfall, avdodPersonPdl, avdodPersonSoknad, innsender, mottattDato } =
    usePersonInfoFromBehandling()
  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving

  const doedsfallVilkaar: any = vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)
  const avdoedErForelderVilkaar =
    doedsfallVilkaar.kriterier.find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER).resultat ===
    VilkaarVurderingsResultat.OPPFYLT
  const avdoedErLikISoeknad = dodsfall?.foedselsnummer === avdodPersonSoknad.foedselsnummer

  return (
    <>
      <h1>Søknadsoversikt</h1>
      <HeadingWrapper>
        <Heading spacing size="small" level="5">
          Om søknaden
        </Heading>
        <div className="details">
          <BehandlingsStatusSmall status={IBehandlingsStatus.FORSTEGANG} />
          <BehandlingsTypeSmall type={IBehandlingsType.BARNEPENSJON} />
        </div>
      </HeadingWrapper>

      <InfoWrapper>
        <DetailWrapper>
          <Detail size="medium">Mottaker</Detail>
          {sjekkDataFraSoeknadMotPdl(
            `${soekerPdl?.fornavn} ${soekerPdl?.etternavn}`,
            `${soekerSoknad?.fornavn} ${soekerSoknad?.etternavn}`
          )}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Avdød forelder</Detail>
          {avdoedErForelderVilkaar ? (
            sjekkDataFraSoeknadMotPdl(
              `${avdodPersonPdl?.fornavn} ${avdodPersonPdl?.etternavn}`,
              `${avdodPersonSoknad?.fornavn} ${avdodPersonSoknad?.etternavn}`
            )
          ) : (
            <WarningText>Ingen foreldre er død</WarningText>
          )}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Søknad fremsatt av</Detail>
          {innsender?.fornavn} {innsender?.etternavn}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Søknad mottatt</Detail>
          {format(new Date(mottattDato?.mottattDato), 'dd.MM.yyyy')}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Dato for dødsfall</Detail>
          {format(new Date(dodsfall?.doedsdato), 'dd.MM.yyyy')}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Første mulig virkningstidspunkt</Detail>
          01.01.22
        </DetailWrapper>
        {avdoedErForelderVilkaar &&
          !avdoedErLikISoeknad &&
          WarningAlert(
            `I PDL er det oppgitt ${avdodPersonPdl?.fornavn} ${avdodPersonPdl?.etternavn} som avdød forelder, men i søknad er det oppgitt ${avdodPersonSoknad?.fornavn} ${avdodPersonSoknad?.etternavn}. Må avklares.`
          )}
        {!avdoedErForelderVilkaar && WarningAlert('Oppgitt avdød i søknad er ikke forelder til barnet. Må avklares.')}
      </InfoWrapper>
    </>
  )
}
