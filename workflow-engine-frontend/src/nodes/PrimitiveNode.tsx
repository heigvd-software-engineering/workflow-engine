import "./css/PrimitiveNode.css"

import { useCallback } from 'react';
import { Handle, NodeProps, Position } from 'reactflow';

// const handleStyle = { left: 10 };
 
export type ValueData = {
  initialValue: string | number | undefined;
}

export default function PrimitiveNode(props: NodeProps<ValueData>) {
  const onChange = useCallback((evt: React.ChangeEvent<HTMLInputElement>) => {
    console.log("Primitive value changed to " + evt.target?.value);
  }, []);
 
  return (
    <div className="primitive-node">
      <Handle type="target" position={Position.Left} />
      <div style={{display: "flex"}}>
        <label htmlFor="text">Text:</label>
        <input id="text" name="text" size={1} style={{flexGrow: 1}} onChange={onChange} className="nodrag" defaultValue={props.data.initialValue} />
      </div>
      <span>Inputs / Outputs</span>
      <Handle type="source" position={Position.Right} style={{top: 0}} id="a" />
      <Handle type="source" position={Position.Right} id="b" />
    </div>
  );
}