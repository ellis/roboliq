'use babel';

// Copied from https://jsfiddle.net/Cifren/pz3nxpnj/

class Autocomplete extends React.Component {

	constructor(props){
		super(props);
		this.state = {
			label: this.getOption("defaultValue"),
			dataList: this.getOption("values")
		};
	}

	// on component loading
	componentDidMount(){
		this.initAwesomplete();
	}

	// Init awesomplete
	initAwesomplete(){
		var input = this.refs.input;
		//use Awesomplete lib
		new Awesomplete(input, {
			list: this.state.dataList,
			replace: this.onReplace(input),
			autoFirst: this.getOption("autoFirst")
		});
	}

	// anytime the awesomplete replace, the state and parent form data change
	onReplace(input) {
		return ((item) => {
			// use the Form parent function
			if (this.props.onChange) {
				this.props.onChange(item.value);
			}
			this.setState({label: item.label});
		}).bind(this);
	}

	// on input change function
	onChange(event){
		// Anytime the input change, the State change
		// Anytime the state change, the component will be rendered with the new label
		this.setState({
			label: event.target.value
		});
	}

	// get default options based on props
	getDefaultOptions(){
		const props = this.props;
		return {
			// when popup open, first item is selected
			"autoFirst": props["autoFirst"]?props["autoFirst"]:false,
			// value to display on first init
			"defaultValue": props["defaultValue"]?props["defaultValue"]:null,
			// values to display in the popup
			"values": props["values"]?props["values"]:null,
			// minimum character before popup open
			"minChar": props["minChar"]?props["minChar"]:2
		};
	}

	// get option with name
	getOption(optionName){
		return this.options[optionName];
	}

	// get all options
	get options(){
		if(!this._options){
			this._options = this.getDefaultOptions();
		}
		return this._options;
	}

	getValue() {
		return this.state.label;
	}

	render() {
		return <input
			ref="input"
			value={this.state.label}
			className='form-control'
			onChange={this.onChange.bind(this)}
			minChars={this.props.minChars}
		/>
	}
}

export default Autocomplete;
