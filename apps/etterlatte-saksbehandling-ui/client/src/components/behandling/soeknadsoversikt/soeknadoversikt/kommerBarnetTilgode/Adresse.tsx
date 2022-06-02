import { Label } from '@navikt/ds-react'
import { DetailWrapper, WarningIconWrapper } from '../../styled'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { VurderingsResultat, IVilkaarsproving } from '../../../../../store/reducers/BehandlingReducer'

export const Adresse = ({
  gjenlevendeOgSoekerLikAdresse,
}: {
  gjenlevendeOgSoekerLikAdresse: IVilkaarsproving | undefined
}) => {
  return (
    <DetailWrapper>
      {gjenlevendeOgSoekerLikAdresse?.resultat === VurderingsResultat.OPPFYLT && (
        <div>
          <Label size="small">Adresse</Label>
          <div className="text">Gjenlevende og barnet bor fortsatt sammen</div>
        </div>
      )}

      {gjenlevendeOgSoekerLikAdresse?.resultat === VurderingsResultat.IKKE_OPPFYLT && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            Adresse
          </Label>
          <span className="warningText">Barnet bor ikke på samme adresse som gjenlevende forelder</span>
        </div>
      )}
      {gjenlevendeOgSoekerLikAdresse?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Adresse
          </Label>
          <span className="warningText">Mangler info</span>
        </div>
      )}
    </DetailWrapper>
  )
}
