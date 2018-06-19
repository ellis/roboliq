'use babel'

import _ from 'lodash';
import _repeat from 'lodash/repeat';
import FontAwesome from 'react-fontawesome';

import Autocomplete from './Autocomplete.jsx';

function makeStars(product) {
	const rating0 = (product.rating) ? (product.rating / 100) : 0;
	const rating = Math.round(rating0 * 2);
	const nstar = rating / 2;
	const nhalf = (rating % 2);
	const nempty = 5 - nstar - nhalf;
	let s = "";
	return _repeat('\uf005', nstar) + _repeat("\uf123", nhalf) + _repeat("\uf006", nempty);
}

const ProductListItem = React.createClass({
	propTypes: {
		product: React.PropTypes.object
	},
	getInitialState: function() {
		return {editing: false}
	},
	render: function() {
		const {product, controls, niches} = this.props;
		const onSetProductBucket = controls.onSetProductBucket;

		const editor = (!this.state.editing)
			? (<span onClick={this.onOpenEditor}><u>{product.niche || "...niche..."}</u></span>)
			: (
					<form onSubmit={this.onSubmit}>
						<Autocomplete
							ref="niche"
							defaultValue={product.niche || ""}
							autoFirst={false}
							values={niches}
							minChars="1"
						/>
						<input type="submit" value="Save"/> <input type="button" value="Cancel" onClick={this.onCloseEditor}/>
					</form>
				);

		const weight = (product.weight) ? (product.weight / 1000).toFixed(3).replace(/0+$/, "") + "kg" : null;
		const size
			= _.isString(product.size)
			? product.size.replace(/ /g, "")
			: _.isArray(product.size)
				? product.size.map(n => n / 10).join("/")
				: null;

		return <tr>
			<td>{product.rank}<br/>{(product.category || "").substr(0, 5)}<br/><img src="images/hide.png" onClick={this.onHideProduct} alt="hide" style={{width: "1em"}}/></td>
			<td><img src={(product.thumbnail || "").replace(/_S.+\./, "_SS120_.")} onClick={() => controls.openBrowser(product.url)}/></td>
			<td>
				<span
					style={(product.bucket === "current") ? {backgroundColor: "#40ff40"} : null}
					onClick={() => onSetProductBucket([product.id], "current")}
				>C</span>/<span
					style={(product.bucket === "soon") ? {backgroundColor: "#80ffff"} : null}
					onClick={() => onSetProductBucket([product.id], "soon")}
				>S</span>/<span
					style={(product.bucket === "later") ? {backgroundColor: "#8080ff"} : null}
					onClick={() => onSetProductBucket([product.id], "later")}
				>L</span>/<span
					style={(product.bucket === "never") ? {backgroundColor: "#ff8080"} : null}
					onClick={() => onSetProductBucket([product.id], "never")}
				>N</span>
			</td>
			<td>{_.isNumber(product.cost) ? (product.cost / 10000).toFixed(2)+ "€" : product.cost}<br/>{weight}<br/>{size}</td>
			<td>{product.turnover}<br/>{product.turnoverScore}<br/>{product.turnoverGross}</td>
			<td>
				{product.title} ({product.asin}) {editor}<br/>
				<span style={{color: "rgba(234,184,83,1)", fontFamily: "FontAwesome"}}>{makeStars(product)}</span> {(product.rating) ? (product.rating / 100).toFixed(1) : "0.0"} ({product.reviews || ""}) {product.category}
			</td>
		</tr>;
	},
	onHideProduct: function() {
		this.props.controls.onHideProduct(this.props.product.id);
	},
	onOpenEditor: function() {
		this.setState({editing: true});
		const that = this;
		setTimeout(function() { ReactDOM.findDOMNode(that.refs.niche).focus() }, 100)
	},
	onCloseEditor: function() {
		this.setState({editing: false});
		this.refs.niche.value = this.props.product.niche;
	},
	onSubmit: function(e) {
		e.preventDefault();

		const {product} = this.props;

		// const num = (x) => _.isEmpty(x.value) ? null : Number(x.value);
		// const str = (x) => _.isEmpty(x.value) ? null : x.value;
		// console.log("onSubmit")
		// console.log(this.refs.niche.getValue());
		// console.log(this.refs.niche)
		const data = {id: product.id, niche: this.refs.niche.getValue()};
		// console.log({data});
		this.setState({editing: false});
		this.props.controls.onSetProductProperties(data);
	}
});
//

/**
 * Component for a list of products.
 * @param  {array} products        [description]
 * @param  {object} ui               [description]
 * @param  {function} onSetProductBucket  [description]
 * @param  {function} onSetProductProperties [description]
 * @param  {function} onHideProduct}   [description]
 */
const ProductList = ({products, controls, niches}) => (
	<div>
		({(products || []).length} products)
		<br/>
		Sort by:
		<span onClick={() => controls.onSortProducts("turnover")}>turnover</span>
		<span onClick={() => controls.onSortProducts("turnoverGross")}>gross</span>
		<table>
			<thead>
				<tr><th/><th/><th>Bucket</th></tr>
			</thead>
			<tbody>
				{(products)
					? products.map(product => <ProductListItem key={product.asin} product={product} controls={controls} niches={niches}/>)
					: null}
			</tbody>
		</table>
	</div>
);

export default ProductList;
