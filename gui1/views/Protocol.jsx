'use babel'

import _ from 'lodash';
import {List} from 'immutable';
const MarkdownIt = require('markdown-it');
import * as YAML from 'yamljs';

const markdown = new MarkdownIt();

const Protocol = ({
	state,
	onEdit,
	onSetProperty
}) => (
	<div>
		<h1>roboliq version {_.get(state, ["protocols", "main", "roboliq"], "UNKNOWN")}</h1>
		<div dangerouslySetInnerHTML={{__html: markdown.render(_.get(state, ["protocols", "main", "description"], ""))}}/>
		<ProtocolParameters state={state} onEdit={onEdit} onSetProperty={onSetProperty}/>
		<ProtocolObjects state={state} onEdit={onEdit} onSetProperty={onSetProperty}/>
		<ProtocolSteps state={state} onEdit={onEdit} onSetProperty={onSetProperty}/>
	</div>
);

//
// Protocol parameters
//

const ProtocolParameters = (props) => {
	const path = ["parameters"];
	const schema = {
		properties: {
			description: {type: "markdown"},
			value: {description: "value of the parameter"}
		}
	};

	const paramElems = _.map(_.get(props.state, ["protocols", "main", "parameters"]), (paramObject, paramName) => {
		// console.log({paramName, paramObject});
		return <div key={paramName}>
			<h3>{paramName}</h3>
			<div style={{marginLeft: "1em"}}>
			{_.map(["description", "value"], (key) => {
				const value = paramObject[key];
				const path2 = ["parameters", paramName, key];
				const propertySchema = schema.properties[key];
				// console.log({key, path2, value, propertySchema})
				return <ProtocolObjectProperty key={key} state={props.state} path={path2} schema={_.clone(schema)} propertyName={key} propertyValue={value} propertySchema={propertySchema} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>;
			})}
			</div>
		</div>;
	});

	return <div>
		<h2>Parameters</h2>
		{paramElems}
	</div>;
}

//
// Protocol objects
//

const ProtocolObjects = (props) => (
	<div>
		<h2>Objects</h2>
		{_.map(_.get(props.state, ["protocols", "main", "objects"]), (value, key) => <ProtocolObject key={key} state={props.state} path={["objects", key]} value={value} valueKey={key} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>)}
	</div>
);

const ProtocolObject = (props) => {
	const {state, path, value, valueKey} = props;
	// console.log("onSetProperty type: "+(typeof _.keys(props.onSetProperty)))
	const schema = _.get(state, ["protocols", "output", "schemas", value.type], {});
	const keysInRequired = schema.required || [];
	const keysInObject = _.keys(value) || [];
	const keysInSchema = _.keys(schema.properties) || [];
	const keysFirst = _.filter(keysInSchema, key => _.includes(keysInRequired, key) || value.hasOwnProperty(key));
	const keys = _.uniq(_.flatten([keysFirst, keysInSchema, keysInObject]));
	const type = value.type;

	return <div>
		<h3>{valueKey}</h3>
		<div style={{marginLeft: "1em"}}>
		{
			_.map(keys, key => <ProtocolObjectProperty key={key} state={state} path={path.concat(key)} schema={schema} propertyName={key} propertyValue={_.get(value, key)} propertySchema={_.get(schema, ["properties", key])} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>)
		}
		</div>
	</div>
};

