
import CopyToClipboard from "react-copy-to-clipboard";
import { CopyIcon } from "../../shared/icons/copyIcon";

export const Fnr = (props: { value: string; copy?: boolean }) => {
  return (
      <div>
          {props.value}{" "}
          {props.copy && (
              <CopyToClipboard text={props.value}>
                  <span style={{verticalAlign: 'text-top', cursor: "pointer" }} aria-label="kopier fødselsnummer">
                      <CopyIcon />
                  </span>
              </CopyToClipboard>
          )}
      </div>
  );
};