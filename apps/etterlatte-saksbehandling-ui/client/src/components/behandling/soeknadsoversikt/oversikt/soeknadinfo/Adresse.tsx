import { Detail } from '@navikt/ds-react'
import { DetailWrapper, WarningIconWrapper } from '../../styled'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { VurderingsResultat, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'

export const Adresse = ({
  gjenlevendeOgSoekerLikAdresse,
}: {
  gjenlevendeOgSoekerLikAdresse: IGyldighetproving | undefined
}) => {
  return (
    <DetailWrapper>
      {gjenlevendeOgSoekerLikAdresse?.resultat === VurderingsResultat.OPPFYLT && (
        <div>
          <Detail size="medium">Adresse</Detail>
          <div className="text">Barnet bor på samme adresse som gjenlevende forelder</div>
        </div>
      )}

      {gjenlevendeOgSoekerLikAdresse?.resultat === VurderingsResultat.IKKE_OPPFYLT && (
        <div>
          <Detail size="medium" className="detailWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Adresse
          </Detail>
          <span className="warningText">Barnet bor ikke på samme adresse som gjenlevende forelder</span>
        </div>
      )}
      {gjenlevendeOgSoekerLikAdresse?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
        <div>
          <Detail size="medium" className="detailWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Adresse
          </Detail>
          <span className="warningText">Mangler info</span>
        </div>
      )}
    </DetailWrapper>
  )
}