const ProtocolObjectProperty = (props) => {
	const {state, path, schema, propertyName, propertyValue, propertySchema = {}} = props;
	// console.log({props})
	const isRequired = _.includes(schema.required, propertyName);
	const isDefined = (schema.properties || {}).hasOwnProperty(propertyName);
	const isDeclared = !_.isNil(propertyValue);
	const isExtra = (isDeclared && !isDefined);
	const style = {
		fontWeight: (isRequired) ? "bold" : undefined,
		color: (isDefined) ? "#690" : "red",
		fontStyle: (isExtra) ? "italic" : undefined
	};

	// Property name
	const propertyNameSpan0 = <span style={style}>{propertyName}:</span>;
	// console.log({propertySchema})
	const propertyNameSpan = (isDefined && !_.isEmpty(propertySchema) && propertyName != "type")
		? <span className="tooltip">
				{propertyNameSpan0}
				<span className="tooltiptext"><pre>{YAML.stringify(propertySchema, 3, 2)}</pre></span>
			</span>
		: propertyNameSpan0;
	const hasPropertyNameTooltip = isDefined;

	const propertyValueElem = makeProtocolObjectPropertyElem(props);

	// All together
	return <div className="row">
		<div className="col-md-2" style={{textAlign: "right"}}>
			{propertyNameSpan}
		</div>
		<div className="col-md-10">{propertyValueElem}</div>
	</div>;
}

const makeProtocolObjectPropertyElem = (props) => {
	//if (props.state.editing) console.log("editing "+props.state.editing.join(".")+" / "+props.path.join(".")+" :"+_.isEqual(props.state.editing, props.path))
	return (_.isEqual(props.state.editing, props.path))
		? makeProtocolObjectPropertyElemRW(props)
		: makeProtocolObjectPropertyElemRO(props);
}

const makeProtocolObjectPropertyElemRO = (props) => {
	const {state, path, schema, propertyName, propertyValue, propertySchema = {}} = props;
	// Property value
	const valueHandler = protocolObjectPropertyHandlers[propertySchema.type];
	if (propertyName == "steps") {
		console.log({path})
	}
	console.log({propertyName, propertyValue, path})
	const valueElem
		// Empty value
		= (_.isNil(propertyValue)) ? undefined
		// `type` property
		: (propertyName == "type" || propertyName == "command") ?
			<span className="tooltip">
				<span style={{fontWeight: "bold"}} onClick={() => props.onEdit(path)}>{propertyValue}</span>
				<span className="tooltiptext"><pre>{YAML.stringify(_.get(state, ["protocols", "output", "schemas", propertyValue], {}), 10, 2)}</pre></span>
			</span>
		// property references an object
		: (_.isPlainObject(propertySchema) && /^[A-Z]/.test(propertySchema.type) && _.isString(propertyValue)) ?
			<span className="tooltip">
				<span style={{color: "#07a"}} onClick={() => props.onEdit(path)}>{propertyValue}</span>
				<span className="tooltiptext"><pre>{YAML.stringify(_.get(state, "protocols.output.objects."+propertyValue, {}), 10, 2)}</pre></span>
			</span>
		// Steps
		: (propertyName == "steps") ?
			<ProtocolStep state={props.state} path={path} step={propertyValue} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>
		// we have a handler for the value
		: (valueHandler) ? valueHandler(propertyValue, propertySchema, path, props)
		// otherwise
		: <div className="yaml" onClick={() => props.onEdit(path)}>{YAML.stringify(propertyValue || "", 3, 2)}</div>;
	return valueElem;
}

const makeProtocolObjectPropertyElemRW = (props) => {
	return <YamlEditor path={props.path} value={props.propertyValue} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>
}

class JsonEditor extends React.Component {
	constructor(props) {
		console.log("JsonEditor props: "+_.keys(props))
		super(props);
		this.state = {
			value: JSON.stringify(props.value, null, "\t")
		};

		this.handleChange = this.handleChange.bind(this);
		this.handleCancel = this.handleCancel.bind(this);
		this.handleSubmit = this.handleSubmit.bind(this);
	}

	render() {
		return <div>
			<textarea cols="80" rows="10" value={this.state.value} onChange={this.handleChange}/>
			<button onClick={this.handleCancel}>Cancel</button>
			<button onClick={this.handleSubmit}>Save</button>
		</div>;
	}

	handleChange(event) {
		this.setState({value: event.target.value});
	}

	handleCancel(event) {
		this.props.onEdit(undefined);
		event.preventDefault();
	}

	handleSubmit(event) {
		const value = JSON.parse(this.state.value);
		this.props.onSetProperty(this.props.path, value);
		this.props.onEdit(undefined);
		event.preventDefault();
	}
}

