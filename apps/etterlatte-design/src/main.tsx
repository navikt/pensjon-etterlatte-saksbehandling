import React from "react";
import ReactDOM from "react-dom/client";
import "@navikt/ds-css";
import { Box, Heading, HStack, Label, Tag, VStack } from "@navikt/ds-react";
import { ChildEyesIcon, PlantIcon } from "@navikt/aksel-icons";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Box paddingBlock="6" paddingInline="6">
      <VStack gap="4">
        <Heading size="xlarge">
          Etterlatte sine visuelle bestemmelser for Gjenny!
        </Heading>

        <Box paddingBlock="4" paddingInline="4" background="bg-subtle">
          <Heading size="large">Tags</Heading>

          <HStack gap="4">
            <VStack gap="2">
              <Heading size="medium">Ytelse</Heading>

              <Label>Lang</Label>
              <div>
                <Tag
                  variant="alt2-moderate"
                  icon={<ChildEyesIcon aria-hidden />}
                >
                  Barnepejson
                </Tag>
              </div>
              <div>
                <Tag variant="alt1-moderate" icon={<PlantIcon aria-hidden />}>
                  Omstillingstønad
                </Tag>
              </div>

              <Label>Kort</Label>
              <div>
                <Tag
                  variant="alt2-moderate"
                  icon={<ChildEyesIcon aria-hidden />}
                >
                  BP
                </Tag>
              </div>
              <div>
                <Tag variant="alt1-moderate" icon={<PlantIcon aria-hidden />}>
                  OMS
                </Tag>
              </div>
            </VStack>

            <VStack gap="2">
              <Heading size="medium">Behandlingstype</Heading>
            </VStack>
          </HStack>
        </Box>
      </VStack>
    </Box>
  </React.StrictMode>,
);
