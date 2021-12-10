import { Content, ContentHeader } from "../../../shared/styled";
import { Vilkaar } from "./vilkaar";

export const Inngangsvilkaar = () => {
    return (
        <Content>
            <ContentHeader>
                <h1>inngangsvilkaar</h1>
            </ContentHeader>
            <div>
                <Vilkaar />
                <Vilkaar />
                <Vilkaar />
                <Vilkaar />
                <Vilkaar />
            </div>
        </Content>
    );
};