class YamlEditor extends React.Component {
	constructor(props) {
		console.log("YamlEditor props: "+_.keys(props))
		super(props);
		this.state = {
			value: YAML.stringify(props.value, 3, 2)
		};

		this.handleChange = this.handleChange.bind(this);
		this.handleCancel = this.handleCancel.bind(this);
		this.handleSubmit = this.handleSubmit.bind(this);
	}

	render() {
		return <div>
			<textarea cols="80" rows="10" value={this.state.value} onChange={this.handleChange}/>
			<button onClick={this.handleCancel}>Cancel</button>
			<button onClick={this.handleSubmit}>Save</button>
		</div>;
	}

	handleChange(event) {
		this.setState({value: event.target.value});
	}

	handleCancel(event) {
		this.props.onEdit(undefined);
		event.preventDefault();
	}

	handleSubmit(event) {
		const value = YAML.parse(this.state.value);
		this.props.onSetProperty(this.props.path, value);
		this.props.onEdit(undefined);
		event.preventDefault();
	}
}

const protocolObjectPropertyHandlers = {
	markdown: (value) => {
		return <span style={{color: "#e90"}} dangerouslySetInnerHTML={{__html: markdown.renderInline(value)}}/>
	},
	string: (value) => {
		return <span style={{color: "#e90"}}>{value}</span>
	},
}


//
// Protocol steps
//

const ProtocolSteps = (props) => {
	return <div>
		<h2>Steps</h2>
		<ProtocolStep state={props.state} path={["steps"]} step={_.get(props.state, ["protocols", "output", "steps"])} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>
	</div>
};

const stepSchemaGeneric = {
	properties: {
		description: {type: "markdown"},
		data: {type: "object"},
		command: {type: "string"}
	}
};

const ProtocolStep = (props) => {
	const {state, path, step} = props;
	// console.log("path: "+path.join("."))
	// console.log({step})
	//
	if (_.isUndefined(step)) {
		return null;
	}

	const stepKeys = _.keys(step);
	const substepKeys = _.filter(stepKeys, key => /^[0-9]/.test(key)); // all keys beginning with a digit, thus representing a sub-step.
	const otherKeys = _.difference(stepKeys, substepKeys); // all other keys
	const paramKeys = _.difference(otherKeys, ["command", "data", "description"]); // parameters to the command

	// console.log("onSetProperty type: "+(typeof _.keys(props.onSetProperty)))
	const schema = _.get(state, ["protocols", "output", "schemas", step.command], {});
	const keysInRequired = schema.required || [];
	const keysInStep = stepKeys;
	const keysInSchema = _.keys(schema.properties) || [];
	const keysFirst = _.filter(keysInSchema, key => _.includes(keysInRequired, key) || step.hasOwnProperty(key));
	const keys = _.uniq(_.flatten([keysFirst, keysInSchema, keysInStep]));

	const iStepLast = _.lastIndexOf(path, "steps");
	const displayPath = _.drop(path, iStepLast + 1);

	return <div>
		<h3>{displayPath.join(".")}</h3>
		<div style={{marginLeft: "1em"}}>
		{_.map(["description", "data", "command"], key => (!_.isEmpty(step[key]))
			? <ProtocolObjectProperty key={key} state={state} path={path.concat(key)} schema={stepSchemaGeneric} propertyName={key} propertyValue={step[key]} propertySchema={stepSchemaGeneric.properties[key]} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>
			: undefined
		)}
		{
			_.map(paramKeys, key => <ProtocolObjectProperty key={key} state={state} path={path.concat(key)} schema={schema} propertyName={key} propertyValue={step[key]} propertySchema={_.get(schema, ["properties", key])} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>)
		}
		{
			_.map(substepKeys, key => <ProtocolStep key={key} state={props.state} path={path.concat(key)} step={step[key]} onEdit={props.onEdit} onSetProperty={props.onSetProperty}/>)
		}
		</div>
	</div>
};

export default Protocol;
